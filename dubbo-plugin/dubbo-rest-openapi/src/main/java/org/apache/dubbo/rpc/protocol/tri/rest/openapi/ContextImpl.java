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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;

final class ContextImpl extends AbstractContext implements Context {

    private final OpenAPIRequest request;

    private Boolean openAPI31;
    private Holder<HttpRequest> httpRequest;
    private Holder<HttpResponse> httpResponse;

    ContextImpl(OpenAPI openAPI, SchemaResolver schemaResolver, ExtensionFactory extFactory, OpenAPIRequest request) {
        super(openAPI, schemaResolver, extFactory);
        this.request = request;
    }

    @Override
    public boolean isOpenAPI31() {
        if (openAPI31 == null) {
            String v = request.getOpenapi();
            openAPI31 = v != null && v.startsWith("3.1.");
        }
        return openAPI31;
    }

    @Override
    public OpenAPIRequest getRequest() {
        return request;
    }

    @Override
    public HttpRequest getHttpRequest() {
        Holder<HttpRequest> holder = httpRequest;
        if (holder == null) {
            holder = new Holder<>();
            holder.set(RpcContext.getServiceContext().getRequest(HttpRequest.class));
            httpRequest = holder;
        }
        return holder.get();
    }

    @Override
    public HttpResponse getHttpResponse() {
        Holder<HttpResponse> holder = httpResponse;
        if (holder == null) {
            holder = new Holder<>();
            holder.set(RpcContext.getServiceContext().getResponse(HttpResponse.class));
            httpResponse = holder;
        }
        return holder.get();
    }
}
