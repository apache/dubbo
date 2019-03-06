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
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class AsyncRpcResult implements Result {
    private static final Logger logger = LoggerFactory.getLogger(AsyncRpcResult.class);

    /**
     * RpcContext can be changed, because thread may have been used by other thread. It should be cloned before store.
     * So we use Invocation instead, Invocation will create for every invoke, but invocation only support attachments of string type.
     */
    private RpcContext storedContext;
    private RpcContext storedServerContext;

    protected CompletableFuture<Result> resultFuture;

    public AsyncRpcResult(CompletableFuture<Result> future) {
        this.resultFuture = future;
        // employ copy of context avoid the other call may modify the context content
        this.storedContext = RpcContext.getContext().copyOf();
        this.storedServerContext = RpcContext.getServerContext().copyOf();
    }

    @Override
    public Object getValue() {
        return getRpcResult().getValue();
    }

    @Override
    public Throwable getException() {
        return getRpcResult().getException();
    }

    @Override
    public boolean hasException() {
        return getRpcResult().hasException();
    }

    public CompletableFuture<Result> getResultFuture() {
        return resultFuture;
    }

    public void setResultFuture(CompletableFuture<Result> resultFuture) {
        this.resultFuture = resultFuture;
    }

    public Result getRpcResult() {
        try {
            if (resultFuture.isDone()) {
                return resultFuture.get();
            }
        } catch (Exception e) {
            // This should never happen;
            logger.error("Got exception when trying to fetch the underlying result from AsyncRpcResult.", e);
        }
        return new RpcResult();
    }

    @Override
    public Object recreate() throws Throwable {
        // FIXME
        return new RpcResult();
    }

    public Result get() throws InterruptedException, ExecutionException {
        return resultFuture.get();
    }

    public AsyncRpcResult thenApplyWithContext(Function<Result, Result> fn) {
        this.resultFuture = resultFuture.thenApply(fn.compose(beforeContext).andThen(afterContext));
        return this;
    }

    public <U> CompletableFuture<U> thenApply(Function<Result,? extends U> fn) {
        return this.resultFuture.thenApply(fn);
    }

    public CompletableFuture<Result> whenComplete
            (BiConsumer<Result, ? super Throwable> action) {
        return this.resultFuture.whenComplete(action);
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

    public static AsyncRpcResult newDefaultAsyncResult(Object value) {
       return newDefaultAsyncResult(value, null);
    }

    public static AsyncRpcResult newDefaultAsyncResult(Throwable t) {
        return newDefaultAsyncResult(null, t);
    }

    public static AsyncRpcResult newDefaultAsyncResult(Object value, Throwable t) {
        CompletableFuture<Result> future = new CompletableFuture<>();
        RpcResult result = new RpcResult();
        if (t != null) {
            result.setException(t);
        } else {
            result.setValue(value);
        }
        future.complete(result);
        return new AsyncRpcResult(future);
    }
}

