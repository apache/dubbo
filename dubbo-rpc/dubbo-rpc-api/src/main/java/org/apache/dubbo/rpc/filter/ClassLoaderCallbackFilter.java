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

import static org.apache.dubbo.common.constants.CommonConstants.WORKING_CLASSLOADER_KEY;

/**
 * Switch thread context class loader on filter callback.
 */
@Activate(group = CommonConstants.PROVIDER, order = Integer.MAX_VALUE)
public class ClassLoaderCallbackFilter implements Filter, BaseFilter.Listener {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        setClassLoader(invoker, invocation);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        setClassLoader(invoker, invocation);
    }

    private void setClassLoader(Invoker<?> invoker, Invocation invocation) {
        ClassLoader workingClassLoader = (ClassLoader) invocation.get(WORKING_CLASSLOADER_KEY);
        if (workingClassLoader != null) {
            Thread.currentThread().setContextClassLoader(workingClassLoader);
        }
    }
}
