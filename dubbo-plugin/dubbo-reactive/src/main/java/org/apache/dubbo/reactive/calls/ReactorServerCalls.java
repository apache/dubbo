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
package org.apache.dubbo.reactive.calls;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.reactive.ServerTripleReactorPublisher;
import org.apache.dubbo.reactive.ServerTripleReactorSubscriber;
import org.apache.dubbo.rpc.StatusRpcException;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A collection of methods to convert server-side stream calls to Reactor calls.
 */
public final class ReactorServerCalls {

    private ReactorServerCalls() {}

    /**
     * Implements a unary -> unary call as Mono -> Mono
     *
     * @param request request
     * @param responseObserver response StreamObserver
     * @param func service implementation
     */
    public static <T, R> void oneToOne(T request, StreamObserver<R> responseObserver, Function<Mono<T>, Mono<R>> func) {
        try {
            func.apply(Mono.just(request))
                    .switchIfEmpty(Mono.error(TriRpcStatus.NOT_FOUND.asException()))
                    .subscribe(
                            responseObserver::onNext,
                            throwable -> doOnResponseHasException(throwable, responseObserver),
                            responseObserver::onCompleted);
        } catch (Throwable throwable) {
            doOnResponseHasException(throwable, responseObserver);
        }
    }

    /**
     * Implements a unary -> stream call as Mono -> Flux
     *
     * @param request request
     * @param responseObserver response StreamObserver
     * @param func service implementation
     */
    public static <T, R> CompletableFuture<List<R>> oneToMany(
            T request, StreamObserver<R> responseObserver, Function<Mono<T>, Flux<R>> func) {
        try {
            ServerCallToObserverAdapter<R> serverCallToObserverAdapter =
                    (ServerCallToObserverAdapter<R>) responseObserver;
            Flux<R> response = func.apply(Mono.just(request));
            ServerTripleReactorSubscriber<R> reactorSubscriber =
                    new ServerTripleReactorSubscriber<>(serverCallToObserverAdapter);
            response.subscribeWith(reactorSubscriber).subscribe(serverCallToObserverAdapter);
            return reactorSubscriber.getExecutionFuture();
        } catch (Throwable throwable) {
            doOnResponseHasException(throwable, responseObserver);
            CompletableFuture<List<R>> future = new CompletableFuture<>();
            future.completeExceptionally(throwable);
            return future;
        }
    }

    /**
     * Implements a stream -> unary call as Flux -> Mono
     *
     * @param responseObserver response StreamObserver
     * @param func service implementation
     * @return request StreamObserver
     */
    public static <T, R> StreamObserver<T> manyToOne(
            StreamObserver<R> responseObserver, Function<Flux<T>, Mono<R>> func) {
        ServerTripleReactorPublisher<T> serverPublisher =
                new ServerTripleReactorPublisher<>((CallStreamObserver<R>) responseObserver);
        try {
            Mono<R> responseMono = func.apply(Flux.from(serverPublisher));
            responseMono.subscribe(
                    value -> {
                        // Don't try to respond if the server has already canceled the request
                        if (!serverPublisher.isCancelled()) {
                            responseObserver.onNext(value);
                        }
                    },
                    throwable -> {
                        // Don't try to respond if the server has already canceled the request
                        if (!serverPublisher.isCancelled()) {
                            responseObserver.onError(throwable);
                        }
                    },
                    responseObserver::onCompleted);
            serverPublisher.startRequest();
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }
        return serverPublisher;
    }

    /**
     * Implements a stream -> stream call as Flux -> Flux
     *
     * @param responseObserver response StreamObserver
     * @param func service implementation
     * @return request StreamObserver
     */
    public static <T, R> StreamObserver<T> manyToMany(
            StreamObserver<R> responseObserver, Function<Flux<T>, Flux<R>> func) {
        // responseObserver is also a subscription of publisher, we can use it to request more data
        ServerTripleReactorPublisher<T> serverPublisher =
                new ServerTripleReactorPublisher<>((CallStreamObserver<R>) responseObserver);
        try {
            Flux<R> responseFlux = func.apply(Flux.from(serverPublisher));
            ServerTripleReactorSubscriber<R> serverSubscriber =
                    responseFlux.subscribeWith(new ServerTripleReactorSubscriber<>());
            serverSubscriber.subscribe((CallStreamObserver<R>) responseObserver);
            serverPublisher.startRequest();
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }

        return serverPublisher;
    }

    private static void doOnResponseHasException(Throwable throwable, StreamObserver<?> responseObserver) {
        StatusRpcException statusRpcException =
                TriRpcStatus.getStatus(throwable).asException();
        responseObserver.onError(statusRpcException);
    }
}
