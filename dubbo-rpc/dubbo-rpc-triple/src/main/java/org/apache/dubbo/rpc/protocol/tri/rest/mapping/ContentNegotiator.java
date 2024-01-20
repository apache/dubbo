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

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoderFactory;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.codec.CodecUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentNegotiator {

    private final String parameterName;
    private final CodecUtils codecUtils;

    private Map<String, MediaType> extensionMapping;

    public ContentNegotiator(FrameworkModel frameworkModel) {
        Configuration conf = ConfigurationUtils.getGlobalConfiguration(frameworkModel.defaultApplication());
        parameterName = conf.getString(RestConstants.FORMAT_PARAMETER_NAME_KEY, "format");
        codecUtils = frameworkModel.getBeanFactory().getOrRegisterBean(CodecUtils.class);
    }

    public String negotiate(HttpRequest request) {
        // 1. find mediaType by producible
        List<MediaType> produces = request.attribute(RestConstants.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        if (CollectionUtils.isNotEmpty(produces)) {
            for (MediaType mediaType : produces) {
                String name = mediaType.getName();
                if (name.indexOf('*') == -1) {
                    return name;
                }
            }
        }

        // 2. find mediaType by accept header
        List<String> accepts = HttpUtils.parseAccept(request.accept());
        if (!accepts.isEmpty()) {
            for (String accept : accepts) {
                if (accept.indexOf('*') == -1) {
                    return accept;
                }
            }
        }

        // 3. find mediaType by format parameter
        String format = request.queryParameter(parameterName);
        if (format != null) {
            String mediaType = getMediaTypeByExtension(format);
            if (mediaType != null) {
                return mediaType;
            }
        }

        // 5. find mediaType by extension
        String path = request.rawPath();
        int index = path.lastIndexOf('.');
        if (index != -1) {
            String extension = path.substring(index + 1);
            String mediaType = getMediaTypeByExtension(extension);
            if (mediaType != null) {
                return mediaType;
            }
        }

        // 6. use request mediaType
        String mediaType = request.mediaType();
        if (mediaType != null) {
            return mediaType;
        }

        // 7. use "application/json" as default mediaType
        return MediaType.APPLICATION_JSON.getName();
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

            extensionMapping.put("yml", MediaType.APPLICATION_YAML);
            extensionMapping.put("html", MediaType.TEXT_HTML);
            extensionMapping.put("htm", MediaType.TEXT_HTML);
            for (String ext : new String[] {"txt", "md", "csv", "log", "properties"}) {
                extensionMapping.put(ext, MediaType.TEXT_PLAIN);
            }

            this.extensionMapping = extensionMapping;
        }
        MediaType mediaType = extensionMapping.get(extension);
        return mediaType == null ? null : mediaType.getName();
    }
}
