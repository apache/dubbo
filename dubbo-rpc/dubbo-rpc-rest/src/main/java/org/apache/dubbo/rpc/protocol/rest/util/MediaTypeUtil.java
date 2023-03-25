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
package org.apache.dubbo.rpc.protocol.rest.util;

import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.exception.UnSupportContentTypeException;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;

import java.util.Arrays;
import java.util.List;

public class MediaTypeUtil {
    private static final List<MediaType> mediaTypes = Arrays.asList(MediaType.values());

    /**
     * return first match , if any multiple content-type  ,acquire mediaType by targetClass type .if contentTypes is empty
     *
     * @param contentTypes
     * @return
     */
    public static MediaType convertMediaType(Class<?> targetType, String... contentTypes) {

        if ((contentTypes == null || contentTypes.length == 0) && targetType != null) {

            return HttpMessageCodecManager.typeSupport(targetType);
        }

        for (String contentType : contentTypes) {
            for (MediaType mediaType : mediaTypes) {

                if (contentType != null && contentType.contains(mediaType.value)) {
                    return mediaType;
                }
            }
        }

        throw new UnSupportContentTypeException(Arrays.toString(contentTypes));

    }
}
