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
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.rest.Param;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.model.MethodDescriptor.RpcType;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.AbstractArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta.StreamParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestUtils;

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
        boolean noBodyParam = true;
        int paramCount = -1;
        if (param instanceof MethodParameterMeta) {
            MethodMeta methodMeta = ((MethodParameterMeta) param).getMethodMeta();
            ParameterMeta[] paramMetas = methodMeta.getParameters();
            for (ParameterMeta paramMeta : paramMetas) {
                AnnotationMeta<Param> anno = paramMeta.findAnnotation(Param.class);
                if (anno != null && anno.getAnnotation().type() == ParamType.Body) {
                    noBodyParam = false;
                    break;
                }
            }
            paramCount = methodMeta.getMethodDescriptor().getRpcType() != RpcType.UNARY ? 1 : paramMetas.length;
        } else if (param instanceof StreamParameterMeta) {
            paramCount = 1;
            noBodyParam = false;
        }
        return new FallbackNamedValueMeta(param.isAnnotated(Annotations.Nonnull), noBodyParam, paramCount);
    }

    @Override
    protected Object resolveValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return doResolveValue(meta, true, request, response);
    }

    @Override
    protected Object resolveCollectionValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return doResolveValue(meta, false, request, response);
    }

    protected Object doResolveValue(NamedValueMeta meta, boolean single, HttpRequest request, HttpResponse response) {
        FallbackNamedValueMeta fm = (FallbackNamedValueMeta) meta;

        if (HttpMethods.supportBody(request.method())) {
            if (fm.paramCount == 1) {
                try {
                    Object body = RequestUtils.decodeBody(request, meta.genericType());
                    if (body != null) {
                        if (body != RequestUtils.EMPTY_BODY) {
                            return body;
                        }
                        Object value = single ? request.parameter(meta.name()) : request.parameterValues(meta.name());
                        return value == null ? body : value;
                    }
                } catch (DecodeException ignored) {
                }
            }
            if (fm.noBodyParam) {
                Object body = RequestUtils.decodeBodyAsObject(request);
                if (body instanceof List) {
                    List<?> list = (List<?>) body;
                    if (list.size() == fm.paramCount) {
                        return list.get(meta.parameter().getIndex());
                    }
                } else if (body instanceof Map) {
                    Object value = ((Map<?, ?>) body).get(meta.name());
                    if (value != null) {
                        return value;
                    }
                }
            }
        }

        if (single) {
            if (Map.class.isAssignableFrom(meta.type())) {
                return RequestUtils.getParametersMap(request);
            }
            String value = request.parameter(meta.name());
            if (meta.parameter().isSimple() || RestUtils.isMaybeJSONObject(value)) {
                return value;
            }
            return meta.parameter().bind(request, response);
        }

        return request.parameterValues(meta.name());
    }

    @Override
    protected Object resolveMapValue(NamedValueMeta meta, HttpRequest request, HttpResponse response) {
        return resolveValue(meta, request, response);
    }

    private static final class FallbackNamedValueMeta extends NamedValueMeta {

        private final boolean noBodyParam;
        private final int paramCount;

        FallbackNamedValueMeta(boolean required, boolean noBodyParam, int paramCount) {
            super(null, required);
            this.noBodyParam = noBodyParam;
            this.paramCount = paramCount;
        }
    }
}
