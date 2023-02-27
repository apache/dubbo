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

package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.stream.StreamObserver;

import java.util.concurrent.CompletableFuture;

public class FutureToObserverAdaptor<T> implements StreamObserver<T> {

    private final CompletableFuture<T> future;

    public FutureToObserverAdaptor(CompletableFuture<T> future) {
        this.future = future;
    }

    @Override
    public void onNext(T data) {
        if (future.isDone() || future.isCancelled() || future.isCompletedExceptionally()) {
            throw new IllegalStateException("Too many response for unary method");
        }
        future.complete(data);
    }

    @Override
    public void onError(Throwable throwable) {
        if (future.isDone() || future.isCancelled() || future.isCompletedExceptionally()) {
            throw new IllegalStateException("Too many response for unary method");
        }
        future.completeExceptionally(throwable);
    }

    @Override
    public void onCompleted() {
        if (future.isDone() || future.isCancelled() || future.isCompletedExceptionally()) {
            return;
        }
        throw new IllegalStateException("Completed without value or exception ");
    }
}
