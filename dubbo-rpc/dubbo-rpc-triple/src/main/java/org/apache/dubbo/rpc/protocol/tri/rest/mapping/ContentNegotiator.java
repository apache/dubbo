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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoderFactory;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.codec.CodecUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentNegotiator {

    private final CodecUtils codecUtils;
    private Map<String, MediaType> extensionMapping;
    private String parameterName;

    public ContentNegotiator(FrameworkModel frameworkModel) {
        codecUtils = frameworkModel.getOrRegisterBean(CodecUtils.class);
    }

    public String negotiate(HttpRequest request, HandlerMeta meta) {
        String mediaType;

        // 1. find mediaType by producible
        List<MediaType> produces = request.attribute(RestConstants.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        if (produces != null) {
            for (int i = 0, size = produces.size(); i < size; i++) {
                mediaType = getSuitableMediaType(produces.get(i).getName());
                if (mediaType != null) {
                    return mediaType;
                }
            }
        }

        // 2. find mediaType by accept header
        List<String> accepts = HttpUtils.parseAccept(request.accept());
        String preferMediaType = null;
        boolean hasAll = false;
        if (accepts != null) {
            for (int i = 0, size = accepts.size(); i < size; i++) {
                String accept = accepts.get(i);
                if (!hasAll && MediaType.ALL.getName().equals(accept)) {
                    hasAll = true;
                }
                if (preferMediaType == null) {
                    mediaType = getSuitableMediaType(accept);
                    if (mediaType != null) {
                        preferMediaType = mediaType;
                    }
                }
            }
        }

        // 3. find mediaType by format parameter
        String format = request.queryParameter(getParameterName());
        if (format != null) {
            mediaType = getMediaTypeByExtension(format);
            if (mediaType != null) {
                return mediaType;
            }
        }

        // 4. find mediaType by extension
        String path = request.rawPath();
        int index = path.lastIndexOf('.');
        if (index != -1) {
            mediaType = getMediaTypeByExtension(path.substring(index + 1));
            if (mediaType != null) {
                return mediaType;
            }
        }

        if (preferMediaType == null) {
            return null;
        }

        // Keep consistent with Spring MVC behavior
        if (hasAll && preferMediaType.startsWith("text/")) {
            Class<?> responseType = meta.getMethodMetadata().getActualResponseType();
            if (responseType != null && !CharSequence.class.isAssignableFrom(responseType)) {
                return MediaType.APPLICATION_JSON.getName();
            }
        }

        return preferMediaType;
    }

    public boolean supportExtension(String extension) {
        return getMediaTypeByExtension(extension) != null;
    }

    private String getSuitableMediaType(String name) {
        int index = name.indexOf('/');
        if (index == -1 || index == name.length() - 1) {
            return null;
        }

        String type = name.substring(0, index);
        if (MediaType.WILDCARD.equals(type)) {
            return null;
        }

        String subType = name.substring(index + 1);
        if (MediaType.WILDCARD.equals(subType)) {
            return MediaType.TEXT_PLAIN.getType().equals(type) ? MediaType.TEXT_PLAIN.getName() : null;
        }

        int suffixIndex = subType.lastIndexOf('+');
        if (suffixIndex != -1) {
            return getMediaTypeByExtension(subType.substring(suffixIndex + 1));
        }

        return name;
    }

    public String getParameterName() {
        if (parameterName == null) {
            parameterName = ConfigManager.getProtocolOrDefault(CommonConstants.TRIPLE)
                    .getTripleOrDefault()
                    .getRestOrDefault()
                    .getFormatParameterNameOrDefault();
        }
        return parameterName;
    }

    private String getMediaTypeByExtension(String extension) {
        Map<String, MediaType> extensionMapping = this.extensionMapping;
        if (extensionMapping == null) {
            extensionMapping = new HashMap<>();

            for (HttpMessageEncoderFactory factory : codecUtils.getEncoderFactories()) {
                MediaType mediaType = factory.mediaType();
                String subType = mediaType.getSubType();
                int index = subType.lastIndexOf('+');
                if (index != -1) {
                    subType = subType.substring(index + 1);
                }
                extensionMapping.putIfAbsent(subType, mediaType);
            }

            extensionMapping.put("css", MediaType.TEXT_CSS);
            extensionMapping.put("js", MediaType.TEXT_JAVASCRIPT);
            extensionMapping.put("yml", MediaType.APPLICATION_YAML);
            extensionMapping.put("xhtml", MediaType.TEXT_HTML);
            extensionMapping.put("html", MediaType.TEXT_HTML);
            extensionMapping.put("htm", MediaType.TEXT_HTML);
            extensionMapping.put("proto", new MediaType(MediaType.TEXT, "proto"));
            for (String ext : new String[] {"txt", "md", "csv", "log", "properties"}) {
                extensionMapping.put(ext, MediaType.TEXT_PLAIN);
            }
            for (String ext : new String[] {"jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "tiff", "ico"}) {
                extensionMapping.put(ext, new MediaType("image", ext));
            }
            for (String ext : new String[] {"zip", "gz", "7z", "tar", "rar"}) {
                extensionMapping.put(ext, MediaType.APPLICATION_OCTET_STREAM);
            }
            for (String ext : new String[] {"xls", "xlsx", "doc", "docx", "ppt", "pptx", "pdf"}) {
                extensionMapping.put(ext, MediaType.APPLICATION_OCTET_STREAM);
            }
            for (String ext : new String[] {"mp3", "m4a", "mp4", "avi", "flv"}) {
                extensionMapping.put(ext, MediaType.APPLICATION_OCTET_STREAM);
            }
            this.extensionMapping = extensionMapping;
        }
        MediaType mediaType = extensionMapping.get(extension);
        return mediaType == null ? null : mediaType.getName();
    }
}
