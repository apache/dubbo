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

package com.alibaba.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public interface Router extends org.apache.dubbo.rpc.cluster.Router {

    com.alibaba.dubbo.common.URL getUrl();

    <T> List<com.alibaba.dubbo.rpc.Invoker<T>> route(List<com.alibaba.dubbo.rpc.Invoker<T>> invokers,
                                                     com.alibaba.dubbo.common.URL url,
                                                     com.alibaba.dubbo.rpc.Invocation invocation)
            throws com.alibaba.dubbo.rpc.RpcException;

    @Override
    default <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        List<com.alibaba.dubbo.rpc.Invoker<T>> invs = invokers.stream().map(invoker ->
                new com.alibaba.dubbo.rpc.Invoker.CompatibleInvoker<T>(invoker)).
                collect(Collectors.toList());

        List<com.alibaba.dubbo.rpc.Invoker<T>> res = this.route(invs, new com.alibaba.dubbo.common.URL(url),
                new com.alibaba.dubbo.rpc.Invocation.CompatibleInvocation(invocation));

        return res.stream().map(inv -> inv.getOriginal()).collect(Collectors.toList());
    }
}
