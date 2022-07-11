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

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.reactive.ClientTripleReactorPublisher;
import org.apache.dubbo.rpc.stub.StubInvocationUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A collection of methods to convert client-side Reactor calls to stream calls.
 */
public final class ReactorClientCalls {

    private ReactorClientCalls() {
    }

    // TODO oneToOne, ManyToOne, ManyToMany

    /**
     * Implements a unary -> stream call as Mono -> Flux
     *
     * @param invoker invoker
     * @param monoRequest the mono with request
     * @param methodDescriptor the method descriptor
     * @return the flux with response
     */
    public static <TRequest, TResponse, TInvoker> Flux<TResponse> oneToMany(Invoker<TInvoker> invoker,
                                                                            Mono<TRequest> monoRequest,
                                                                            StubMethodDescriptor methodDescriptor) {
        try {
            return monoRequest
                .flatMapMany(request -> {
                    ClientTripleReactorPublisher<TResponse> consumerStreamObserver =
                        new ClientTripleReactorPublisher<>();
                    StubInvocationUtil.serverStreamCall(invoker, methodDescriptor , request, consumerStreamObserver);
                    return consumerStreamObserver;
                });
        } catch (Throwable throwable) {
            return Flux.error(throwable);
        }
    }
}
