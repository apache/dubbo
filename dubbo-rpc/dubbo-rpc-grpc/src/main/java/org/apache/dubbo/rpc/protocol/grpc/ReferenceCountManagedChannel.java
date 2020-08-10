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
package org.apache.dubbo.rpc.protocol.grpc;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Also see ReferenceCountExchangeClient
 */
public class ReferenceCountManagedChannel extends ManagedChannel {

    private final AtomicInteger referenceCount = new AtomicInteger(0);

    private ManagedChannel grpcChannel;

    public ReferenceCountManagedChannel(ManagedChannel delegated) {
        this.grpcChannel = delegated;
    }

    /**
     * The reference count of current ExchangeClient, connection will be closed if all invokers destroyed.
     */
    public void incrementAndGetCount() {
        referenceCount.incrementAndGet();
    }

    @Override
    public ManagedChannel shutdown() {
        if (referenceCount.decrementAndGet() <= 0) {
            return grpcChannel.shutdown();
        }
        return grpcChannel;
    }

    @Override
    public boolean isShutdown() {
        return grpcChannel.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return grpcChannel.isTerminated();
    }

    @Override
    public ManagedChannel shutdownNow() {
        // TODO
        return shutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return grpcChannel.awaitTermination(timeout, unit);
    }

    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
        return grpcChannel.newCall(methodDescriptor, callOptions);
    }

    @Override
    public String authority() {
        return grpcChannel.authority();
    }
}
