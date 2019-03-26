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
package com.adobe.cq.wcm.core.components.models;

import com.adobe.cq.export.json.ComponentExporter;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface Video extends ComponentExporter {

    String PN_GET_TITLE_FROM_DAM = "titleValueFromDAM";
    String PN_GET_DESCRIPTION_FROM_DAM = "descriptionValueFromDAM";
    String PN_AUTOPLAY = "autoplay";
    String PN_LOOP = "loop";
    String PN_HIDE_CONTROLS = "hideControls";
    String PN_PRELOAD = "preload";
    String PRELOAD_DEFAULT_NONE = "none";

    List<String> ALLOWED_MIME_TYPES = Collections.unmodifiableList(Arrays.asList("video/3gpp", "video/x-flv", "video/mp4", "video/ogg", "video/webm"));

    default List<VideoSource> getSources() {
        throw new UnsupportedOperationException();
    }

    default String getTitle() {
        throw new UnsupportedOperationException();
    }

    default String getDescription() {
        throw new UnsupportedOperationException();
    }

    default boolean isAutoplayEnabled() {
        throw new UnsupportedOperationException();
    }

    default boolean isLoopEnabled() {
        throw new UnsupportedOperationException();
    }

    default boolean hideControls() {
        throw new UnsupportedOperationException();
    }

    default String getPreload() {
        throw new UnsupportedOperationException();
    }

    default String getPosterImage() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see ComponentExporter#getExportedType()
     * @since com.adobe.cq.wcm.core.components.models 12.2.0
     */
    @Nonnull
    @Override
    default String getExportedType() {
        throw new UnsupportedOperationException();
    }
}
