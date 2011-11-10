/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.proxy.wrapper;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * MockInvoker
 * 
 * @author william.liangf
 */
public class MockProxyInvoker<T> implements Invoker<T> {
    
    private final Invoker<T> invoker;
    
    private final Invoker<T> mockInvoker;

    public MockProxyInvoker(Invoker<T> invoker, Invoker<T> mockInvoker) {
        this.invoker = invoker;
        this.mockInvoker = mockInvoker;
    }

    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    public URL getUrl() {
        return invoker.getUrl();
    }

    public boolean isAvailable() {
        return invoker.isAvailable() && mockInvoker.isAvailable();
    }

    public Result invoke(Invocation invocation) throws RpcException {
        try {
            return invoker.invoke(invocation);
        } catch (RpcException e) {
            return mockInvoker.invoke(invocation);
        }
    }

    public void destroy() {
        try {
            invoker.destroy();
        } finally {
            mockInvoker.destroy();
        }
    }

}