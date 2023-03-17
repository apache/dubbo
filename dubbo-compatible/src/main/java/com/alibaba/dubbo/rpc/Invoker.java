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

import com.alibaba.dubbo.common.URL;

@Deprecated
public interface Invoker<T> extends org.apache.dubbo.rpc.Invoker<T> {

    Result invoke(Invocation invocation) throws RpcException;

    @Override
    URL getUrl();

    default org.apache.dubbo.rpc.Invoker<T> getOriginal() {
        return null;
    }

    // This method will never be called for a legacy invoker.
    @Override
    default org.apache.dubbo.rpc.Result invoke(org.apache.dubbo.rpc.Invocation invocation) throws org.apache.dubbo.rpc.RpcException {
        return null;
    }

    class CompatibleInvoker<T> implements Invoker<T> {

        private org.apache.dubbo.rpc.Invoker<T> invoker;

        public CompatibleInvoker(org.apache.dubbo.rpc.Invoker<T> invoker) {
            this.invoker = invoker;
        }

        @Override
        public Class<T> getInterface() {
            return invoker.getInterface();
        }

        @Override
        public org.apache.dubbo.rpc.Result invoke(org.apache.dubbo.rpc.Invocation invocation) throws org.apache.dubbo.rpc.RpcException {
            return new Result.CompatibleResult(invoker.invoke(invocation));
        }
        
        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return new Result.CompatibleResult(invoker.invoke(invocation.getOriginal()));
        }

        @Override
        public URL getUrl() {
            return new URL(invoker.getUrl());
        }

        @Override
        public boolean isAvailable() {
            return invoker.isAvailable();
        }

        @Override
        public void destroy() {
            invoker.destroy();
        }

        @Override
        public org.apache.dubbo.rpc.Invoker<T> getOriginal() {
            return invoker;
        }
    }
}
