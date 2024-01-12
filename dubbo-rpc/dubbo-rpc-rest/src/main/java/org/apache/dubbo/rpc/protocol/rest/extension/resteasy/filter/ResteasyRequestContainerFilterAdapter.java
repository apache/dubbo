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
package org.apache.dubbo.rpc.protocol.rest.extension.resteasy.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.extension.resteasy.ResteasyContext;
import org.apache.dubbo.rpc.protocol.rest.filter.RestRequestFilter;
import org.apache.dubbo.rpc.protocol.rest.filter.context.RestFilterContext;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import javax.ws.rs.container.ContainerRequestFilter;

import java.util.List;

import org.jboss.resteasy.specimpl.BuiltResponse;

@Activate(
        value = "resteasy",
        onClass = {
            "javax.ws.rs.container.ContainerRequestFilter",
            "org.jboss.resteasy.plugins.server.netty.NettyHttpRequest",
            "org.jboss.resteasy.plugins.server.netty.NettyHttpResponse"
        },
        order = Integer.MAX_VALUE - 1)
public class ResteasyRequestContainerFilterAdapter implements RestRequestFilter, ResteasyContext {

    @Override
    public void filter(RestFilterContext restFilterContext) throws Exception {

        ServiceDeployer serviceDeployer = restFilterContext.getServiceDeployer();
        RequestFacade requestFacade = restFilterContext.getRequestFacade();
        URL url = restFilterContext.getUrl();
        NettyHttpResponse response = restFilterContext.getResponse();

        List<ContainerRequestFilter> containerRequestFilters =
                getExtension(serviceDeployer, ContainerRequestFilter.class);

        if (containerRequestFilters.isEmpty()) {

            return;
        }

        DubboPreMatchContainerRequestContext containerRequestContext = convertHttpRequestToContainerRequestContext(
                requestFacade, containerRequestFilters.toArray(new ContainerRequestFilter[0]));

        // set resteasy request for save user`s custom  request attribute
        restFilterContext.setOriginRequest(containerRequestContext.getHttpRequest());

        try {
            BuiltResponse restResponse = containerRequestContext.filter();

            if (restResponse == null) {
                return;
            }

            addResponseHeaders(response, restResponse.getHeaders());
            writeResteasyResponse(url, requestFacade, response, restResponse);
            // completed
            restFilterContext.setComplete(true);
        } catch (Throwable e) {
            throw new RuntimeException("dubbo rest resteasy ContainerRequestFilter write response encode error", e);
        } finally {
            containerRequestContext.getHttpRequest().releaseContentBuffer();
        }
    }
}
