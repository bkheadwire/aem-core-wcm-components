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

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
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
import java.io.InputStream;

/**
 * Servlet for obfuscating file download URLs so as not to reveal the underlying repository structure. The file to be
 * downloaded is identified via a suffix with the format /{@code UUID}/{@code filename}
 *
 * The download servlet intentionally uses a static path to allow the download files to be cached once, rather than
 * tying them to the hierarchy of the page content and caching them separately each time they are referenced.
 */
@Component(
    immediate = true,
    service = Servlet.class,
    property = {
        "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        "sling.servlet.extensions=" + CoreFileDownloadServlet.EXTENSION,
        "sling.servlet.paths=" + CoreFileDownloadServlet.PATH
    }
)
@Designate(
    ocd = CoreFileDownloadServlet.Configuration.class
)
public class CoreFileDownloadServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreFileDownloadServlet.class);


    /**
     * The extension of the download servlet
     */
    public static final String EXTENSION = "file";

    /**
     * The static path to the download servlet
     */
    public static final String PATH = "/bin/download";

    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

    private static final String ASSET_RESOURCE_TYPE = "dam:Asset";

    private static final String SUFFIX_SEPARATOR = "/";
    private static final String ATTACHMENT_DISPOSITION = "attachment";
    private static final String INLINE_DISPOSITION = "inline";

    private boolean forceDownload;

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

        if(StringUtils.isBlank(uuid) || StringUtils.isBlank(filename))
        {
            LOGGER.error("Expected suffix to contain a UUID and a filename, instead got: {}", suffix);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        ResourceResolver resourceResolver = request.getResourceResolver();
        Resource downloadResource = findDownloadableResource(resourceResolver, uuid);

        if(downloadResource == null){
            LOGGER.error("No resource found with the uuid: {}", uuid);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if(downloadResource.isResourceType(ASSET_RESOURCE_TYPE)) {
            if(!filename.equalsIgnoreCase(downloadResource.getName()))
            {
                LOGGER.error("Filename from suffix '{}' does not match filename from resource '{}'",
                    filename, downloadResource.getName());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            streamAsset(downloadResource, filename, response);
        }

        LOGGER.debug("Download Resource: {}", downloadResource.getPath());
    }

    private void streamAsset(@Nonnull Resource downloadResource, @Nonnull String filename, @Nonnull SlingHttpServletResponse response) throws IOException
    {
        Asset downloadAsset = downloadResource.adaptTo(Asset.class);
        if(downloadAsset == null) {
            LOGGER.error("Could not adapt Resource to Asset: {}", downloadResource.getPath());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Rendition original = downloadAsset.getOriginal();
        if(original == null){
            LOGGER.error("No original rendition found for asset: {}", downloadAsset.getPath());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        InputStream inputStream = original.getStream();
        String contentType = original.getMimeType();
        int size = Math.toIntExact(original.getSize());

        stream(response, inputStream, contentType, filename, size);
    }

    private void stream(@Nonnull SlingHttpServletResponse response, @Nonnull InputStream inputStream,
                        @Nonnull String contentType, String filename, int size) throws IOException{
        if(size > 0){
            response.setContentLength(size);
        }
        response.setContentType(contentType);

        StringBuffer dispositionBuilder = new StringBuffer();
        if(forceDownload){
            dispositionBuilder.append(ATTACHMENT_DISPOSITION);
            if(StringUtils.isNotBlank(filename)){
                dispositionBuilder.append("; filename=\"").append(filename).append("\"");
            }
        }
        else
        {
            dispositionBuilder.append(INLINE_DISPOSITION);
        }

        response.setHeader(CONTENT_DISPOSITION_HEADER, dispositionBuilder.toString());

        try{
            IOUtils.copy(inputStream, response.getOutputStream());
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
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

    @Activate
    @Modified
    protected void activate(Configuration configuration){
        forceDownload = configuration.force_download();
    }

    @ObjectClassDefinition(
        name = "Core File Download Servlet",
        description = "Proxies downloadable content through a servlet to obfuscate the repository path"
    )
    @interface Configuration {
        @AttributeDefinition(
            name = "Force Download",
            description = "Sets the content disposition header of the servlet response to attachment, forcing the file " +
                "to be downloaded rather than opened as a browser tab.",
            type = AttributeType.BOOLEAN
        ) boolean force_download() default false;
    }
}
