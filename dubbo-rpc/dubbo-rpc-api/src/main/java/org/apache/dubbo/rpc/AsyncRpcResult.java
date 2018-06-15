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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;


public class AsyncRpcResult extends AbstractResult {
    private static final Logger logger = LoggerFactory.getLogger(AsyncRpcResult.class);

    private Map<String, String> attachments = new HashMap<String, String>();

    protected CompletableFuture<Object> valueFuture;

    protected CompletableFuture<Result> resultFuture;

    public AsyncRpcResult(CompletableFuture<Object> future) {
        this(future, true);
    }

    public AsyncRpcResult(CompletableFuture<Object> future, boolean registerCallback) {
        if (registerCallback) {
            resultFuture = new CompletableFuture<>();
            /**
             * We do not know whether future already completed or not, it's a future exposed or even created by end user.
             * 1. future complete before whenComplete. whenComplete fn (resultFuture.complete) will be executed in thread subscribing, in our case, is Dubbo thread.
             * 2. future complete after whenComplete. whenComplete fn (resultFuture.complete) will be executed in thread calling complete, normally is User thread.
             */
            future.whenComplete((v, t) -> {
                RpcResult rpcResult;
                if (t != null) {
                    if (t instanceof CompletionException) {
                        rpcResult = new RpcResult(t.getCause());
                    } else {
                        rpcResult = new RpcResult(t);
                    }
                } else {
                    rpcResult = new RpcResult(v);
                }
                resultFuture.complete(rpcResult);
            });
        }
        this.valueFuture = future;
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

    @Override
    public Object getResult() {
        return getRpcResult().getResult();
    }

    public CompletableFuture getValueFuture() {
        return valueFuture;
    }

    public CompletableFuture<Result> getResultFuture() {
        return resultFuture;
    }

    public void setResultFuture(CompletableFuture<Result> resultFuture) {
        this.resultFuture = resultFuture;
    }

    public Result getRpcResult() {
        Result result;
        try {
            result = resultFuture.get();
        } catch (Exception e) {
            // This should never happen;
            logger.error("", e);
            result = new RpcResult();
        }
        return result;
    }

    @Override
    public Object recreate() throws Throwable {
        return valueFuture;
    }
}

