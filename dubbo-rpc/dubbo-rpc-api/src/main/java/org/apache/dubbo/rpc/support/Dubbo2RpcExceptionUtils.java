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
package org.apache.dubbo.rpc.support;

import java.lang.reflect.Constructor;

public class Dubbo2RpcExceptionUtils {
    private static final Class<? extends org.apache.dubbo.rpc.RpcException> RPC_EXCEPTION_CLASS;
    private static final Constructor<? extends org.apache.dubbo.rpc.RpcException> RPC_EXCEPTION_CONSTRUCTOR_I_S_T;

    static {
        RPC_EXCEPTION_CLASS = loadClass();
        RPC_EXCEPTION_CONSTRUCTOR_I_S_T = loadConstructor(int.class, String.class, Throwable.class);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends org.apache.dubbo.rpc.RpcException> loadClass() {
        try {
            Class<?> clazz = Class.forName("com.alibaba.dubbo.rpc.RpcException");
            if (org.apache.dubbo.rpc.RpcException.class.isAssignableFrom(clazz)) {
                return (Class<? extends org.apache.dubbo.rpc.RpcException>) clazz;
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    private static Constructor<? extends org.apache.dubbo.rpc.RpcException> loadConstructor(
            Class<?>... parameterTypes) {
        if (RPC_EXCEPTION_CLASS == null) {
            return null;
        }
        try {
            return RPC_EXCEPTION_CLASS.getConstructor(parameterTypes);
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isRpcExceptionClassLoaded() {
        return RPC_EXCEPTION_CLASS != null && RPC_EXCEPTION_CONSTRUCTOR_I_S_T != null;
    }

    public static Class<? extends org.apache.dubbo.rpc.RpcException> getRpcExceptionClass() {
        return RPC_EXCEPTION_CLASS;
    }

    public static org.apache.dubbo.rpc.RpcException newRpcException(int code, String message, Throwable cause) {
        if (RPC_EXCEPTION_CONSTRUCTOR_I_S_T == null) {
            return null;
        }
        try {
            return RPC_EXCEPTION_CONSTRUCTOR_I_S_T.newInstance(code, message, cause);
        } catch (Throwable e) {
            return null;
        }
    }
}
