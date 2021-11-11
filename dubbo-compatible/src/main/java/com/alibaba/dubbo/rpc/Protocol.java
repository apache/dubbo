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

import org.apache.dubbo.rpc.ProtocolServer;

import com.alibaba.dubbo.common.URL;

import java.util.Collections;
import java.util.List;

@Deprecated
public interface Protocol extends org.apache.dubbo.rpc.Protocol {

    <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;

    <T> Invoker<T> refer(Class<T> aClass, URL url) throws RpcException;

    @Override
    default <T> org.apache.dubbo.rpc.Exporter<T> export(org.apache.dubbo.rpc.Invoker<T> invoker) throws RpcException {
        return this.export(new Invoker.CompatibleInvoker<>(invoker));
    }

    @Override
    default <T> org.apache.dubbo.rpc.Invoker<T> refer(Class<T> aClass, org.apache.dubbo.common.URL url) throws RpcException {
        return this.refer(aClass, new URL(url));
    }

    @Override
    default List<ProtocolServer> getServers() {
        return Collections.emptyList();
    }
}
