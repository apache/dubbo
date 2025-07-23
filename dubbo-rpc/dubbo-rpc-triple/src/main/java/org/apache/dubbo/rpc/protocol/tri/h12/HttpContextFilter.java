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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;

@Activate(group = CommonConstants.PROVIDER, order = -29000)
public class HttpContextFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (invocation.get(TripleConstants.HANDLER_TYPE_KEY) == null) {
            return invoker.invoke(invocation);
        }

        HttpRequest request = (HttpRequest) invocation.get(TripleConstants.HTTP_REQUEST_KEY);
        HttpResponse response = (HttpResponse) invocation.get(TripleConstants.HTTP_RESPONSE_KEY);
        RpcServiceContext context = RpcContext.getServiceContext();
        context.setRemoteAddress(request.remoteHost(), request.remotePort());
        if (context.getLocalAddress() == null) {
            context.setLocalAddress(request.localHost(), request.localPort());
        }
        context.setRequest(request);
        context.setResponse(response);
        return invoker.invoke(invocation);
    }
}
