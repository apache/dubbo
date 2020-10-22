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

package com.alibaba.dubbo.rpc.support;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;

import java.lang.reflect.Type;

/**
 * 2019-04-18
 */
public class RpcUtils extends org.apache.dubbo.rpc.support.RpcUtils {


    public static Class<?> getReturnType(Invocation invocation) {
        return org.apache.dubbo.rpc.support.RpcUtils.getReturnType(invocation);
    }

    // TODO why not get return type when initialize Invocation?
    public static Type[] getReturnTypes(Invocation invocation) {
        return org.apache.dubbo.rpc.support.RpcUtils.getReturnTypes(invocation);
    }

    public static Long getInvocationId(Invocation inv) {
        return org.apache.dubbo.rpc.support.RpcUtils.getInvocationId(inv);
    }

    /**
     * Idempotent operation: invocation id will be added in async operation by default
     *
     * @param url
     * @param inv
     */
    public static void attachInvocationIdIfAsync(URL url, Invocation inv) {
        org.apache.dubbo.rpc.support.RpcUtils.attachInvocationIdIfAsync(url.getOriginalURL(), inv);
    }


    public static String getMethodName(Invocation invocation) {
        return org.apache.dubbo.rpc.support.RpcUtils.getMethodName(invocation);
    }

    public static Object[] getArguments(Invocation invocation) {
        return org.apache.dubbo.rpc.support.RpcUtils.getArguments(invocation);
    }

    public static Class<?>[] getParameterTypes(Invocation invocation) {
        return org.apache.dubbo.rpc.support.RpcUtils.getParameterTypes(invocation);
    }

    public static boolean isAsync(URL url, Invocation inv) {
        return org.apache.dubbo.rpc.support.RpcUtils.isAsync(url.getOriginalURL(), inv);
    }

    public static boolean isReturnTypeFuture(Invocation inv) {
        return org.apache.dubbo.rpc.support.RpcUtils.isReturnTypeFuture(inv);
    }

    public static boolean isOneway(URL url, Invocation inv) {
        return org.apache.dubbo.rpc.support.RpcUtils.isOneway(url.getOriginalURL(), inv);
    }
}
