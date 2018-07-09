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

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.methods=POST",
        "sling.servlet.resourceTypes=sling/servlet/default",
        "sling.servlet.selectors=" + ChildrenEditorServlet.SELECTOR,
        "sling.servlet.extensions=html"
    }
)
public class ChildrenEditorServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChildrenEditorServlet.class);

    protected static final String SELECTOR = "childreneditor";
    private static final String PARAM_DELETED_CHILDREN = "deletedChildren";
    private static final String PARAM_OREDERED_CHILDREN = "orderedChildren";

    @Override
    protected void doPost(SlingHttpServletRequest request,
                          final SlingHttpServletResponse response)
        throws ServletException, IOException {


        ResourceResolver resolver = request.getResourceResolver();
        Resource container = request.getResource();

        // Remove children
        String[] deletedChildrenNames = request.getParameterValues(PARAM_DELETED_CHILDREN);
        if (deletedChildrenNames != null && deletedChildrenNames.length > 0) {
            for (String childName: deletedChildrenNames) {
                Resource child = container.getChild(childName);
                if (child != null) {
                    resolver.delete(child);
                    resolver.commit();
                }
            }
        }

        // Order children
        String[] orderedChildrenNames = request.getParameterValues(PARAM_OREDERED_CHILDREN);
        if (orderedChildrenNames != null && orderedChildrenNames.length > 0) {
            final Node containerNode = container.adaptTo(Node.class);
            if (containerNode != null) {
                for (int i = orderedChildrenNames.length - 1; i >=0 ; i--) {
                    try {
                        // TODO: revisit this assumption: it could lead to concurrency issues
                        // if the child node does not exist, we create it so that it can be ordered
                        if (!containerNode.hasNode(orderedChildrenNames[i])) {
                            containerNode.addNode(orderedChildrenNames[i]);
                        }
                        if (i == orderedChildrenNames.length - 1) {
                            // Put the last item at the end
                            containerNode.orderBefore(orderedChildrenNames[i], null);
                        } else {
                            containerNode.orderBefore(orderedChildrenNames[i], orderedChildrenNames[i+1]);
                        }
                    } catch (RepositoryException e) {
                        LOGGER.error("Could not re-order the items: {}", e);
                    }
                    resolver.commit();
                }
            }
        }

    }

}