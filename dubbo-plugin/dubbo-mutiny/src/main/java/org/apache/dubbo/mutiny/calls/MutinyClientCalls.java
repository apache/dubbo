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
import org.apache.dubbo.mutiny.ClientTripleMutinyPublisher;
import org.apache.dubbo.mutiny.ClientTripleMutinySubscriber;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.stub.StubInvocationUtil;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

/**
 * A collection of methods to convert client-side Mutiny calls to stream calls.
 */
public class MutinyClientCalls {

    private MutinyClientCalls() {}

    /**
     * Implements a unary -> unary call as Uni -> Uni
     *
     * @param invoker invoker
     * @param uniRequest the uni with request
     * @param methodDescriptor the method descriptor
     * @return the uni with response
     */
    public static <TRequest, TResponse, TInvoker> Uni<TResponse> oneToOne(
            Invoker<TInvoker> invoker, Uni<TRequest> uniRequest, StubMethodDescriptor methodDescriptor) {
        try {
            return uniRequest.onItem().transformToUni(request -> Uni.createFrom()
                    .emitter((UniEmitter<? super TResponse> emitter) -> {
                        StubInvocationUtil.unaryCall(
                                invoker, methodDescriptor, request, new StreamObserver<TResponse>() {
                                    @Override
                                    public void onNext(TResponse value) {
                                        emitter.complete(value);
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        emitter.fail(t);
                                    }

                                    @Override
                                    public void onCompleted() {
                                        // No-op
                                    }
                                });
                    }));
        } catch (Throwable throwable) {
            return Uni.createFrom().failure(throwable);
        }
    }

    /**
     * Implements a unary -> stream call as Uni -> Multi
     *
     * @param invoker invoker
     * @param uniRequest the uni with request
     * @param methodDescriptor the method descriptor
     * @return the multi with response
     */
    public static <TRequest, TResponse, TInvoker> Multi<TResponse> oneToMany(
            Invoker<TInvoker> invoker, Uni<TRequest> uniRequest, StubMethodDescriptor methodDescriptor) {
        try {
            return uniRequest.onItem().transformToMulti(request -> {
                ClientTripleMutinyPublisher<TResponse> clientPublisher = new ClientTripleMutinyPublisher<>();
                StubInvocationUtil.serverStreamCall(invoker, methodDescriptor, request, clientPublisher);
                return clientPublisher;
            });
        } catch (Throwable throwable) {
            return Multi.createFrom().failure(throwable);
        }
    }

    /**
     * Implements a stream -> unary call as Multi -> Uni
     *
     * @param invoker invoker
     * @param multiRequest the multi with request
     * @param methodDescriptor the method descriptor
     * @return the uni with response
     */
    public static <TRequest, TResponse, TInvoker> Uni<TResponse> manyToOne(
            Invoker<TInvoker> invoker, Multi<TRequest> multiRequest, StubMethodDescriptor methodDescriptor) {
        try {
            ClientTripleMutinySubscriber<TRequest> clientSubscriber =
                    multiRequest.subscribe().withSubscriber(new ClientTripleMutinySubscriber<>());
            ClientTripleMutinyPublisher<TResponse> clientPublisher = new ClientTripleMutinyPublisher<>(
                    s -> clientSubscriber.subscribe((CallStreamObserver<TRequest>) s), clientSubscriber::cancel);
            return Uni.createFrom()
                    .publisher(clientPublisher)
                    .onSubscription()
                    .invoke(() -> StubInvocationUtil.biOrClientStreamCall(invoker, methodDescriptor, clientPublisher));
        } catch (Throwable err) {
            return Uni.createFrom().failure(err);
        }
    }

    /**
     * Implements a stream -> stream call as Multi -> Multi
     *
     * @param invoker invoker
     * @param multiRequest the multi with request
     * @param methodDescriptor the method descriptor
     * @return the multi with response
     */
    public static <TRequest, TResponse, TInvoker> Multi<TResponse> manyToMany(
            Invoker<TInvoker> invoker, Multi<TRequest> multiRequest, StubMethodDescriptor methodDescriptor) {
        try {
            ClientTripleMutinySubscriber<TRequest> clientSubscriber =
                    multiRequest.subscribe().withSubscriber(new ClientTripleMutinySubscriber<>());
            ClientTripleMutinyPublisher<TResponse> clientPublisher = new ClientTripleMutinyPublisher<>(
                    s -> clientSubscriber.subscribe((CallStreamObserver<TRequest>) s), clientSubscriber::cancel);
            return Multi.createFrom()
                    .publisher(clientPublisher)
                    .onSubscription()
                    .invoke(() -> StubInvocationUtil.biOrClientStreamCall(invoker, methodDescriptor, clientPublisher));
        } catch (Throwable err) {
            return Multi.createFrom().failure(err);
        }
    }
}
