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
package com.alibaba.dubbo.rpc;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;

import javax.annotation.Resource;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Deprecated
public class RpcResult extends AppResponse implements com.alibaba.dubbo.rpc.Result {

    /**
     * Whether set future to Thread Local when invocation mode is sync
     */
    private static final boolean setFutureWhenSync = Boolean.parseBoolean(System.getProperty(CommonConstants.SET_FUTURE_IN_SYNC_MODE, "true"));

    @Resource
    private CompletableFuture<AppResponse> responseFuture;

    public RpcResult() {
        responseFuture = CompletableFuture.completedFuture(this);
    }

    public RpcResult(Object result) {
        super(result);
        responseFuture = CompletableFuture.completedFuture(this);
    }

    public RpcResult(Throwable exception) {
        super(exception);
        responseFuture = CompletableFuture.completedFuture(this);
    }

    @Override
    public org.apache.dubbo.rpc.Result whenCompleteWithContext(BiConsumer<org.apache.dubbo.rpc.Result, Throwable> fn) {
        this.responseFuture = this.responseFuture.whenComplete((v, t) -> {
            fn.accept(v, t);
        });

        if (setFutureWhenSync) {
            RpcContext.getServiceContext().setFuture(new FutureAdapter<>(this.responseFuture));
        }

        return this;
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<org.apache.dubbo.rpc.Result, ? extends U> fn) {
        return responseFuture.thenApply(fn);
    }

    @Override
    public org.apache.dubbo.rpc.Result get() throws InterruptedException, ExecutionException {
        return this;
    }

    @Override
    public org.apache.dubbo.rpc.Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this;
    }

}
