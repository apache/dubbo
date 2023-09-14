/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.rest.message;

import org.apache.dubbo.metadata.rest.media.MediaType;

import java.util.ArrayList;
import java.util.List;

public enum MediaTypeMatcher {


    MULTI_VALUE(createMediaList(MediaType.APPLICATION_FORM_URLENCODED_VALUE)),
    APPLICATION_JSON(createMediaList(MediaType.APPLICATION_JSON_VALUE)),
    TEXT_PLAIN(createMediaList(MediaType.TEXT_PLAIN, MediaType.OCTET_STREAM)),
    TEXT_XML(createMediaList(MediaType.TEXT_XML)),

    ;

    private List<MediaType> mediaTypes;


    MediaTypeMatcher(List<MediaType> mediaTypes) {
        this.mediaTypes = mediaTypes;
    }


    private static List<MediaType> createMediaList(MediaType... mediaTypes) {
        List<MediaType> mediaTypeList = getDefaultList();

        for (MediaType mediaType : mediaTypes) {

            mediaTypeList.add(mediaType);
        }
        return mediaTypeList;
    }

    private static List<MediaType> getDefaultList() {

        List<MediaType> defaultList = new ArrayList<>();
        return defaultList;
    }

    public boolean mediaSupport(MediaType mediaType) {
        return mediaTypes.contains(mediaType);
    }


}
