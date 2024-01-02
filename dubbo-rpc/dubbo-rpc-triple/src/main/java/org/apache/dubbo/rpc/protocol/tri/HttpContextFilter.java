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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcServiceContext;

@Activate(group = CommonConstants.PROVIDER, order = -29000)
public class HttpContextFilter implements Filter, BaseFilter.Listener {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (invocation.get(TripleConstant.HANDLER_TYPE_KEY) == null) {
            return invoker.invoke(invocation);
        }

        HttpRequest request = (HttpRequest) invocation.get(TripleConstant.HTTP_REQUEST_KEY);
        HttpResponse response = (HttpResponse) invocation.get(TripleConstant.HTTP_RESPONSE_KEY);
        RpcServiceContext context = RpcContext.getServiceContext();
        context.setRemoteAddress(request.remoteHost(), request.remotePort());
        context.setLocalAddress(request.localHost(), request.localPort());
        context.setRequest(request);
        context.setResponse(response);
        if (response.isCommitted()) {
            return new AppResponse(response);
        }
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        if (invocation.get(TripleConstant.HANDLER_TYPE_KEY) == null) {
            return;
        }

        HttpResponse response = (HttpResponse) invocation.get(TripleConstant.HTTP_RESPONSE_KEY);
        if (response.isEmpty()) {
            return;
        }
        if (response.isCommitted()) {
            appResponse.setValue(response);
            return;
        }
        Object value = appResponse.getValue();
        if (value instanceof HttpResult) {
            HttpResult<?> httpResult = (HttpResult<?>) value;
            if (httpResult.getStatus() != 0) {
                response.setStatus(httpResult.getStatus());
            }
            if (httpResult.getHeaders() != null) {
                response.headers().putAll(httpResult.getHeaders());
            }
            if (httpResult.getBody() != null) {
                response.setBody(httpResult.getBody());
            }
        } else if (response.noContent()) {
            response.setBody(appResponse.hasException() ? appResponse.getException() : value);
        }
        response.commit();
        appResponse.setValue(response);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {}
}
