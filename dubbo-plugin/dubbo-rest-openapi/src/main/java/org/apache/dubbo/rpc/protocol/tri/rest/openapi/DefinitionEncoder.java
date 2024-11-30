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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.remoting.http12.exception.UnsupportedMediaTypeException;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.codec.YamlCodec;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class DefinitionEncoder {

    private final ExtensionFactory extensionFactory;
    private final SchemaResolver schemaResolver;

    private ProtoEncoder protoEncoder;

    DefinitionEncoder(FrameworkModel frameworkModel) {
        extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        schemaResolver = frameworkModel.getOrRegisterBean(SchemaResolver.class);
    }

    public String encode(OpenAPI openAPI, OpenAPIRequest request) {
        if (openAPI == null) {
            openAPI = new OpenAPI();
        }
        Map<String, Object> root = new LinkedHashMap<>();
        ContextImpl context = new ContextImpl(openAPI, schemaResolver, extensionFactory, request);
        openAPI.writeTo(root, context);

        String format = request.getFormat();
        format = format == null ? MediaType.JSON : format.toLowerCase();
        switch (format) {
            case MediaType.JSON:
                if (Boolean.TRUE.equals(request.getPretty())) {
                    return JsonUtils.toPrettyJson(root);
                }
                return JsonUtils.toJson(root);
            case "yml":
            case "yaml":
                ByteArrayOutputStream os = new ByteArrayOutputStream(4096);
                YamlCodec.INSTANCE.encode(os, root, StandardCharsets.UTF_8);
                return new String(os.toByteArray(), StandardCharsets.UTF_8);
            case "proto":
                if (protoEncoder == null) {
                    protoEncoder = new ProtoEncoder();
                }
                return protoEncoder.encode(openAPI);
            default:
                throw new UnsupportedMediaTypeException("text/" + format);
        }
    }
}
