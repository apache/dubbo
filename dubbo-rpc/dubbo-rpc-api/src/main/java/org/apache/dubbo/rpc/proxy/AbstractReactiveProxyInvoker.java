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
package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.common.Constants.KEY_PUBLISHER_TYPE;
import static org.apache.dubbo.common.Constants.VALUE_PUBLISHER_MONO;
import static org.apache.dubbo.common.Constants.VALUE_PUBLISHER_FLUX;


/**
 * Abstraction of reactive proxy invoker which will delegate consumer in provider side.
 * @author cherry
 */
public abstract class AbstractReactiveProxyInvoker<T> extends AbstractProxyInvoker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractReactiveProxyInvoker.class);
    protected final T proxy;
    private final Class<T> type;
    private final URL url;

    public AbstractReactiveProxyInvoker(T proxy, Class<T> type, URL url) {
        super(proxy, type, url);
        this.proxy = proxy;
        this.type = type;
        this.url = url;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        String publisher = invocation.getAttachment(KEY_PUBLISHER_TYPE);
        if(StringUtils.isBlank(publisher)) {
            return super.invoke(invocation);
        }
        RpcContext rpcContext = RpcContext.getContext();
        try {
            Object obj = doInvoke(proxy, invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments());
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
        } catch (InvocationTargetException e) {
            // TODO async throw exception before async thread write back, should stop asyncContext
            if (rpcContext.isAsyncStarted() && !rpcContext.stopAsync()) {
                LOGGER.error("Provider async started, but got an exception from the original method, cannot write the exception back to consumer because an async result may have returned the new thread.", e);
            }
            return new RpcResult(e.getTargetException());
        } catch (Throwable e) {
            throw new RpcException("Failed to invoke remote proxy method " + invocation.getMethodName() + " to " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }
}