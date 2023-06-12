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
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import java.util.List;


public class RestResponseInterceptorChain {

    private List<RestResponseInterceptor> interceptors;
    int pos = 0;

    public RestResponseInterceptorChain(List<RestResponseInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    public void intercept(URL url, RequestFacade request, NettyHttpResponse response, Object result, RpcInvocation rpcInvocation, RestResponseInterceptorChain interceptorChain, ServiceDeployer serviceDeployer) throws Exception {
        if (pos < interceptors.size()) {
            interceptors.get(pos++).intercept(url, request, response, result, rpcInvocation, interceptorChain,serviceDeployer);
        }
    }


}
