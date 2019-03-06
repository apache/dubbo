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
package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class AsyncToSyncInvoker<T> implements Invoker<T> {

    private Invoker<T> invoker;

    AsyncToSyncInvoker(Invoker<T> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        AsyncRpcResult asyncResult = (AsyncRpcResult)invoker.invoke(invocation);
        if (RpcUtils.isReturnTypeFuture(invocation)) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            Result rpcResult = new RpcResult(future);
            asyncResult.whenComplete((result, t) -> {
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
            return rpcResult;
        } else if (RpcUtils.isAsync(invoker.getUrl(), invocation)) {
            return new RpcResult();
        } else {
            try {
                return asyncResult.get();
            } catch (InterruptedException e) {
                throw new RpcException("Interrupted unexpectedly while waiting for remoting result to return!  method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                if (t instanceof TimeoutException) {
                    throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
                } else if (t instanceof RemotingException) {
                    throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
                }
            }
        }
        return new RpcResult();
    }

    @Override
    public URL getUrl() {
        return invoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return invoker.isAvailable();
    }

    @Override
    public void destroy() {
        invoker.destroy();
    }
}
