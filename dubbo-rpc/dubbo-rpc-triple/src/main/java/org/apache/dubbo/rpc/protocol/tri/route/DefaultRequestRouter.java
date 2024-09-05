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
package org.apache.dubbo.rpc.protocol.tri.route;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.remoting.http12.message.HttpMessageAdapterFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;

import java.util.List;

public final class DefaultRequestRouter implements RequestRouter {

    private final HttpMessageAdapterFactory<HttpRequest, HttpMetadata, Void> httpMessageAdapterFactory;
    private final List<RequestHandlerMapping> requestHandlerMappings;

    @SuppressWarnings("unchecked")
    public DefaultRequestRouter(FrameworkModel frameworkModel) {
        httpMessageAdapterFactory = frameworkModel.getFirstActivateExtension(HttpMessageAdapterFactory.class);
        requestHandlerMappings = frameworkModel.getActivateExtensions(RequestHandlerMapping.class);
    }

    @Override
    public RpcInvocationBuildContext route(URL url, RequestMetadata metadata, HttpChannel httpChannel) {
        HttpRequest request = httpMessageAdapterFactory.adaptRequest(metadata, httpChannel);
        HttpResponse response = httpMessageAdapterFactory.adaptResponse(request, metadata);

        for (int i = 0, size = requestHandlerMappings.size(); i < size; i++) {
            RequestHandlerMapping mapping = requestHandlerMappings.get(i);
            RequestHandler handler = mapping.getRequestHandler(url, request, response);
            if (handler == null) {
                continue;
            }
            handler.setAttribute(TripleConstants.HANDLER_TYPE_KEY, mapping.getType());
            handler.setAttribute(TripleConstants.HTTP_REQUEST_KEY, request);
            handler.setAttribute(TripleConstants.HTTP_RESPONSE_KEY, response);
            return handler;
        }

        return null;
    }
}
