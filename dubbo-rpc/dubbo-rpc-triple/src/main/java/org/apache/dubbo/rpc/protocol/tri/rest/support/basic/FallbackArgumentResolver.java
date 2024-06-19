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
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.AbstractArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import java.util.List;
import java.util.Map;

@Activate(order = Integer.MAX_VALUE - 10000)
public class FallbackArgumentResolver extends AbstractArgumentResolver {

    @Override
    public boolean accept(ParameterMeta param) {
        return param.getToolKit().getDialect() == RestConstants.DIALECT_BASIC;
    }

    @Override
    protected NamedValueMeta createNamedValueMeta(ParameterMeta param) {
        return new NamedValueMeta(param.isAnnotated(Annotations.Nonnull), null);
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
            ParameterMeta parameterMeta = meta.parameterMeta();
            Object body = RequestUtils.decodeBody(request, parameterMeta.getType(), parameterMeta.isSingle());
            if (body != null) {
                if (parameterMeta.getType().isInstance(body)) {
                    return body;
                } else if (body instanceof List) {
                    List<?> list = (List<?>) body;
                    int index = parameterMeta.getIndex();
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
        }
        return single ? request.parameter(meta.name()) : request.parameterValues(meta.name());
    }

    @Override
    protected Object resolveMapValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return resolveValue(meta, request, response);
    }
}
