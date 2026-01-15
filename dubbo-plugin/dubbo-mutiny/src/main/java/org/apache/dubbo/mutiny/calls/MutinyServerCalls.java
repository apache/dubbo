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
package org.apache.dubbo.mutiny.calls;

import org.apache.dubbo.common.stream.CallStreamObserver;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.mutiny.ServerTripleMutinyPublisher;
import org.apache.dubbo.mutiny.ServerTripleMutinySubscriber;
import org.apache.dubbo.rpc.StatusRpcException;
import org.apache.dubbo.rpc.TriRpcStatus;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * A collection of methods to convert server-side stream calls to Mutiny calls.
 */
public class MutinyServerCalls {

    private MutinyServerCalls() {}

    /**
     * Implements a unary -> unary call as Uni -> Uni
     *
     * @param request request
     * @param responseObserver response StreamObserver
     * @param func service implementation
     */
    public static <T, R> void oneToOne(T request, StreamObserver<R> responseObserver, Function<Uni<T>, Uni<R>> func) {
        try {
            func.apply(Uni.createFrom().item(request))
                    .onItem()
                    .ifNull()
                    .failWith(TriRpcStatus.NOT_FOUND.asException())
                    .subscribe()
                    .with(
                            item -> {
                                responseObserver.onNext(item);
                                responseObserver.onCompleted();
                            },
                            throwable -> doOnResponseHasException(throwable, responseObserver));
        } catch (Throwable throwable) {
            doOnResponseHasException(throwable, responseObserver);
        }
    }

    /**
     * Implements a unary -> stream call as Uni -> Multi
     *
     * @param request request
     * @param responseObserver response StreamObserver
     * @param func service implementation
     */
    public static <T, R> CompletableFuture<List<R>> oneToMany(
            T request, StreamObserver<R> responseObserver, Function<Uni<T>, Multi<R>> func) {
        try {
            CallStreamObserver<R> callStreamObserver = (CallStreamObserver<R>) responseObserver;
            Multi<R> response = func.apply(Uni.createFrom().item(request));
            ServerTripleMutinySubscriber<R> mutinySubscriber = new ServerTripleMutinySubscriber<>(callStreamObserver);
            response.subscribe().withSubscriber(mutinySubscriber).subscribe(callStreamObserver);
            return mutinySubscriber.getExecutionFuture();
        } catch (Throwable throwable) {
            doOnResponseHasException(throwable, responseObserver);
            CompletableFuture<List<R>> failed = new CompletableFuture<>();
            failed.completeExceptionally(throwable);
            return failed;
        }
    }

    /**
     * Implements a stream -> unary call as Multi -> Uni
     *
     * @param responseObserver response StreamObserver
     * @param func service implementation
     * @return request StreamObserver
     */
    public static <T, R> StreamObserver<T> manyToOne(
            StreamObserver<R> responseObserver, Function<Multi<T>, Uni<R>> func) {
        CallStreamObserver<R> callStreamObserver = (CallStreamObserver<R>) responseObserver;
        ServerTripleMutinyPublisher<T> serverPublisher = new ServerTripleMutinyPublisher<>(callStreamObserver);
        try {
            Uni<R> responseUni = func.apply(Multi.createFrom().publisher(serverPublisher))
                    .onItem()
                    .ifNull()
                    .failWith(TriRpcStatus.NOT_FOUND.asException());
            responseUni
                    .subscribe()
                    .with(
                            value -> {
                                if (!serverPublisher.isCancelled()) {
                                    callStreamObserver.onNext(value);
                                    callStreamObserver.onCompleted();
                                }
                            },
                            throwable -> {
                                if (!serverPublisher.isCancelled()) {
                                    callStreamObserver.onError(throwable);
                                }
                            });
            serverPublisher.startRequest();
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }
        return serverPublisher;
    }

    /**
     * Implements a stream -> stream call as Multi -> Multi
     *
     * @param responseObserver response StreamObserver
     * @param func service implementation
     * @return request StreamObserver
     */
    public static <T, R> StreamObserver<T> manyToMany(
            StreamObserver<R> responseObserver, Function<Multi<T>, Multi<R>> func) {
        CallStreamObserver<R> callStreamObserver = (CallStreamObserver<R>) responseObserver;
        ServerTripleMutinyPublisher<T> serverPublisher = new ServerTripleMutinyPublisher<>(callStreamObserver);
        try {
            Multi<R> responseMulti = func.apply(Multi.createFrom().publisher(serverPublisher));
            ServerTripleMutinySubscriber<R> serverSubscriber =
                    responseMulti.subscribe().withSubscriber(new ServerTripleMutinySubscriber<>());
            serverSubscriber.subscribe(callStreamObserver);
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
