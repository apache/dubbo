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

package org.apache.dubbo.reactive.handler;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.reactive.calls.ReactorServerCalls;
import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;
import org.apache.dubbo.rpc.stub.StubMethodHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The handler of ManyToOne() method for stub invocation.
 */
public class ManyToOneMethodHandler<T, R> implements StubMethodHandler<T, R> {

    private final Function<Flux<T>, Mono<R>> func;

    public ManyToOneMethodHandler(Function<Flux<T>, Mono<R>> func) {
        this.func = func;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<StreamObserver<T>> invoke(Object[] arguments) {
        CallStreamObserver<R> responseObserver = (CallStreamObserver<R>) arguments[0];
        StreamObserver<T> requestObserver = ReactorServerCalls.manyToOne(responseObserver, func);
        return CompletableFuture.completedFuture(requestObserver);
    }
}
