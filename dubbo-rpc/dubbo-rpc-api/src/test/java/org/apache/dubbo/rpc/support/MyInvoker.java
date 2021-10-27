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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.concurrent.CompletableFuture;

/**
 * MockInvoker.java
 */
public class MyInvoker<T> implements Invoker<T> {

    URL url;
    Class<T> type;
    boolean hasException = false;
    boolean destroyed = false;

    public MyInvoker(URL url) {
        this.url = url;
        type = (Class<T>) DemoService.class;
    }

    public MyInvoker(URL url, boolean hasException) {
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

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        AppResponse result = new AppResponse();
        if (!hasException) {
            result.setValue("alibaba");
        } else {
            result.setException(new RuntimeException("mocked exception"));
        }

        return new AsyncRpcResult(CompletableFuture.completedFuture(result), invocation);
    }

    @Override
    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public String toString() {
        return "MyInvoker.toString()";
    }

}
