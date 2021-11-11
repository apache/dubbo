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
package org.apache.dubbo.filter;


import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.service.DemoService;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

public class LegacyInvoker<T> implements Invoker<T> {

    URL url;
    Class<T> type;
    boolean hasException = false;

    public LegacyInvoker(URL url) {
        this.url = url;
        type = (Class<T>) DemoService.class;
    }

    public LegacyInvoker(URL url, boolean hasException) {
        this.url = url;
        type = (Class<T>) DemoService.class;
        this.hasException = hasException;
    }

    @Override
    public Class<T> getInterface() {
        return type;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    public Result invoke(Invocation invocation) throws RpcException {
        AppResponse result = new AppResponse();
        if (!hasException) {
            result.setValue("alibaba");
        } else {
            result.setException(new RuntimeException("mocked exception"));
        }
        return new Result.CompatibleResult(result);
    }

    @Override
    public void destroy() {
    }

}
