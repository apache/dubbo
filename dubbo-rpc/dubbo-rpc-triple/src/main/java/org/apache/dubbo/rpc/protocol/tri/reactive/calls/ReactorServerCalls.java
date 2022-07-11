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

package org.apache.dubbo.rpc.protocol.tri.reactive.calls;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.reactive.AbstractTripleReactorSubscriber;
import org.apache.dubbo.rpc.protocol.tri.reactive.ServerTripleReactorSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * A collection of methods to convert server-side stream calls to Reactor calls.
 */
public final class ReactorServerCalls {

    private ReactorServerCalls() {
    }

    // TODO oneToOne, ManyToOne, ManyToMany

    /**
     * Implements a unary -> stream call as Mono -> Flux
     *
     * @param request request
     * @param responseObserver responseObserver
     * @param func service implementation
     */
    public static <T, R> void oneToMany (T request,
                                         StreamObserver<R> responseObserver,
                                         Function<Mono<T>, Flux<R>> func) {
        try {
            Flux<R> response = func.apply(Mono.just(request));
            AbstractTripleReactorSubscriber<R> subscriber = response.subscribeWith(new ServerTripleReactorSubscriber<>());
            subscriber.subscribe((ServerCallToObserverAdapter<R>) responseObserver);
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }
    }
}
