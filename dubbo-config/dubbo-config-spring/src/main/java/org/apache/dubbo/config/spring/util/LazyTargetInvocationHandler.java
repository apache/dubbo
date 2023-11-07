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
package org.apache.dubbo.config.spring.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LazyTargetInvocationHandler implements InvocationHandler {
    private final LazyTargetSource lazyTargetSource;
    private volatile Object target;

    public LazyTargetInvocationHandler(LazyTargetSource lazyTargetSource) {
        this.lazyTargetSource = lazyTargetSource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("toString".equals(methodName)) {
                if (target != null) {
                    return target.toString();
                } else {
                    return this.toString();
                }
            } else if ("hashCode".equals(methodName)) {
                return this.hashCode();
            }
        } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
            return this.equals(args[0]);
        }

        if (target == null) {
            target = lazyTargetSource.getTarget();
        }
        if (method.getDeclaringClass().isInstance(target)) {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException exception) {
                Throwable targetException = exception.getTargetException();
                if (targetException != null) {
                    throw targetException;
                }
            }
        }
        throw new IllegalStateException("The proxied interface [" + method.getDeclaringClass() + "] contains a method ["
                + method + "] that is not implemented by the proxy class [" + target.getClass() + "]");
    }
}
