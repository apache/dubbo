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
package org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.AnnotationBaseArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;

@Activate(onClass = "org.jboss.resteasy.annotations.Body")
public class BodyArgumentResolver implements AnnotationBaseArgumentResolver<Annotation> {

    @Override
    public Class<Annotation> accept() {
        return Annotations.Body.type();
    }

    @Override
    public NamedValueMeta getNamedValueMeta(ParameterMeta parameter, AnnotationMeta<Annotation> annotation) {
        return new NamedValueMeta().setParamType(ParamType.Body);
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
                throw new RestException(e);
            }
        }
        return RequestUtils.decodeBody(request, parameter.getActualGenericType());
    }
}
