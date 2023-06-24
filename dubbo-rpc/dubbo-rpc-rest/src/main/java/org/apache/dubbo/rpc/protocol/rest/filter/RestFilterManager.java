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
package org.apache.dubbo.rpc.protocol.rest.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.filter.context.RestFilterContext;
import org.apache.dubbo.rpc.protocol.rest.filter.context.RestInterceptContext;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import java.util.ArrayList;
import java.util.List;

public class RestFilterManager {
    private static final List<RestFilter> restRequestFilters = new ArrayList<>(FrameworkModel.defaultModel().getExtensionLoader(RestRequestFilter.class).getActivateExtensions());
    private static final List<RestFilter> restResponseFilters = new ArrayList<>(FrameworkModel.defaultModel().getExtensionLoader(RestResponseFilter.class).getActivateExtensions());
    private static final List<RestResponseInterceptor> restResponseInterceptors = FrameworkModel.defaultModel().getExtensionLoader(RestResponseInterceptor.class).getActivateExtensions();


    /**
     * execute request filters
     *
     * @param url
     * @param requestFacade
     * @param nettyHttpResponse
     * @throws Exception
     */
    public static void executeRequestFilters(URL url, RequestFacade requestFacade, NettyHttpResponse nettyHttpResponse, ServiceDeployer serviceDeployer) throws Exception {
        RestFilterContext restFilterContext = new RestFilterContext(url, requestFacade, nettyHttpResponse, serviceDeployer);

        for (RestFilter restRequestFilter : restRequestFilters) {
            restRequestFilter.filter(restFilterContext);
            if (restFilterContext.complete()) {
                break;
            }
        }
    }

    /**
     * execute response filters
     *
     * @param url
     * @param requestFacade
     * @param nettyHttpResponse
     * @throws Exception
     */
    public static void executeResponseFilters(URL url, RequestFacade requestFacade, NettyHttpResponse nettyHttpResponse, ServiceDeployer serviceDeployer) throws Exception {
        RestFilterContext restFilterContext = new RestFilterContext(url, requestFacade, nettyHttpResponse, serviceDeployer);

        for (RestFilter restResponseFilter : restResponseFilters) {
            restResponseFilter.filter(restFilterContext);
            if (restFilterContext.complete()) {
                break;
            }
        }
    }


    /**
     * execute response Intercepts
     *
     * @param url
     * @param request
     * @param nettyHttpResponse
     * @param result
     * @param rpcInvocation
     * @param serviceDeployer
     * @throws Exception
     */
    public static void executeResponseIntercepts(URL url, RequestFacade request, NettyHttpResponse nettyHttpResponse, Object result, RpcInvocation rpcInvocation, ServiceDeployer serviceDeployer) throws Exception {
        RestInterceptContext restFilterContext = new RestInterceptContext(url, request, nettyHttpResponse, serviceDeployer, result, rpcInvocation);

        for (RestResponseInterceptor restResponseInterceptor : restResponseInterceptors) {

            restResponseInterceptor.intercept(restFilterContext);

            if (restFilterContext.complete()) {
                break;
            }

        }

    }

}
