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
import java.util.function.Function;

public class AsyncRpcResult extends AbstractResult {
    private static final Logger logger = LoggerFactory.getLogger(AsyncRpcResult.class);

    /**
     * RpcContext may already have been changed when callback happens, it happens when the same thread is used to execute another RPC call.
     * So we should keep the reference of current RpcContext instance and restore it before callback being executed.
     */
    private RpcContext storedContext;
    private RpcContext storedServerContext;

    private Invocation invocation;

    public AsyncRpcResult(Invocation invocation) {
        this.invocation = invocation;
        this.storedContext = RpcContext.getContext();
        this.storedServerContext = RpcContext.getServerContext();
    }

    public AsyncRpcResult(AsyncRpcResult asyncRpcResult) {
        this.invocation = asyncRpcResult.getInvocation();
        this.storedContext = asyncRpcResult.getStoredContext();
        this.storedServerContext = asyncRpcResult.getStoredServerContext();
    }

    @Override
    public Object getValue() {
        return getRpcResult().getValue();
    }

    @Override
    public void setValue(Object value) {

    }

    @Override
    public Throwable getException() {
        return getRpcResult().getException();
    }

    @Override
    public void setException(Throwable t) {

    }

    @Override
    public boolean hasException() {
        return getRpcResult().hasException();
    }

    public Result getRpcResult() {
        try {
            if (this.isDone()) {
                return this.get();
            }
        } catch (Exception e) {
            // This should never happen;
            logger.error("Got exception when trying to fetch the underlying result from AsyncRpcResult.", e);
        }
        return new RpcResult();
    }

    @Override
    public Object recreate() throws Throwable {
        RpcInvocation rpcInvocation = (RpcInvocation) invocation;
        if (InvokeMode.FUTURE == rpcInvocation.getInvokeMode()) {
            RpcResult rpcResult = new RpcResult();
            CompletableFuture<Object> future = new CompletableFuture<>();
            rpcResult.setValue(future);
            this.whenComplete((result, t) -> {
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
            return rpcResult.recreate();
        } else if (this.isDone()) {
            return this.get().recreate();
        }
        return (new RpcResult()).recreate();
    }

    public AsyncRpcResult thenApplyWithContext(Function<Result, Result> fn) {
        CompletableFuture<Result> future = this.thenApply(fn.compose(beforeContext).andThen(afterContext));
        AsyncRpcResult nextAsyncRpcResult = new AsyncRpcResult(this);
        nextAsyncRpcResult.subscribeTo(future);
        return nextAsyncRpcResult;
    }

    public void subscribeTo(CompletableFuture<?> future) {
        future.whenComplete((obj, t) -> {
            if (t != null) {
                this.completeExceptionally(t);
            } else {
                this.complete((Result) obj);
            }
        });
    }

    @Override
    public Map<String, String> getAttachments() {
        return getRpcResult().getAttachments();
    }

    @Override
    public void setAttachments(Map<String, String> map) {
        getRpcResult().setAttachments(map);
    }

    @Override
    public void addAttachments(Map<String, String> map) {
        getRpcResult().addAttachments(map);
    }

    @Override
    public String getAttachment(String key) {
        return getRpcResult().getAttachment(key);
    }

    @Override
    public String getAttachment(String key, String defaultValue) {
        return getRpcResult().getAttachment(key, defaultValue);
    }

    @Override
    public void setAttachment(String key, String value) {
        getRpcResult().setAttachment(key, value);
    }

    public RpcContext getStoredContext() {
        return storedContext;
    }

    public RpcContext getStoredServerContext() {
        return storedServerContext;
    }

    public Invocation getInvocation() {
        return invocation;
    }

    public void setInvocation(Invocation invocation) {
        this.invocation = invocation;
    }

    /**
     * tmp context to use when the thread switch to Dubbo thread.
     */
    private RpcContext tmpContext;
    private RpcContext tmpServerContext;

    private Function<Result, Result> beforeContext = (result) -> {
        tmpContext = RpcContext.getContext();
        tmpServerContext = RpcContext.getServerContext();
        RpcContext.restoreContext(storedContext);
        RpcContext.restoreServerContext(storedServerContext);
        return result;
    };

    private Function<Result, Result> afterContext = (result) -> {
        RpcContext.restoreContext(tmpContext);
        RpcContext.restoreServerContext(tmpServerContext);
        return result;
    };

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
        AsyncRpcResult asyncRpcResult = new AsyncRpcResult(invocation);
        RpcResult result = new RpcResult();
        if (t != null) {
            result.setException(t);
        } else {
            result.setValue(value);
        }
        asyncRpcResult.complete(result);
        return asyncRpcResult;
    }
}

