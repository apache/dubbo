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
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpCookie;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpRequest.FileUpload;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.rest.Param;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.RestParameterException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.AbstractAnnotationBaseArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Activate
public class ParamArgumentResolver extends AbstractAnnotationBaseArgumentResolver {

    @Override
    protected NamedValueMeta createNamedValueMeta(ParameterMeta param, AnnotationMeta<Annotation> anno) {
        String defaultValue = anno.getString("defaultValue");
        if (Param.DEFAULT_NONE.equals(defaultValue)) {
            defaultValue = null;
        }
        return new NamedValueMeta(anno.getValue(), anno.getBoolean("required"), defaultValue)
                .setParamType(anno.getEnum("type"));
    }

    @Override
    public Class<Annotation> accept() {
        return Annotations.Param.type();
    }

    @Override
    protected Object resolveValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        switch (meta.paramType()) {
            case PathVariable:
                return resolvePathVariable(meta, request);
            case MatrixVariable:
                return CollectionUtils.first(resolveMatrixVariable(meta, request));
            case Param:
                return request.parameter(meta.name());
            case Form:
                return request.formParameter(meta.name());
            case Header:
                return request.header(meta.name());
            case Cookie:
                return request.cookie(meta.name());
            case Attribute:
                return request.attribute(meta.name());
            case Part:
                return request.part(meta.name());
            case Body:
                if (RequestUtils.isFormOrMultiPart(request)) {
                    return request.formParameter(meta.name());
                }
                return RequestUtils.decodeBody(request, meta.genericType());
        }
        return null;
    }

    @Override
    protected Object filterValue(Object value, NamedValueMeta meta) {
        return StringUtils.EMPTY_STRING.equals(value) ? meta.defaultValue() : value;
    }

    @Override
    protected Object resolveCollectionValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        switch (meta.paramType()) {
            case PathVariable:
                String value = resolvePathVariable(meta, request);
                return value == null ? Collections.emptyList() : Collections.singletonList(value);
            case MatrixVariable:
                return resolveMatrixVariable(meta, request);
            case Param:
                return request.parameterValues(meta.name());
            case Form:
                return request.formParameterValues(meta.name());
            case Header:
                return request.headerValues(meta.name());
            case Cookie:
                Collection<HttpCookie> cookies = request.cookies();
                if (cookies.isEmpty()) {
                    return Collections.emptyList();
                }
                String name = meta.name();
                List<HttpCookie> result = new ArrayList<>(cookies.size());
                for (HttpCookie cookie : cookies) {
                    if (name.equals(cookie.name())) {
                        result.add(cookie);
                    }
                }
                return result;
            case Attribute:
                return request.attribute(meta.name());
            case Part:
                return meta.type() == byte[].class ? request.part(meta.name()) : request.parts();
            case Body:
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
        return null;
    }

    @Override
    protected Object resolveMapValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        switch (meta.paramType()) {
            case PathVariable:
                String value = resolvePathVariable(meta, request);
                return value == null ? Collections.emptyMap() : Collections.singletonMap(meta.name(), value);
            case MatrixVariable:
                return CollectionUtils.first(resolveMatrixVariable(meta, request));
            case Param:
                return RequestUtils.getParametersMap(request);
            case Form:
                return RequestUtils.getFormParametersMap(request);
            case Header:
                return request.headers().asMap();
            case Cookie:
                Collection<HttpCookie> cookies = request.cookies();
                if (cookies.isEmpty()) {
                    return Collections.emptyMap();
                }
                Map<String, List<HttpCookie>> mapValue = CollectionUtils.newLinkedHashMap(cookies.size());
                for (HttpCookie cookie : cookies) {
                    mapValue.computeIfAbsent(cookie.name(), k -> new ArrayList<>())
                            .add(cookie);
                }
                return mapValue;
            case Attribute:
                return request.attributes();
            case Part:
                Collection<FileUpload> parts = request.parts();
                if (parts.isEmpty()) {
                    return Collections.emptyMap();
                }
                Map<String, FileUpload> result = new LinkedHashMap<>(parts.size());
                for (FileUpload part : parts) {
                    result.put(part.name(), part);
                }
                return result;
            case Body:
                if (RequestUtils.isFormOrMultiPart(request)) {
                    return RequestUtils.getFormParametersMap(request);
                }
                return RequestUtils.decodeBody(request, meta.genericType());
        }
        return null;
    }

    private static String resolvePathVariable(NamedValueMeta meta, HttpRequest request) {
        Map<String, String> variableMap = request.attribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variableMap == null) {
            if (meta.required()) {
                throw new RestParameterException(Messages.ARGUMENT_VALUE_MISSING, meta.name(), meta.type());
            }
            return null;
        }
        String value = variableMap.get(meta.name());
        if (value == null) {
            return null;
        }
        int index = value.indexOf(';');
        return RequestUtils.decodeURL(index == -1 ? value : value.substring(0, index));
    }

    private static List<String> resolveMatrixVariable(NamedValueMeta meta, HttpRequest request) {
        Map<String, String> variableMap = request.attribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return RequestUtils.parseMatrixVariableValues(variableMap, meta.name());
    }
}
