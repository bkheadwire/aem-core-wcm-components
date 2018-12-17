/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2018 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.adobe.cq.wcm.core.components.internal.servlets;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(
    immediate = true,
    service = Servlet.class,
    property = {
        "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        "sling.servlet.extensions=" + FileDownloadServlet.EXTENSION,
        "sling.servlet.paths=" + FileDownloadServlet.PATH
    }
)
public class FileDownloadServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadServlet.class);
    public static final String EXTENSION = "file";
    public static final String PATH = "/bin/download";

    private static final String SUFFIX_SEPARATOR = "/";

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        String suffix = requestPathInfo.getSuffix();

        if(StringUtils.isBlank(suffix)) {
            LOGGER.error("Expected a suffix with a UUID for a downloadable file, but no suffix was present.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String[] suffixParts = suffix.split(SUFFIX_SEPARATOR);
        if(suffixParts.length != 3) {
            LOGGER.error("Expected suffix to contain a UUID and a filename, instead got: {}", suffix);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String uuid = suffixParts[1];
        String filename = suffixParts[2];
        LOGGER.debug("Filename: {}", filename);

        ResourceResolver resourceResolver = request.getResourceResolver();
        Resource downloadResource = findDownloadableResource(resourceResolver, uuid);

        if(downloadResource == null){
            LOGGER.error("No resource found with the uuid: {}", uuid);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        LOGGER.debug("Download Resource: {}", downloadResource.getPath());
    }

    @Nullable
    private Resource findDownloadableResource(@Nonnull ResourceResolver resourceResolver, @Nonnull String uuid) {
        Resource resource = null;
        Session session = resourceResolver.adaptTo(Session.class);
        if(session == null)
        {
            LOGGER.error("Got a null session from the resource resolver: {}", resourceResolver.toString());
            return null;
        }
        try {
            Node fileNode = session.getNodeByIdentifier(uuid);
            if(fileNode == null) {
                //This should be caught as an ItemNotFoundException but just in case
                LOGGER.error("Null node returned for uuid: {}", uuid);
                return null;
            }

            String path = fileNode.getPath();
            resource = resourceResolver.getResource(path);

        } catch (RepositoryException e) {
            LOGGER.error("Could not get the node with the UUID: " + uuid, e);
        }

        return resource;
    }
}
