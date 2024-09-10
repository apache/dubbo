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
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;

public abstract class RestFilterAdapter implements Filter, BaseFilter.Listener {

    @Override
    public final Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (TripleConstants.TRIPLE_HANDLER_TYPE_REST.equals(invocation.get(TripleConstants.HANDLER_TYPE_KEY))) {
            HttpRequest request = (HttpRequest) invocation.get(TripleConstants.HTTP_REQUEST_KEY);
            HttpResponse response = (HttpResponse) invocation.get(TripleConstants.HTTP_RESPONSE_KEY);
            return invoke(invoker, invocation, request, response);
        }
        return invoker.invoke(invocation);
    }

    @Override
    public final void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        if (TripleConstants.TRIPLE_HANDLER_TYPE_REST.equals(invocation.get(TripleConstants.HANDLER_TYPE_KEY))) {
            HttpRequest request = (HttpRequest) invocation.get(TripleConstants.HTTP_REQUEST_KEY);
            HttpResponse response = (HttpResponse) invocation.get(TripleConstants.HTTP_RESPONSE_KEY);
            onResponse(appResponse, invoker, invocation, request, response);
        }
    }

    @Override
    public final void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        if (TripleConstants.TRIPLE_HANDLER_TYPE_REST.equals(invocation.get(TripleConstants.HANDLER_TYPE_KEY))) {
            HttpRequest request = (HttpRequest) invocation.get(TripleConstants.HTTP_REQUEST_KEY);
            HttpResponse response = (HttpResponse) invocation.get(TripleConstants.HTTP_RESPONSE_KEY);
            onError(t, invoker, invocation, request, response);
        }
    }

    protected abstract Result invoke(
            Invoker<?> invoker, Invocation invocation, HttpRequest request, HttpResponse response) throws RpcException;

    protected void onResponse(
            Result result, Invoker<?> invoker, Invocation invocation, HttpRequest request, HttpResponse response) {}

    protected void onError(
            Throwable t, Invoker<?> invoker, Invocation invocation, HttpRequest request, HttpResponse response) {}
}
