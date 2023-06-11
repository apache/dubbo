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
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;


import static org.apache.dubbo.rpc.protocol.rest.filter.ServiceInvokeRestFilter.writeResult;

/**
 *  default RestResponseInterceptor
 */
@Activate(value = "invoke",order = Integer.MAX_VALUE)
public class ServiceInvokeRestResponseInterceptor implements RestResponseInterceptor {

    @Override
    public void intercept(URL url, RequestFacade request, NettyHttpResponse nettyHttpResponse, Object result, RpcInvocation rpcInvocation, RestResponseInterceptorChain interceptorChain) throws Exception {

        writeResult(nettyHttpResponse, request, url, result, rpcInvocation.getReturnType());
        nettyHttpResponse.setStatus(200);
    }
}
