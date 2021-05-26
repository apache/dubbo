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

package com.alibaba.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Result;

import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ResponseCallback;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * 2019-06-20
 */
@Deprecated
public class FutureAdapter<V> implements Future<V> {

    private CompletableFuture<Object> future;

    public FutureAdapter(CompletableFuture<Object> future) {
        this.future = future;
    }

    public FutureAdapter(ResponseFuture responseFuture) {
        this.future = new CompletableFuture<>();
        responseFuture.setCallback(new ResponseCallback() {
            @Override
            public void done(Object response) {
                future.complete(response);
            }

            @Override
            public void caught(Throwable exception) {
                future.completeExceptionally(exception);
            }
        });
    }

    public ResponseFuture getFuture() {
        return new ResponseFuture() {
            @Override
            public Object get() throws RemotingException {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RemotingException(e);
                }
            }

            @Override
            public Object get(int timeoutInMillis) throws RemotingException {
                try {
                    return future.get(timeoutInMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | TimeoutException | ExecutionException e) {
                    throw new RemotingException(e);
                }
            }

            @Override
            public void setCallback(ResponseCallback callback) {
                FutureAdapter.this.setCallback(callback);
            }

            @Override
            public boolean isDone() {
                return future.isDone();
            }
        };
    }

    void setCallback(ResponseCallback callback) {
        BiConsumer<Object, ? super Throwable> biConsumer = new BiConsumer<Object, Throwable>() {

            @Override
            public void accept(Object obj, Throwable t) {
                if (t != null) {
                    if (t instanceof CompletionException) {
                        t = t.getCause();
                    }
                    callback.caught(t);
                } else {
                    AppResponse appResponse = (AppResponse)obj;
                    if (appResponse.hasException()) {
                        callback.caught(appResponse.getException());
                    } else {
                        callback.done((V) appResponse.getValue());
                    }
                }
            }
        };
        future.whenComplete(biConsumer);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get() throws InterruptedException, ExecutionException {
        try {
            return (V) (((Result) future.get()).recreate());
        } catch (InterruptedException | ExecutionException e)  {
            throw e;
        } catch (Throwable e) {
            throw new RpcException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return (V) (((Result) future.get(timeout, unit)).recreate());
        } catch (InterruptedException | ExecutionException | TimeoutException e)  {
            throw e;
        } catch (Throwable e) {
            throw new RpcException(e);
        }
    }
}
