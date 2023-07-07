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
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import static org.apache.dubbo.common.constants.CommonConstants.STAGED_CLASSLOADER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.WORKING_CLASSLOADER_KEY;

/**
 * Set the current execution thread class loader to service interface's class loader.
 */
@Activate(group = CommonConstants.PROVIDER, order = -30000)
public class ClassLoaderFilter implements Filter, BaseFilter.Listener {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        ClassLoader stagedClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader effectiveClassLoader;
        if (invocation.getServiceModel() != null) {
            effectiveClassLoader = invocation.getServiceModel().getClassLoader();
        } else {
            effectiveClassLoader = invoker.getClass().getClassLoader();
        }

        if (effectiveClassLoader != null) {
            invocation.put(STAGED_CLASSLOADER_KEY, stagedClassLoader);
            invocation.put(WORKING_CLASSLOADER_KEY, effectiveClassLoader);

            Thread.currentThread().setContextClassLoader(effectiveClassLoader);
        }
        try {
            return invoker.invoke(invocation);
        } finally {
            Thread.currentThread().setContextClassLoader(stagedClassLoader);
        }
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        resetClassLoader(invoker, invocation);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        resetClassLoader(invoker, invocation);
    }

    private void resetClassLoader(Invoker<?> invoker, Invocation invocation) {
        ClassLoader stagedClassLoader = (ClassLoader) invocation.get(STAGED_CLASSLOADER_KEY);
        if (stagedClassLoader != null) {
            Thread.currentThread().setContextClassLoader(stagedClassLoader);
        }
    }
}
