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
package org.apache.dubbo.rpc.proxy.bytebuddy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.AbstractFallbackJdkProxyFactory;
import org.apache.dubbo.rpc.proxy.InvokerInvocationHandler;

/**
 * ByteBuddyRpcProxyFactory
 */
public class ByteBuddyProxyFactory extends AbstractFallbackJdkProxyFactory {

    @Override
    protected <T> Invoker<T> doGetInvoker(T proxy, Class<T> type, URL url) {
        return ByteBuddyProxyInvoker.newInstance(proxy, type, url);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doGetProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        ClassLoader classLoader = invoker.getInterface().getClassLoader();
        return (T) ByteBuddyProxy.newInstance(classLoader, interfaces, new InvokerInvocationHandler(invoker));
    }
}
