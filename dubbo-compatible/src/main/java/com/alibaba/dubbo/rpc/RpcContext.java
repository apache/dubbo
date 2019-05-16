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

package com.alibaba.dubbo.rpc;

@Deprecated
public class RpcContext extends org.apache.dubbo.rpc.RpcContext {


    public static RpcContext getContext() {
        return newInstance(org.apache.dubbo.rpc.RpcContext.getContext());
    }

    private static RpcContext newInstance(org.apache.dubbo.rpc.RpcContext rpcContext) {
        RpcContext copy = new RpcContext();
        copy.getAttachments().putAll(rpcContext.getAttachments());
        copy.get().putAll(rpcContext.get());
        copy.setFuture(rpcContext.getFuture());
        copy.setUrls(rpcContext.getUrls());
        copy.setUrl(rpcContext.getUrl());
        copy.setMethodName(rpcContext.getMethodName());
        copy.setParameterTypes(rpcContext.getParameterTypes());
        copy.setArguments(rpcContext.getArguments());
        copy.setLocalAddress(rpcContext.getLocalAddress());
        copy.setRemoteAddress(rpcContext.getRemoteAddress());
        copy.setRemoteApplicationName(rpcContext.getRemoteApplicationName());
        copy.setInvokers(rpcContext.getInvokers());
        copy.setInvoker(rpcContext.getInvoker());
        copy.setInvocation(rpcContext.getInvocation());

        copy.setRequest(rpcContext.getRequest());
        copy.setResponse(rpcContext.getResponse());
        copy.setAsyncContext(rpcContext.getAsyncContext());

        return copy;
    }
}
