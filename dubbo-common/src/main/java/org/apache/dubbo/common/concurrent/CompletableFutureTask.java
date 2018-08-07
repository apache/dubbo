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
package org.apache.dubbo.common.concurrent;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * A {@link FutureTask} that also implements the {@link ListenableFuture} interface.  Unlike {@code FutureTask}, {@code
 * ListenableFutureTask} does not provide an overrideable {@link FutureTask#done() done()} method.  For similar
 * functionality, call {@link #addListener}.
 */
public class CompletableFutureTask<V> {

    /**
     * The execution list to hold our listeners.
     */
    private final ExecutionList executionList = new ExecutionList();

    private CompletableFuture<V> completableFuture;

    CompletableFutureTask(CompletableFuture<V> completableFuture) {
        this.completableFuture = completableFuture;
    }

    /**
     * Creates a {@code ListenableFutureTask} that will upon running, execute the given {@code Callable}.
     * @param supplier
     * @since 10.0
     */
    public static <V> CompletableFutureTask<V> create(Supplier<V> supplier) {
        CompletableFuture<V> completableFuture = CompletableFuture.supplyAsync(supplier);
        return new CompletableFutureTask(completableFuture);
    }

    /**
     * @param supplier
     * @param executor
     * @return
     */
    public CompletableFutureTask create(Supplier<V> supplier, Executor executor) {
        this.completableFuture = CompletableFuture.supplyAsync(supplier, executor);
        return new CompletableFutureTask(completableFuture);
    }

    public V get(long timeout, TimeUnit unit) {
        V v = null;
        try {
            v = completableFuture.get(timeout, unit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return v;
    }

    public void start(Thread connectThread) {
        CompletableFuture.runAsync(connectThread);
    }
}