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

import com.adobe.cq.wcm.core.components.models.VideoSource;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;

public class VideoSourceImpl implements VideoSource {

    private String src;
    private String type;

    public VideoSourceImpl(Asset asset) {
        this(asset.getPath(), asset.getMimeType());
    }

    public VideoSourceImpl(Rendition rendition) {
        this(rendition.getPath(), rendition.getMimeType());
    }

    public VideoSourceImpl(String src, String type) {
        this.src = src;
        this.type = type;
    }

    @Override
    public String getSrc() {
        return src;
    }

    @Override
    public String getType() {
        return type;
    }
}
