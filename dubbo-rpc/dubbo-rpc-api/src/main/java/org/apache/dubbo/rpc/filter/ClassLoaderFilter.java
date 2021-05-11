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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvocationWrapper;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * Set the current execution thread class loader to service interface's class loader.
 */
@Activate(group = CommonConstants.PROVIDER, order = -30000)
public class ClassLoaderFilter implements Filter, BaseFilter.Request {

    private final static String CLASS_LOADER_FILTER_CACHE = "ClassLoaderFilterCache";

    @Override
    public Result onBefore(Invoker<?> invoker, InvocationWrapper invocationWrapper) throws RpcException {
        Invocation invocation = invocationWrapper.getInvocation();
        ClassLoader ocl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(invoker.getInterface().getClassLoader());
        invocation.getAttributes().put(CLASS_LOADER_FILTER_CACHE, ocl);

        return null;
    }

    @Override
    public void onFinish(Invoker<?> invoker, InvocationWrapper invocationWrapper) throws RpcException {
        Invocation invocation = invocationWrapper.getInvocation();
        Object ocl = invocation.getAttributes().get(CLASS_LOADER_FILTER_CACHE);
        if(ocl instanceof ClassLoader) {
            Thread.currentThread().setContextClassLoader((ClassLoader) ocl);
        }

    }
}
