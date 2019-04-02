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
import org.apache.dubbo.rpc.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.common.Constants.KEY_PUBLISHER_TYPE;
import static org.apache.dubbo.common.Constants.VALUE_PUBLISHER_MONO;
import static org.apache.dubbo.common.Constants.VALUE_PUBLISHER_FLUX;

/**
 * MockInvoker.java
 */
public class MyReactiveInvoker<T> implements Invoker<T> {

    URL url;
    Class<T> type;
    boolean hasException = false;
    ReactiveDemoService proxy;

    public MyReactiveInvoker(URL url) {
        this(url,false);
    }

    public MyReactiveInvoker(URL url, boolean hasException) {
        this.url = url;
        type = (Class<T>) ReactiveDemoService.class;
        this.hasException = hasException;
        proxy = new ReactiveDemoServiceImpl();
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
        String publisher = invocation.getAttachment(KEY_PUBLISHER_TYPE);
        try {
            Method method = ReactiveDemoService.class.getMethod(invocation.getMethodName(),invocation.getParameterTypes());
            Object obj = method.invoke(proxy,invocation.getArguments());

            Mono mono = null;
            if(publisher.equals(VALUE_PUBLISHER_MONO)) {
                mono = (Mono) obj;
            } else if(publisher.equals(VALUE_PUBLISHER_FLUX)) {
                Flux<Object> flux = (Flux<Object>) obj;
                mono = flux.collect(ArrayList::new,ArrayList::add);
            }
            if(mono==null) {
                CompletableFuture future = new CompletableFuture();
                Exception ex = new IllegalArgumentException("unexpected publisher type:"+publisher);
                future.completeExceptionally(ex);
                return new AsyncRpcResult(future);
            } else {
                return new AsyncRpcResult(mono.toFuture());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new RpcResult(e);
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public String toString() {
        return "MyInvoker.toString()";
    }

}
