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

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

@Deprecated
public interface InvokerListener extends org.apache.dubbo.rpc.InvokerListener {

    void referred(com.alibaba.dubbo.rpc.Invoker<?> invoker) throws com.alibaba.dubbo.rpc.RpcException;

    void destroyed(com.alibaba.dubbo.rpc.Invoker<?> invoker);

    @Override
    default void referred(Invoker<?> invoker) throws RpcException {
        this.referred(new com.alibaba.dubbo.rpc.Invoker.CompatibleInvoker<>(invoker));
    }

    @Override
    default void destroyed(Invoker<?> invoker) {
        this.destroyed(new com.alibaba.dubbo.rpc.Invoker.CompatibleInvoker<>(invoker));
    }
}
