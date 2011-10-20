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
package com.alibaba.dubbo.rpc.cluster.router;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

public class MockInvoker<T> implements Invoker<T> {
    private boolean available = false;
    private URL url ;

    public MockInvoker() {
    }
    public MockInvoker(URL url) {
        super();
        this.url = url;
    }
    public MockInvoker(boolean available) {
        this.available = available;
    }

    public Class<T> getInterface() {
        return null;
    }

    public URL getUrl() {
        return url;
    }

    public boolean isAvailable() {
        return available;
    }

    public Result invoke(Invocation invocation) throws RpcException {
        return null;
    }

    public void destroy() {
    }
}