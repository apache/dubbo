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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

@Deprecated
public interface ProxyFactory extends org.apache.dubbo.rpc.ProxyFactory {

    <T> T getProxy(com.alibaba.dubbo.rpc.Invoker<T> invoker) throws com.alibaba.dubbo.rpc.RpcException;

    <T> T getProxy(com.alibaba.dubbo.rpc.Invoker<T> invoker, boolean generic) throws com.alibaba.dubbo.rpc.RpcException;

    <T> com.alibaba.dubbo.rpc.Invoker<T> getInvoker(T proxy, Class<T> type, com.alibaba.dubbo.common.URL url) throws com.alibaba.dubbo.rpc.RpcException;

    @Override
    default <T> T getProxy(Invoker<T> invoker) throws RpcException {
        return getProxy(new com.alibaba.dubbo.rpc.Invoker.CompatibleInvoker<>(invoker));
    }

    @Override
    default <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException {
        return getProxy(new com.alibaba.dubbo.rpc.Invoker.CompatibleInvoker<>(invoker), generic);
    }

    @Override
    default <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return getInvoker(proxy, type, new com.alibaba.dubbo.common.URL(url));
    }
}
