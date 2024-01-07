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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.Condition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ConditionWrapper;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ConsumesCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.HeadersCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.MethodsCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ParamsCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ProducesCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ResponseMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.utils.ArrayUtils.isEmpty;

public final class RequestMapping implements Condition<RequestMapping, HttpRequest> {

    private final String name;
    private final PathCondition pathCondition;
    private final MethodsCondition methodsCondition;
    private final ParamsCondition paramsCondition;
    private final HeadersCondition headersCondition;
    private final ConsumesCondition consumesCondition;
    private final ProducesCondition producesCondition;
    private final ConditionWrapper customCondition;
    private final List<MethodMeta> exceptionHandlers;
    private final ResponseMeta response;

    private RequestMapping(
            String name,
            PathCondition pathCondition,
            MethodsCondition methodsCondition,
            ParamsCondition paramsCondition,
            HeadersCondition headersCondition,
            ConsumesCondition consumesCondition,
            ProducesCondition producesCondition,
            Condition<?, HttpRequest> customCondition,
            List<MethodMeta> exceptionHandlers,
            ResponseMeta response) {
        this.name = name;
        this.pathCondition = pathCondition;
        this.methodsCondition = methodsCondition;
        this.paramsCondition = paramsCondition;
        this.headersCondition = headersCondition;
        this.consumesCondition = consumesCondition;
        this.producesCondition = producesCondition;
        this.customCondition = customCondition == null ? null : new ConditionWrapper(customCondition);
        this.exceptionHandlers = exceptionHandlers;
        this.response = response;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RequestMapping combine(RequestMapping other) {
        String name = this.name == null ? other.name : other.name == null ? this.name : this.name + "#" + other.name;
        PathCondition paths = combine(pathCondition, other.pathCondition);
        MethodsCondition methods = combine(methodsCondition, other.methodsCondition);
        ParamsCondition params = combine(paramsCondition, other.paramsCondition);
        HeadersCondition headers = combine(headersCondition, other.headersCondition);
        ConsumesCondition consumes = combine(consumesCondition, other.consumesCondition);
        ProducesCondition produces = combine(producesCondition, other.producesCondition);
        ConditionWrapper custom = combine(customCondition, other.customCondition);
        ResponseMeta response = ResponseMeta.combine(this.response, other.response);
        return new RequestMapping(
                name, paths, methods, params, headers, consumes, produces, custom, exceptionHandlers, response);
    }

    private <T extends Condition<T, HttpRequest>> T combine(T value, T other) {
        return value == null ? other : other == null ? value : value.combine(other);
    }

    @Override
    public RequestMapping match(HttpRequest request) {
        MethodsCondition methods = null;
        if (methodsCondition != null) {
            methods = methodsCondition.match(request);
            if (methods == null) {
                return null;
            }
        }

        ParamsCondition params = null;
        if (paramsCondition != null) {
            params = paramsCondition.match(request);
            if (params == null) {
                return null;
            }
        }

        HeadersCondition headers = null;
        if (headersCondition != null) {
            headers = headersCondition.match(request);
            if (headers == null) {
                return null;
            }
        }

        ConsumesCondition consumes = null;
        if (consumesCondition != null) {
            consumes = consumesCondition.match(request);
            if (consumes == null) {
                return null;
            }
        }

        ProducesCondition produces = null;
        if (producesCondition != null) {
            produces = producesCondition.match(request);
            if (produces == null) {
                return null;
            }
        }

        PathCondition paths = null;
        if (pathCondition != null) {
            paths = pathCondition.match(request);
            if (paths == null) {
                return null;
            }
        }

        ConditionWrapper custom = null;
        if (customCondition != null) {
            custom = customCondition.match(request);
            if (custom == null) {
                return null;
            }
        }

        return new RequestMapping(
                name, paths, methods, params, headers, consumes, produces, custom, exceptionHandlers, response);
    }

    public String getName() {
        return name;
    }

    public PathCondition getPathCondition() {
        return pathCondition;
    }

    public ProducesCondition getProducesCondition() {
        return producesCondition;
    }

    public List<MethodMeta> getExceptionHandlers() {
        return exceptionHandlers;
    }

    public ResponseMeta getResponse() {
        return response;
    }

    @Override
    public int compareTo(RequestMapping other, HttpRequest request) {
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RequestMapping{name='");
        sb.append(name).append('\'');
        if (pathCondition != null) {
            sb.append(", pathCondition=").append(pathCondition);
        }
        if (methodsCondition != null) {
            sb.append(", methodsCondition=").append(methodsCondition);
        }
        if (paramsCondition != null) {
            sb.append(", paramsCondition=").append(paramsCondition);
        }
        if (headersCondition != null) {
            sb.append(", headersCondition=").append(headersCondition);
        }
        if (consumesCondition != null) {
            sb.append(", consumesCondition=").append(consumesCondition);
        }
        if (producesCondition != null) {
            sb.append(", producesCondition=").append(producesCondition);
        }
        if (customCondition != null) {
            sb.append(", customCondition=").append(customCondition);
        }
        if (exceptionHandlers != null) {
            sb.append(", exceptionHandlers=").append(exceptionHandlers);
        }
        if (response != null) {
            sb.append(", response=").append(response);
        }
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {
        private String name;
        private String contextPath;
        private String[] paths;
        private String[] methods;
        private String[] params;
        private String[] headers;
        private String[] consumes;
        private String[] produces;
        private Condition<?, HttpRequest> customCondition;
        private List<MethodMeta> exceptionHandlers;
        private Integer responseStatusCode;
        private String responseReason;
        private Map<String, List<String>> responseHeaders;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder contextPath(String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        public Builder path(String... paths) {
            this.paths = paths;
            return this;
        }

        public Builder method(String... methods) {
            this.methods = methods;
            return this;
        }

        public Builder param(String... params) {
            this.params = params;
            return this;
        }

        public Builder header(String... headers) {
            this.headers = headers;
            return this;
        }

        public Builder consume(String... consumes) {
            this.consumes = consumes;
            return this;
        }

        public Builder produce(String... produces) {
            this.produces = produces;
            return this;
        }

        public Builder custom(Condition<?, HttpRequest> customCondition) {
            this.customCondition = customCondition;
            return this;
        }

        public Builder exceptionHandler(MethodMeta exceptionHandler) {
            if (exceptionHandlers == null) {
                exceptionHandlers = new ArrayList<>();
            }
            exceptionHandlers.add(exceptionHandler);
            return this;
        }

        public Builder responseStatusCode(int statusCode) {
            responseStatusCode = statusCode;
            return this;
        }

        public Builder responseReason(String reason) {
            responseReason = reason;
            return this;
        }

        public Builder responseHeaders(Map<String, List<String>> headers) {
            responseHeaders = headers;
            return this;
        }

        public Builder responseHeader(String name, String value) {
            return responseHeader(name, Collections.singletonList(value));
        }

        public Builder responseHeader(String name, List<String> values) {
            if (responseHeaders == null) {
                responseHeaders = new LinkedHashMap<>();
            }
            responseHeaders.put(name, values);
            return this;
        }

        public RequestMapping build() {
            PathCondition pathCondition = isEmpty(paths) ? null : new PathCondition(contextPath, paths);
            MethodsCondition methodsCondition = isEmpty(methods) ? null : new MethodsCondition(methods);
            ParamsCondition paramsCondition = isEmpty(params) ? null : new ParamsCondition(params);
            HeadersCondition headersCondition = isEmpty(headers) ? null : new HeadersCondition(headers);
            ConsumesCondition consumesCondition = isEmpty(consumes) ? null : new ConsumesCondition(consumes);
            ProducesCondition producesCondition = isEmpty(produces) ? null : new ProducesCondition(produces);
            ResponseMeta response = null;
            if (responseStatusCode != null || responseReason != null || responseHeaders != null) {
                response = new ResponseMeta(responseStatusCode, responseReason, responseHeaders);
            }
            return new RequestMapping(
                    name,
                    pathCondition,
                    methodsCondition,
                    paramsCondition,
                    headersCondition,
                    consumesCondition,
                    producesCondition,
                    customCondition,
                    exceptionHandlers,
                    response);
        }
    }
}
