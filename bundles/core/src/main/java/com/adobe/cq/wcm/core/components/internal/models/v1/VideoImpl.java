/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe Systems Incorporated
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
import com.adobe.cq.wcm.core.components.models.Video;
import com.adobe.cq.wcm.core.components.models.VideoSource;
import com.day.cq.commons.DownloadResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Rendition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Model(adaptables = SlingHttpServletRequest.class, adapters = {Video.class, ComponentExporter.class}, resourceType = VideoImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class VideoImpl implements Video {

    public static final String RESOURCE_TYPE = "core/wcm/components/video/v1/video";

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoImpl.class);

    @Self
    protected SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    @SlingObject
    @JsonIgnore
    private ResourceResolver resourceResolver;

    @ValueMapValue(name = Video.PN_GET_TITLE_FROM_DAM, injectionStrategy = InjectionStrategy.OPTIONAL)
    private boolean titleFromDam;

    @ValueMapValue(name = Video.PN_GET_DESCRIPTION_FROM_DAM, injectionStrategy = InjectionStrategy.OPTIONAL)
    private boolean descriptionFromDam;

    @ValueMapValue(name = DownloadResource.PN_REFERENCE, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String fileReference;

    @ValueMapValue(name = JcrConstants.JCR_TITLE, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String title;

    @ValueMapValue(name = JcrConstants.JCR_DESCRIPTION, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String description;

    @ValueMapValue(name = Video.PN_PRELOAD, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String preload;

    @ValueMapValue(name = Video.PN_AUTOPLAY, injectionStrategy = InjectionStrategy.OPTIONAL)
    private boolean autoplayEnabled;

    @ValueMapValue(name = Video.PN_LOOP, injectionStrategy = InjectionStrategy.OPTIONAL)
    private boolean loopEnabled;

    @ValueMapValue(name = Video.PN_HIDE_CONTROLS, injectionStrategy = InjectionStrategy.OPTIONAL)
    private boolean hideControls;

    private List<VideoSource> sources = new ArrayList<>();

    private String posterImage;

    @PostConstruct
    protected void initModel() {
        if(StringUtils.isBlank(fileReference)) {
            //The component has not been authored, skip init
            return;
        }

        Resource assetResource = resourceResolver.getResource(fileReference);
        if(assetResource == null) {
            LOGGER.error("The asset path '{}' used by video component '{}' does not resolve to a resource.", fileReference, resource.getPath());
            return;
        }

        Asset videoAsset = assetResource.adaptTo(Asset.class);
        if(videoAsset == null) {
            LOGGER.error("Could not adapt resource '{}' used by video component '{}' to an asset.", fileReference, resource.getPath());
            return;
        }

        String mimeType = videoAsset.getMimeType();
        if(StringUtils.isBlank(mimeType) || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            LOGGER.error("Mime type '{}' of video asset '{}' is not a recognized video mime type.", mimeType, fileReference);
            return;
        }

        sources.add(new VideoSourceImpl(videoAsset));
        List<Rendition> renditions = videoAsset.getRenditions();
        for(Rendition rendition : renditions) {
            String renditionMimeType = rendition.getMimeType();
            if(StringUtils.isNotBlank(renditionMimeType) &&
                !renditionMimeType.equals(mimeType) &&
                ALLOWED_MIME_TYPES.contains(renditionMimeType)) {
                sources.add(new VideoSourceImpl(rendition));
            }
        }

        Rendition imagePreviewRendition = videoAsset.getImagePreviewRendition();
        if(imagePreviewRendition == null || imagePreviewRendition.equals(videoAsset.getOriginal())) {
            LOGGER.warn("No suitable image preview rendition exists for video '{}'", fileReference);
        } else {
            posterImage = imagePreviewRendition.getPath();
        }

        if(titleFromDam) {
            String damTitle = videoAsset.getMetadataValue(DamConstants.DC_TITLE);
            if(StringUtils.isNotBlank(damTitle)) {
                title = damTitle;
            }
        }
        if(descriptionFromDam) {
            String damDescription = videoAsset.getMetadataValue(DamConstants.DC_DESCRIPTION);
            if(StringUtils.isNotBlank(damDescription)) {
                description = videoAsset.getMetadataValue(DamConstants.DC_DESCRIPTION);
            }
        }

        if(StringUtils.isBlank(preload)) {
            preload = PRELOAD_DEFAULT_NONE;
        }

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
    public String getPreload() {
        return preload;
    }

    @Override
    public List<VideoSource> getSources() {
        return sources;
    }

    @Override
    public boolean isAutoplayEnabled() {
        return autoplayEnabled;
    }

    @Override
    public boolean isLoopEnabled() {
        return loopEnabled;
    }

    @Override
    public boolean hideControls() {
        return hideControls;
    }

    @Override
    public String getPosterImage() {
        return posterImage;
    }

    @Nonnull
    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }
}
