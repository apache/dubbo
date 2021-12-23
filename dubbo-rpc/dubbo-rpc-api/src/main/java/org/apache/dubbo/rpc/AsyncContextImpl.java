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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncContextImpl implements AsyncContext {

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private CompletableFuture<Object> future;

    private final RpcContext.RestoreContext restoreContext;
    private final ClassLoader restoreClassLoader;
    private ClassLoader stagedClassLoader;

    public AsyncContextImpl() {
        restoreContext = RpcContext.storeContext();
        restoreClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void write(Object value) {
        if (isAsyncStarted() && stop()) {
            if (value instanceof Throwable) {
                Throwable bizExe = (Throwable) value;
                future.completeExceptionally(bizExe);
            } else {
                future.complete(value);
            }
        } else {
            throw new IllegalStateException("The async response has probably been wrote back by another thread, or the asyncContext has been closed.");
        }
    }

    @Override
    public boolean isAsyncStarted() {
        return started.get();
    }

    @Override
    public boolean stop() {
        return stopped.compareAndSet(false, true);
    }

    @Override
    public void start() {
        if (this.started.compareAndSet(false, true)) {
            this.future = new CompletableFuture<>();
        }
    }

    @Override
    public void signalContextSwitch() {
        RpcContext.restoreContext(restoreContext);
        if (restoreClassLoader != null) {
            stagedClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(restoreClassLoader);
        }
    }

    @Override
    public void resetContext() {
        RpcContext.removeContext();
        if (stagedClassLoader != null) {
            Thread.currentThread().setContextClassLoader(restoreClassLoader);
        }
    }

    public CompletableFuture<Object> getInternalFuture() {
        return future;
    }
}
