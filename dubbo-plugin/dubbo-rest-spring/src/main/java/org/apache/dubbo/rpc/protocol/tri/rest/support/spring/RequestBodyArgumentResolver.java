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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.AnnotationBaseArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;

import static org.apache.dubbo.rpc.protocol.tri.rest.Messages.PARAMETER_VALUE_MISSING;
import static org.apache.dubbo.rpc.protocol.tri.rest.RestConstants.BODY_DECODER_ATTRIBUTE;

@Activate(onClass = "org.springframework.web.bind.annotation.RequestBody")
public class RequestBodyArgumentResolver implements AnnotationBaseArgumentResolver<Annotation> {

    @Override
    public Class<Annotation> accept() {
        return Annotations.RequestBody.type();
    }

    @Override
    public Object resolve(
            ParameterMeta parameter,
            AnnotationMeta<Annotation> annotation,
            HttpRequest request,
            HttpResponse response) {
        Class<?> type = parameter.getActualType();
        if (type == byte[].class) {
            try {
                return StreamUtils.readBytes(request.inputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Object value = null;
        HttpMessageDecoder decoder = request.attribute(BODY_DECODER_ATTRIBUTE);
        if (decoder != null) {
            value = decoder.decode(request.inputStream(), type, Charset.forName(request.charset()));
        }
        if (value == null && Helper.isRequired(annotation)) {
            String typeName = parameter.getType().getSimpleName();
            throw new IllegalArgumentException(PARAMETER_VALUE_MISSING.format(parameter.getName(), typeName));
        }
        return value;
    }
}
