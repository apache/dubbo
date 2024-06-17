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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpRequest.FileUpload;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.message.codec.CodecUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.AbstractArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import java.util.List;
import java.util.Map;

@Activate(order = Integer.MAX_VALUE - 10000)
public class FallbackArgumentResolver extends AbstractArgumentResolver {

    private final FrameworkModel frameworkModel;
    private final CodecUtils codecUtils;

    public FallbackArgumentResolver(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        codecUtils = frameworkModel.getBeanFactory().getOrRegisterBean(CodecUtils.class);
    }

    @Override
    public boolean accept(ParameterMeta param) {
        return param.getToolKit().getDialect() == RestConstants.DIALECT_BASIC;
    }

    @Override
    protected NamedValueMeta createNamedValueMeta(ParameterMeta param) {
        return new NamedValueMeta(param.isAnnotated(Annotations.Nonnull), null);
    }

    @Override
    protected String emptyDefaultValue(NamedValueMeta meta) {
        return StringUtils.EMPTY_STRING;
    }

    @Override
    protected Object resolveValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return doResolveValue(meta, request, true);
    }

    @Override
    protected Object resolveCollectionValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return doResolveValue(meta, request, false);
    }

    protected Object doResolveValue(NamedValueMeta meta, HttpRequest request, boolean single) {
        if (HttpMethods.supportBody(request.method())) {
            Object body = RequestUtils.decodeBody(request);
            if (body != null) {
                if (body instanceof List) {
                    List<?> list = (List<?>) body;
                    int index = meta.parameterMeta().getIndex();
                    if (index >= 0 && list.size() > index) {
                        return list.get(index);
                    }
                } else if (body instanceof Map) {
                    Object value = ((Map<?, ?>) body).get(meta.name());
                    if (value != null) {
                        return value;
                    }
                }
            }
            if (RequestUtils.isMultiPart(request)) {
                FileUpload fu = request.part(meta.name());
                if (fu != null) {
                    return codecUtils
                            .determineHttpMessageDecoder(null, frameworkModel, fu.contentType())
                            .decode(fu.inputStream(), meta.type(), request.charsetOrDefault());
                }
            }
        }
        return single ? request.parameter(meta.name()) : request.parameterValues(meta.name());
    }

    @Override
    protected Object resolveMapValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return resolveValue(meta, request, response);
    }
}
