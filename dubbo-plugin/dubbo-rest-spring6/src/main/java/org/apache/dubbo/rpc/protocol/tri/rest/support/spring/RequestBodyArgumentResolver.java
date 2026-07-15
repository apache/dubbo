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
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;

@Activate(onClass = "org.springframework.web.bind.annotation.RequestBody")
public class RequestBodyArgumentResolver extends AbstractSpringArgumentResolver {

    @Override
    public Class<Annotation> accept() {
        return Annotations.RequestBody.type();
    }

    @Override
    protected ParamType getParamType(NamedValueMeta meta) {
        return ParamType.Body;
    }

    @Override
    protected NamedValueMeta createNamedValueMeta(ParameterMeta param, AnnotationMeta<Annotation> anno) {
        return new NamedValueMeta(null, Helper.isRequired(anno));
    }

    @Override
    protected Object resolveValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        if (RequestUtils.isFormOrMultiPart(request)) {
            if (meta.parameter().isSimple()) {
                return request.formParameter(meta.name());
            }
            return meta.parameter().bind(request, response);
        }
        return RequestUtils.decodeBody(request, meta.genericType());
    }

    @Override
    protected Object resolveCollectionValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        Class<?> type = meta.type();
        if (type == byte[].class) {
            try {
                return StreamUtils.readBytes(request.inputStream());
            } catch (IOException e) {
                throw new RestException(e);
            }
        }
        if (RequestUtils.isFormOrMultiPart(request)) {
            return request.formParameterValues(meta.name());
        }
        return RequestUtils.decodeBody(request, meta.genericType());
    }

    @Override
    protected Object resolveMapValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        if (RequestUtils.isFormOrMultiPart(request)) {
            return RequestUtils.getFormParametersMap(request);
        }
        return RequestUtils.decodeBody(request, meta.genericType());
    }
}
