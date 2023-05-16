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
import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;
import org.apache.dubbo.rpc.proxy.MethodInvoker;

class ByteBuddyProxyInvoker<T> extends AbstractProxyInvoker<T> {

    private final MethodInvoker methodInvoker;

    private ByteBuddyProxyInvoker(T proxy,
                                  Class<T> type,
                                  URL url,
                                  MethodInvoker methodInvoker) {
        super(proxy, type, url);
        this.methodInvoker = methodInvoker;
    }

    @Override
    protected Object doInvoke(T instance, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable {
        if ("getClass".equals(methodName)) {
            return instance.getClass();
        }
        if ("hashCode".equals(methodName)) {
            return instance.hashCode();
        }
        if ("toString".equals(methodName)) {
            return instance.toString();
        }
        if ("equals".equals(methodName)) {
            if (arguments.length == 1) {
                return instance.equals(arguments[0]);
            }
            throw new IllegalArgumentException("Invoke method [" + methodName + "] argument number error.");
        }
        return methodInvoker.invoke(instance, methodName, parameterTypes, arguments);
    }

    static <T> ByteBuddyProxyInvoker<T> newInstance(T proxy, Class<T> type, URL url) {
        return new ByteBuddyProxyInvoker<>(proxy, type, url, MethodInvoker.newInstance(proxy.getClass()));
    }
}
