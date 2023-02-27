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

import org.apache.dubbo.reactive.handler.OneToManyMethodHandler;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Unit test for OneToManyMethodHandler
 */
public final class OneToManyMethodHandlerTest {

    @Test
    void testInvoke() {
        String request = "1,2,3,4,5,6,7";
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
        OneToManyMethodHandler<String, String> handler = new OneToManyMethodHandler<>(requestMono ->
            requestMono.flatMapMany(r -> Flux.fromArray(r.split(","))));
        CompletableFuture<?> future = handler.invoke(new Object[]{request, responseObserver});
        Assertions.assertTrue(future.isDone());
        Assertions.assertEquals(7, nextCounter.get());
        Assertions.assertEquals(0, errorCounter.get());
        Assertions.assertEquals(1, completeCounter.get());
    }

    @Test
    void testError() {
        String request = "1,2,3,4,5,6,7";
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
        OneToManyMethodHandler<String, String> handler = new OneToManyMethodHandler<>(requestMono ->
            Flux.create(emitter -> {
                for (int i = 0; i < 10; i++) {
                    if (i == 6) {
                        emitter.error(new Throwable());
                    } else {
                        emitter.next(String.valueOf(i));
                    }
                }
            }));
        CompletableFuture<?> future = handler.invoke(new Object[]{request, responseObserver});
        Assertions.assertTrue(future.isDone());
        Assertions.assertEquals(6, nextCounter.get());
        Assertions.assertEquals(1, errorCounter.get());
        Assertions.assertEquals(0, completeCounter.get());
    }
}
