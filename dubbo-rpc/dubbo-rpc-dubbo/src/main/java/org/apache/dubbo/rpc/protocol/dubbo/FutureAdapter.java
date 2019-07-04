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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ResponseCallback;
import org.apache.dubbo.remoting.exchange.ResponseFuture;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.RpcException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * FutureAdapter
 */
public class FutureAdapter<V> extends CompletableFuture<V> {

    private final ResponseFuture deprecatedResponseFuture;
    private CompletableFuture<AppResponse> appResponseFuture;

    public FutureAdapter(CompletableFuture<AppResponse> future) {
        this.deprecatedResponseFuture = new DeprecatedRespnoseFuture();
        this.appResponseFuture = future;
        future.whenComplete((appResponse, t) -> {
            if (t != null) {
                if (t instanceof CompletionException) {
                    t = t.getCause();
                }
                this.completeExceptionally(t);
            } else {
                if (appResponse.hasException()) {
                    this.completeExceptionally(appResponse.getException());
                } else {
                    this.complete((V) appResponse.getValue());
                }
            }
        });
    }

    // TODO figure out the meaning of cancel in DefaultFuture.
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return appResponseFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return appResponseFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return super.isDone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } catch (ExecutionException | InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RpcException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RpcException(e);
        }
    }

    public CompletableFuture<AppResponse> getAppResponseFuture() {
        return appResponseFuture;
    }

    @Deprecated
    public ResponseFuture getFuture() {
        return deprecatedResponseFuture;
    }

    private class DeprecatedRespnoseFuture implements ResponseFuture {

        @Override
        public Object get() throws RemotingException {
            try {
                return FutureAdapter.this.get();
            } catch (Exception e) {
                throw new RemotingException(null, e);
            }
        }

        @Override
        public Object get(int timeoutInMillis) throws RemotingException {
            try {
                return FutureAdapter.this.get(timeoutInMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new RemotingException(null, e);
            }
        }

        @Override
        public void setCallback(ResponseCallback callback) {
            FutureAdapter.this.whenComplete((v, t) -> {

            });
        }

        @Override
        public boolean isDone() {
            return FutureAdapter.this.isDone();
        }
    }

}