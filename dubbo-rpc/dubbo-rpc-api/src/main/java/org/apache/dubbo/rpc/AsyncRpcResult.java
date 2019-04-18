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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class AsyncRpcResult implements Result {
    private static final Logger logger = LoggerFactory.getLogger(AsyncRpcResult.class);

    /**
     * RpcContext may already have been changed when callback happens, it happens when the same thread is used to execute another RPC call.
     * So we should keep the reference of current RpcContext instance and restore it before callback being executed.
     */
    private RpcContext storedContext;
    private RpcContext storedServerContext;

    private CompletableFuture<AppResponse> responseFuture;
    private Invocation invocation;

    public AsyncRpcResult(CompletableFuture<AppResponse> future, Invocation invocation) {
        this.responseFuture = future;
        this.invocation = invocation;
        this.storedContext = RpcContext.getContext();
        this.storedServerContext = RpcContext.getServerContext();
    }

    @Override
    public Object getValue() {
        return getAppResponse().getValue();
    }

    @Override
    public void setValue(Object value) {

    }

    @Override
    public Throwable getException() {
        return getAppResponse().getException();
    }

    @Override
    public void setException(Throwable t) {

    }

    @Override
    public boolean hasException() {
        return getAppResponse().hasException();
    }

    public CompletableFuture<AppResponse> getResponseFuture() {
        return responseFuture;
    }

    public void setResponseFuture(CompletableFuture<AppResponse> responseFuture) {
        this.responseFuture = responseFuture;
    }

    public Result getAppResponse() {
        try {
            if (responseFuture.isDone()) {
                return responseFuture.get();
            }
        } catch (Exception e) {
            // This should never happen;
            logger.error("Got exception when trying to fetch the underlying result from AsyncRpcResult.", e);
        }
        return new AppResponse();
    }

    @Override
    public Object recreate() throws Throwable {
        RpcInvocation rpcInvocation = (RpcInvocation) invocation;
        if (InvokeMode.FUTURE == rpcInvocation.getInvokeMode()) {
            AppResponse appResponse = new AppResponse();
            CompletableFuture<Object> future = new CompletableFuture<>();
            appResponse.setValue(future);
            responseFuture.whenComplete((result, t) -> {
                if (t != null) {
                    if (t instanceof CompletionException) {
                        t = t.getCause();
                    }
                    future.completeExceptionally(t);
                } else {
                    if (result.hasException()) {
                        future.completeExceptionally(result.getException());
                    } else {
                        future.complete(result.getValue());
                    }
                }
            });
            return appResponse.recreate();
        } else if (responseFuture.isDone()) {
            return responseFuture.get().recreate();
        }
        return (new AppResponse()).recreate();
    }

    public Result get() throws InterruptedException, ExecutionException {
        return responseFuture.get();
    }

    @Override
    public Result thenApplyWithContext(Function<AppResponse, AppResponse> fn) {
        this.responseFuture = responseFuture.thenApply(fn.compose(beforeContext).andThen(afterContext));
        return this;
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<Result,? extends U> fn) {
        return this.responseFuture.thenApply(fn);
    }

    @Override
    public Map<String, String> getAttachments() {
        return getAppResponse().getAttachments();
    }

    @Override
    public void setAttachments(Map<String, String> map) {
        getAppResponse().setAttachments(map);
    }

    @Override
    public void addAttachments(Map<String, String> map) {
        getAppResponse().addAttachments(map);
    }

    @Override
    public String getAttachment(String key) {
        return getAppResponse().getAttachment(key);
    }

    @Override
    public String getAttachment(String key, String defaultValue) {
        return getAppResponse().getAttachment(key, defaultValue);
    }

    @Override
    public void setAttachment(String key, String value) {
        getAppResponse().setAttachment(key, value);
    }

    /**
     * tmp context to use when the thread switch to Dubbo thread.
     */
    private RpcContext tmpContext;
    private RpcContext tmpServerContext;

    private Function<AppResponse, AppResponse> beforeContext = (appResponse) -> {
        tmpContext = RpcContext.getContext();
        tmpServerContext = RpcContext.getServerContext();
        RpcContext.restoreContext(storedContext);
        RpcContext.restoreServerContext(storedServerContext);
        return appResponse;
    };

    private Function<AppResponse, AppResponse> afterContext = (appResponse) -> {
        RpcContext.restoreContext(tmpContext);
        RpcContext.restoreServerContext(tmpServerContext);
        return appResponse;
    };

    /**
     * Some utility methods used to quickly generate default AsyncRpcResult instance.
     */
    public static AsyncRpcResult newDefaultAsyncResult(AppResponse result, Invocation invocation) {
        return new AsyncRpcResult(CompletableFuture.completedFuture(result), invocation);
    }

    public static AsyncRpcResult newDefaultAsyncResult(Invocation invocation) {
        return newDefaultAsyncResult(null, null, invocation);
    }

    public static AsyncRpcResult newDefaultAsyncResult(Object value, Invocation invocation) {
        return newDefaultAsyncResult(value, null, invocation);
    }

    public static AsyncRpcResult newDefaultAsyncResult(Throwable t, Invocation invocation) {
        return newDefaultAsyncResult(null, t, invocation);
    }

    public static AsyncRpcResult newDefaultAsyncResult(Object value, Throwable t, Invocation invocation) {
        CompletableFuture<AppResponse> future = new CompletableFuture<>();
        AppResponse result = new AppResponse();
        if (t != null) {
            result.setException(t);
        } else {
            result.setValue(value);
        }
        future.complete(result);
        return new AsyncRpcResult(future, invocation);
    }
}

