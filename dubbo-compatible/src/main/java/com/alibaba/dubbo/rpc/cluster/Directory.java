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

import com.alibaba.dubbo.common.URL;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public interface Directory<T> extends org.apache.dubbo.rpc.cluster.Directory<T> {

    URL getUrl();

    List<com.alibaba.dubbo.rpc.Invoker<T>> list(com.alibaba.dubbo.rpc.Invocation invocation) throws com.alibaba.dubbo.rpc.RpcException;

    @Override
    default List<Invoker<T>> list(Invocation invocation) throws RpcException {
        List<com.alibaba.dubbo.rpc.Invoker<T>> res = this.list(new com.alibaba.dubbo.rpc.Invocation.CompatibleInvocation(invocation));
        return res.stream().map(i -> i.getOriginal()).collect(Collectors.toList());
    }
}
