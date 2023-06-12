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
package org.apache.dubbo.rpc.protocol.rest.extension.resteay.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.rest.extension.ServiceDeployerContext;
import org.apache.dubbo.rpc.protocol.rest.filter.RestFilter;
import org.apache.dubbo.rpc.protocol.rest.extension.resteay.ResteasyContext;
import org.apache.dubbo.rpc.protocol.rest.filter.RestFilterChain;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.jboss.resteasy.specimpl.BuiltResponse;

import javax.ws.rs.container.ContainerRequestFilter;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.RESTEASY_NETTY_HTTP_REQUEST_ATTRIBUTE_KEY;


@Activate(value = "resteasy", onClass = {"javax.ws.rs.container.ContainerRequestFilter", "org.jboss.resteasy.plugins.server.netty.NettyHttpRequest"}, order = Integer.MAX_VALUE - 1)
public class ResteasyContainerFilterAdapter implements RestFilter, ServiceDeployerContext, ResteasyContext {


    @Override
    public void filter(URL url, RequestFacade requestFacade, NettyHttpResponse response, RestFilterChain restFilterChain) throws Exception {

        List<ContainerRequestFilter> containerRequestFilters = getExtension(ContainerRequestFilter.class);

        if (containerRequestFilters.isEmpty()) {
            restFilterChain.filter(url, requestFacade, response, restFilterChain);
            return;
        }


        DubboPreMatchContainerRequestContext containerRequestContext = convertHttpRequestToContainerRequestContext(requestFacade, containerRequestFilters.toArray(new ContainerRequestFilter[0]));

        RpcContext.getServiceContext().setObjectAttachment(RESTEASY_NETTY_HTTP_REQUEST_ATTRIBUTE_KEY, containerRequestContext.getHttpRequest());


        // TODO add headers to response

        try {
            BuiltResponse restResponse = containerRequestContext.filter();

            if (restResponse == null) {
                restFilterChain.filter(url, requestFacade, response, restFilterChain);
                return;
            }

            addResponseHeaders(response, restResponse.getHeaders());
            writeResteasyResponse(url, requestFacade, response, restResponse);
        } catch (Throwable e) {
            throw new RuntimeException("dubbo rest resteasy ContainerRequestFilter write response encode error", e);
        }


    }


}
