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
package org.apache.dubbo.rpc.protocol.tri.rest.filter;

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;

public abstract class RestHeaderFilterAdapter implements HeaderFilter {

    @Override
    public RpcInvocation invoke(Invoker<?> invoker, RpcInvocation invocation) throws RpcException {
        if (TripleConstant.TRIPLE_HANDLER_TYPE_REST.equals(invocation.get(TripleConstant.HANDLER_TYPE_KEY))) {
            HttpRequest request = (HttpRequest) invocation.get(TripleConstant.HTTP_REQUEST_KEY);
            HttpResponse response = (HttpResponse) invocation.get(TripleConstant.HTTP_RESPONSE_KEY);
            invoke(invoker, invocation, request, response);
        }
        return invocation;
    }

    protected abstract void invoke(
            Invoker<?> invoker, RpcInvocation invocation, HttpRequest request, HttpResponse response)
            throws RpcException;
}
