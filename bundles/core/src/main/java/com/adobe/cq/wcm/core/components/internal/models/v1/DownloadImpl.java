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
package com.adobe.cq.wcm.core.components.internal.models.v1;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.internal.servlets.CoreFileDownloadServlet;
import com.adobe.cq.wcm.core.components.models.Download;
import com.day.cq.commons.DownloadResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.designer.Style;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.Calendar;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = {Download.class, ComponentExporter.class},
       resourceType = DownloadImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class DownloadImpl implements Download {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadImpl.class);

    public final static String RESOURCE_TYPE = "core/wcm/components/download/v1/download";

    protected static final String JPEG_EXTENSION = ".jpeg";
    protected static final String IMAGE_SERVLET_EXTENSION = ".coreimg" + JPEG_EXTENSION;
    protected static final String DOT = ".";
    protected static final String SLASH = "/";

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Resource resource;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    @JsonIgnore
    protected Style currentStyle;

    @SlingObject
    private ResourceResolver resourceResolver;

    private String downloadUrl;

    private boolean titleFromAsset = false;

    private boolean descriptionFromAsset = false;

    private boolean displaySize;

    private boolean displayFormat;

    private boolean displayFilename;

    @ValueMapValue(optional = true, name = JcrConstants.JCR_TITLE)
    private String title;

    @ValueMapValue(optional = true, name = JcrConstants.JCR_DESCRIPTION)
    private String description;

    @ValueMapValue(optional = true)
    private String actionText;

    private String imagePath;

    private String titleType;

    private String filename;

    private String format;

    private String size;

    private long lastModified = 0;

    @PostConstruct
    protected void initModel() {
        String fileReference = properties.get(DownloadResource.PN_REFERENCE, String.class);
        titleFromAsset = properties.get(PN_TITLE_FROM_ASSET, titleFromAsset);
        descriptionFromAsset = properties.get(PN_DESCRIPTION_FROM_ASSET, descriptionFromAsset);
        if (currentStyle != null) {
            if (StringUtils.isBlank(actionText)) {
                actionText = currentStyle.get(PN_ACTION_TEXT, String.class);
            }
            titleType = currentStyle.get(PN_TITLE_TYPE, String.class);
            displaySize = currentStyle.get(PN_DISPLAY_SIZE, false);
            displayFormat = currentStyle.get(PN_DISPLAY_FORMAT, false);
            displayFilename = currentStyle.get(PN_DISPLAY_FILENAME, false);
        }
        if (StringUtils.isNotBlank(fileReference)) {
            Resource downloadResource = resourceResolver.getResource(fileReference);
            if (downloadResource != null) {
                Asset downloadAsset = downloadResource.adaptTo(Asset.class);
                if (downloadAsset != null) {
                    Calendar resourceLastModified = properties.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
                    if (resourceLastModified != null) {
                        lastModified = resourceLastModified.getTimeInMillis();
                    }
                    long assetLastModified = downloadAsset.getLastModified();
                    if (assetLastModified > lastModified) {
                        lastModified = assetLastModified;
                    }

                    ValueMap downloadResourceProperties = downloadResource.getValueMap();
                    if(downloadResourceProperties != null){
                        String uuid = downloadAsset.getID();
                        if(StringUtils.isNotBlank(uuid)) {
                            StringBuffer downloadUrlBuilder = new StringBuffer();
                            downloadUrlBuilder.append(CoreFileDownloadServlet.PATH)
                                .append(DOT)
                                .append(CoreFileDownloadServlet.EXTENSION)
                                .append(SLASH)
                                .append(uuid)
                                .append(SLASH)
                                .append(downloadAsset.getName());
                            downloadUrl = downloadUrlBuilder.toString();
                        }
                    }

                    StringBuilder imagePathBuilder = new StringBuilder();

                    String resourcePath = resourceResolver.map(request, resource.getPath());

                    imagePathBuilder.append(resourcePath).append(IMAGE_SERVLET_EXTENSION);
                    if (lastModified > 0) {
                        imagePathBuilder.append("/").append(lastModified).append(JPEG_EXTENSION);
                    }

                    imagePath = imagePathBuilder.toString();

                    if (titleFromAsset) {
                        String assetTitle = downloadAsset.getMetadataValue(DamConstants.DC_TITLE);
                        if (StringUtils.isNotBlank(assetTitle)) {
                            title = assetTitle;
                        }
                    }
                    if (descriptionFromAsset) {
                        String assetDescription = downloadAsset.getMetadataValue(DamConstants.DC_DESCRIPTION);
                        if (StringUtils.isNotBlank(assetDescription)) {
                            description = assetDescription;
                        }
                    }

                    size = null;
                    Object rawFileSizeObject = downloadAsset.getMetadata(DamConstants.DAM_SIZE);
                    if(rawFileSizeObject != null){
                        long rawFileSize = (Long)rawFileSizeObject;
                        size = FileUtils.byteCountToDisplaySize(rawFileSize);
                    }

                    filename = downloadAsset.getName();

                    format = downloadAsset.getMetadataValue(DamConstants.DC_FORMAT);
                }
            }
        }
    }

    @Nonnull
    @Override
    public String getExportedType() {
        return request.getResource().getResourceType();
    }

    @Override
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getActionText() {
        return actionText;
    }

    @Override
    public String getImagePath() {
        return imagePath;
    }

    @Override
    public String getTitleType() {
        return titleType;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public String getSize() {
        return size;
    }

    @Override
    public boolean displaySize() {
        return displaySize;
    }

    @Override
    public boolean displayFormat() {
        return displayFormat;
    }

    @Override
    public boolean displayFilename() {
        return displayFilename;
    }
}
