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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.Condition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ConditionWrapper;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ConsumesCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.HeadersCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.MethodsCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ParamsCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ProducesCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ServiceGroupVersionCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.CorsMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ResponseMeta;

import java.util.Objects;

import static org.apache.dubbo.common.utils.ArrayUtils.isEmpty;

public final class RequestMapping implements Condition<RequestMapping, HttpRequest> {

    private final String name;
    private final String sig;
    private final PathCondition pathCondition;
    private final MethodsCondition methodsCondition;
    private final ParamsCondition paramsCondition;
    private final HeadersCondition headersCondition;
    private final ConsumesCondition consumesCondition;
    private final ProducesCondition producesCondition;
    private final ConditionWrapper customCondition;
    private final CorsMeta cors;
    private final ResponseMeta response;

    private int hashCode;

    private RequestMapping(
            String name,
            String sig,
            PathCondition pathCondition,
            MethodsCondition methodsCondition,
            ParamsCondition paramsCondition,
            HeadersCondition headersCondition,
            ConsumesCondition consumesCondition,
            ProducesCondition producesCondition,
            ConditionWrapper customCondition,
            CorsMeta cors,
            ResponseMeta response) {
        this.name = name;
        this.sig = sig;
        this.pathCondition = pathCondition;
        this.methodsCondition = methodsCondition;
        this.paramsCondition = paramsCondition;
        this.headersCondition = headersCondition;
        this.consumesCondition = consumesCondition;
        this.producesCondition = producesCondition;
        this.customCondition = customCondition;
        this.cors = cors;
        this.response = response;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RequestMapping combine(RequestMapping other) {
        return new RequestMapping(
                combineName(name, other.name, other.sig),
                other.sig,
                combine(pathCondition, other.pathCondition),
                combine(methodsCondition, other.methodsCondition),
                combine(paramsCondition, other.paramsCondition),
                combine(headersCondition, other.headersCondition),
                combine(consumesCondition, other.consumesCondition),
                combine(producesCondition, other.producesCondition),
                combine(customCondition, other.customCondition),
                CorsMeta.combine(cors, other.cors),
                ResponseMeta.combine(response, other.response));
    }

    private String combineName(String name, String otherName, String otherSig) {
        if (name == null) {
            name = otherName;
        } else if (otherName != null) {
            name += '#' + otherName;
        }
        return otherSig == null ? name : name + '~' + otherSig;
    }

    private <T extends Condition<T, HttpRequest>> T combine(T source, T other) {
        return source == null ? other : other == null ? source : source.combine(other);
    }

    public RequestMapping match(HttpRequest request, PathExpression path) {
        return doMatch(request, new PathCondition(path));
    }

    public boolean matchMethod(String method) {
        return methodsCondition == null || methodsCondition.getMethods().contains(method);
    }

    public boolean matchParams(HttpRequest request) {
        return paramsCondition == null || paramsCondition.match(request) != null;
    }

    public boolean matchConsumes(HttpRequest request) {
        return consumesCondition == null || consumesCondition.match(request) != null;
    }

    public boolean matchProduces(HttpRequest request) {
        return producesCondition == null || producesCondition.match(request) != null;
    }

    @Override
    public RequestMapping match(HttpRequest request) {
        return doMatch(request, null);
    }

    private RequestMapping doMatch(HttpRequest request, PathCondition pathCondition) {
        MethodsCondition methods = null;
        if (methodsCondition != null) {
            methods = methodsCondition.match(request);
            if (methods == null) {
                return null;
            }
        }

        PathCondition paths = pathCondition;
        if (paths == null && this.pathCondition != null) {
            paths = this.pathCondition.match(request);
            if (paths == null) {
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

        ConditionWrapper custom = null;
        if (customCondition != null) {
            custom = customCondition.match(request);
            if (custom == null) {
                return null;
            }
        }

        if (StringUtils.isNotEmpty(sig)) {
            String rSig = request.attribute(RestConstants.SIG_ATTRIBUTE);
            if (rSig != null && !rSig.equals(sig)) {
                return null;
            }
        }

        return new RequestMapping(
                name, sig, paths, methods, params, headers, consumes, produces, custom, cors, response);
    }

    public String getName() {
        return name;
    }

    public String getSig() {
        return sig;
    }

    public PathCondition getPathCondition() {
        return pathCondition;
    }

    public MethodsCondition getMethodsCondition() {
        return methodsCondition;
    }

    public ParamsCondition getParamsCondition() {
        return paramsCondition;
    }

    public HeadersCondition getHeadersCondition() {
        return headersCondition;
    }

    public ConsumesCondition getConsumesCondition() {
        return consumesCondition;
    }

    public ProducesCondition getProducesCondition() {
        return producesCondition;
    }

    public CorsMeta getCors() {
        return cors;
    }

    public ResponseMeta getResponse() {
        return response;
    }

    @Override
    public int compareTo(RequestMapping other, HttpRequest request) {
        int result;
        if (methodsCondition != null && HttpMethods.HEAD.name().equals(request.method())) {
            result = methodsCondition.compareTo(other.methodsCondition, request);
            if (result != 0) {
                return result;
            }
        }
        if (pathCondition != null) {
            result = pathCondition.compareTo(other.pathCondition, request);
            if (result != 0) {
                return result;
            }
        }
        if (paramsCondition != null) {
            result = paramsCondition.compareTo(other.paramsCondition, request);
            if (result != 0) {
                return result;
            }
        }
        if (headersCondition != null) {
            result = headersCondition.compareTo(other.headersCondition, request);
            if (result != 0) {
                return result;
            }
        }
        if (consumesCondition != null) {
            result = consumesCondition.compareTo(other.consumesCondition, request);
            if (result != 0) {
                return result;
            }
        }
        if (producesCondition != null) {
            result = producesCondition.compareTo(other.producesCondition, request);
            if (result != 0) {
                return result;
            }
        }
        if (methodsCondition != null) {
            result = methodsCondition.compareTo(other.methodsCondition, request);
            if (result != 0) {
                return result;
            }
        }
        if (customCondition != null) {
            result = customCondition.compareTo(other.customCondition, request);
            if (result != 0) {
                return result;
            }
        }
        if (sig != null && other.sig != null) {
            int size = request.queryParameters().size();
            int size1 = sig.length();
            int size2 = other.sig.length();
            if (size1 == size) {
                if (size2 != size) {
                    return -1;
                }
            } else if (size2 == size) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = Objects.hash(
                    pathCondition,
                    methodsCondition,
                    paramsCondition,
                    headersCondition,
                    consumesCondition,
                    producesCondition,
                    customCondition,
                    sig);
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != RequestMapping.class) {
            return false;
        }
        RequestMapping other = (RequestMapping) obj;
        return Objects.equals(pathCondition, other.pathCondition)
                && Objects.equals(methodsCondition, other.methodsCondition)
                && Objects.equals(paramsCondition, other.paramsCondition)
                && Objects.equals(headersCondition, other.headersCondition)
                && Objects.equals(consumesCondition, other.consumesCondition)
                && Objects.equals(producesCondition, other.producesCondition)
                && Objects.equals(customCondition, other.customCondition)
                && Objects.equals(sig, other.sig);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RequestMapping{name='");
        sb.append(name).append('\'');
        if (pathCondition != null) {
            sb.append(", path=").append(pathCondition);
        }
        if (methodsCondition != null) {
            sb.append(", methods=").append(methodsCondition);
        }
        if (paramsCondition != null) {
            sb.append(", params=").append(paramsCondition);
        }
        if (headersCondition != null) {
            sb.append(", headers=").append(headersCondition);
        }
        if (consumesCondition != null) {
            sb.append(", consumes=").append(consumesCondition);
        }
        if (producesCondition != null) {
            sb.append(", produces=").append(producesCondition);
        }
        if (customCondition != null) {
            sb.append(", custom=").append(customCondition);
        }
        if (response != null) {
            sb.append(", response=").append(response);
        }
        if (cors != null) {
            sb.append(", cors=").append(cors);
        }
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {

        private String name;
        private String sig;
        private String contextPath;
        private String[] paths;
        private String[] methods;
        private String[] params;
        private String[] headers;
        private String[] consumes;
        private String[] produces;
        private Condition<?, HttpRequest> customCondition;
        private CorsMeta cors;
        private Integer responseStatus;
        private String responseReason;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder sig(String sig) {
            this.sig = sig;
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

        public Builder service(String ServiceGroup, String ServiceVersion) {
            if (ServiceGroup != null || ServiceVersion != null) {
                customCondition = new ServiceGroupVersionCondition(ServiceGroup, ServiceVersion);
            }
            return this;
        }

        public Builder cors(CorsMeta cors) {
            this.cors = cors;
            return this;
        }

        public Builder responseStatus(int status) {
            responseStatus = status;
            return this;
        }

        public Builder responseReason(String reason) {
            responseReason = reason;
            return this;
        }

        public RequestMapping build() {
            return new RequestMapping(
                    name,
                    sig,
                    isEmpty(paths) ? null : new PathCondition(contextPath, paths),
                    isEmpty(methods) ? null : new MethodsCondition(methods),
                    isEmpty(params) ? null : new ParamsCondition(params),
                    isEmpty(headers) ? null : new HeadersCondition(headers),
                    isEmpty(consumes) ? null : new ConsumesCondition(consumes),
                    isEmpty(produces) ? null : new ProducesCondition(produces),
                    customCondition == null ? null : ConditionWrapper.wrap(customCondition),
                    cors == null || cors.isEmpty() ? null : cors,
                    responseStatus == null ? null : new ResponseMeta(responseStatus, responseReason));
        }
    }
}
