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

package org.apache.dubbo.reactive;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.reactive.handler.ManyToManyMethodHandler;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Unit test for ManyToManyMethodHandler
 */
public final class ManyToManyMethodHandlerTest {

    @Test
    void testInvoke() throws ExecutionException, InterruptedException {
        AtomicInteger nextCounter = new AtomicInteger();
        AtomicInteger completeCounter = new AtomicInteger();
        AtomicInteger errorCounter = new AtomicInteger();
        ServerCallToObserverAdapter<String> responseObserver = Mockito.mock(ServerCallToObserverAdapter.class);
        doAnswer(o -> nextCounter.incrementAndGet())
            .when(responseObserver).onNext(anyString());
        doAnswer(o -> completeCounter.incrementAndGet())
            .when(responseObserver).onCompleted();
        doAnswer(o -> errorCounter.incrementAndGet())
            .when(responseObserver).onError(any(Throwable.class));
        ManyToManyMethodHandler<String, String> handler = new ManyToManyMethodHandler<>(requestFlux ->
            requestFlux.map(r -> r + "0"));
        CompletableFuture<StreamObserver<String>> future = handler.invoke(new Object[]{responseObserver});
        StreamObserver<String> requestObserver = future.get();
        for (int i = 0; i < 10; i++) {
            requestObserver.onNext(String.valueOf(i));
        }
        requestObserver.onCompleted();
        Assertions.assertEquals(10, nextCounter.get());
        Assertions.assertEquals(0, errorCounter.get());
        Assertions.assertEquals(1, completeCounter.get());
    }
}
