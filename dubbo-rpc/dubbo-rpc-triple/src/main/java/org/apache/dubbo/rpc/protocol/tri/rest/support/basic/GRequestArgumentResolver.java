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
package org.apache.dubbo.rpc.protocol.tri.rest.support.basic;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.AnnotationBaseArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.stub.annotations.GRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Activate
public class GRequestArgumentResolver implements AnnotationBaseArgumentResolver<GRequest> {

    @Override
    public Class<GRequest> accept() {
        return GRequest.class;
    }

    @Override
    public NamedValueMeta getNamedValueMeta(ParameterMeta parameter, AnnotationMeta<Annotation> annotation) {
        return new NamedValueMeta().setParamType(ParamType.Body);
    }

    @Override
    public Object resolve(
            ParameterMeta parameter, AnnotationMeta<GRequest> annotation, HttpRequest request, HttpResponse response) {
        HttpMessageDecoder decoder = request.attribute(RestConstants.BODY_DECODER_ATTRIBUTE);
        if (decoder == null) {
            return null;
        }

        Map<String, Object> value = new HashMap<>();
        Map<String, String> variableMap = request.attribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variableMap != null) {
            value.putAll(variableMap);
        }

        InputStream is = request.inputStream();
        try {
            int available = is.available();
            if (available > 0) {
                Object body = decoder.decode(is, Object.class, request.charsetOrDefault());
                if (body instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> bodyMap = (Map<String, Object>) body;
                    String key = annotation.getValue();
                    if ("*".equals(key) || key.isEmpty()) {
                        value.putAll(bodyMap);
                    } else {
                        value.put(key, bodyMap.get(key));
                    }
                }
            }
        } catch (IOException e) {
            throw new DecodeException("Error reading input", e);
        }

        return decoder.decode(
                new ByteArrayInputStream(JsonUtils.toJson(value).getBytes(StandardCharsets.UTF_8)),
                parameter.getType(),
                StandardCharsets.UTF_8);
    }
}
