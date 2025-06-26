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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter.Listener;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ResponseMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Activate(order = -10000, onClass = "org.springframework.http.ResponseEntity")
public class SpringResponseRestFilter implements RestFilter, Listener {

    private final ArgumentResolver argumentResolver;
    private final Map<Key, Optional<MethodMeta>> cache = CollectionUtils.newConcurrentHashMap();

    public SpringResponseRestFilter(FrameworkModel frameworkModel) {
        argumentResolver = frameworkModel.getOrRegisterBean(CompositeArgumentResolver.class);
    }

    @Override
    public void onResponse(Result result, HttpRequest request, HttpResponse response) {
        if (result.hasException()) {
            HandlerMeta handler = request.attribute(RestConstants.HANDLER_ATTRIBUTE);
            if (handler == null) {
                return;
            }

            Throwable t = result.getException();
            Key key = new Key(handler.getMethod().getMethod(), t.getClass());
            cache.computeIfAbsent(key, k -> findSuitableExceptionHandler(handler.getService(), k.type))
                    .ifPresent(m -> {
                        try {
                            result.setValue(invokeExceptionHandler(m, t, request, response));
                            result.setException(null);
                        } catch (Throwable th) {
                            result.setException(th);
                        }
                    });
            return;
        }

        Object value = result.getValue();
        if (value instanceof ResponseEntity) {
            ResponseEntity<?> entity = (ResponseEntity<?>) value;
            result.setValue(HttpResult.builder()
                    .body(entity.getBody())
                    .status(Helper.getStatusCode(entity))
                    .headers(entity.getHeaders())
                    .build());
            return;
        }

        RequestMapping mapping = request.attribute(RestConstants.MAPPING_ATTRIBUTE);
        if (mapping == null) {
            return;
        }
        ResponseMeta responseMeta = mapping.getResponse();
        if (responseMeta == null) {
            return;
        }
        String reason = responseMeta.getReason();
        result.setValue(HttpResult.builder()
                .body(reason == null ? result.getValue() : reason)
                .status(responseMeta.getStatus())
                .build());
    }

    private Optional<MethodMeta> findSuitableExceptionHandler(ServiceMeta serviceMeta, Class<?> exType) {
        if (serviceMeta.getExceptionHandlers() == null) {
            return Optional.empty();
        }
        List<Pair<Class<?>, MethodMeta>> candidates = new ArrayList<>();
        for (MethodMeta methodMeta : serviceMeta.getExceptionHandlers()) {
            ExceptionHandler anno = methodMeta.getMethod().getAnnotation(ExceptionHandler.class);
            if (anno == null) {
                continue;
            }
            for (Class<?> type : anno.value()) {
                if (type.isAssignableFrom(exType)) {
                    candidates.add(Pair.of(type, methodMeta));
                }
            }
        }
        int size = candidates.size();
        if (size == 0) {
            return Optional.empty();
        }
        if (size > 1) {
            candidates.sort((o1, o2) -> {
                Class<?> c1 = o1.getLeft();
                Class<?> c2 = o2.getLeft();
                if (c1.equals(c2)) {
                    return 0;
                }
                return c1.isAssignableFrom(c2) ? 1 : -1;
            });
        }
        return Optional.of(candidates.get(0).getRight());
    }

    private Object invokeExceptionHandler(MethodMeta meta, Throwable t, HttpRequest request, HttpResponse response) {
        ParameterMeta[] parameters = meta.getParameters();
        int len = parameters.length;
        Object[] args = new Object[len];
        for (int i = 0; i < len; i++) {
            ParameterMeta parameter = parameters[i];
            if (parameter.getType().isInstance(t)) {
                args[i] = t;
            } else {
                args[i] = argumentResolver.resolve(parameter, request, response);
            }
        }
        Object value;
        try {
            value = meta.getMethod().invoke(meta.getServiceMeta().getService(), args);
        } catch (Exception e) {
            throw new RestException("Failed to invoke exception handler method: " + meta.getMethod(), e);
        }
        AnnotationMeta<?> rs = meta.getAnnotation(ResponseStatus.class);
        if (rs == null) {
            return value;
        }
        HttpStatus status = rs.getEnum("value");
        String reason = rs.getString("reason");
        return HttpResult.builder()
                .body(StringUtils.isEmpty(reason) ? value : reason)
                .status(status.value())
                .build();
    }

    private static final class Key {
        private final Method method;
        private final Class<?> type;

        Key(Method method, Class<?> type) {
            this.method = method;
            this.type = type;
        }

        @Override
        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass", "EqualsDoesntCheckParameterClass"})
        public boolean equals(Object obj) {
            Key other = (Key) obj;
            return method.equals(other.method) && type.equals(other.type);
        }

        @Override
        public int hashCode() {
            return method.hashCode() * 31 + type.hashCode();
        }
    }
}
