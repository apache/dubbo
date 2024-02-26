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

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

/**
 * Unit test for OneToManyMethodHandler
 */
public final class OneToManyMethodHandlerTest {

    private CreateObserverAdapter creator;

    @BeforeEach
    void init() {
        creator = new CreateObserverAdapter();
    }

    @Test
    void testInvoke() {
        String request = "1,2,3,4,5,6,7";
        OneToManyMethodHandler<String, String> handler =
                new OneToManyMethodHandler<>(requestMono -> requestMono.flatMapMany(r -> Flux.fromArray(r.split(","))));
        CompletableFuture<?> future = handler.invoke(new Object[] {request, creator.getResponseObserver()});
        Assertions.assertTrue(future.isDone());
        Assertions.assertEquals(7, creator.getNextCounter().get());
        Assertions.assertEquals(0, creator.getErrorCounter().get());
        Assertions.assertEquals(1, creator.getCompleteCounter().get());
    }

    @Test
    void testError() {
        String request = "1,2,3,4,5,6,7";
        OneToManyMethodHandler<String, String> handler =
                new OneToManyMethodHandler<>(requestMono -> Flux.create(emitter -> {
                    for (int i = 0; i < 10; i++) {
                        if (i == 6) {
                            emitter.error(new Throwable());
                        } else {
                            emitter.next(String.valueOf(i));
                        }
                    }
                }));
        CompletableFuture<?> future = handler.invoke(new Object[] {request, creator.getResponseObserver()});
        Assertions.assertTrue(future.isDone());
        Assertions.assertEquals(6, creator.getNextCounter().get());
        Assertions.assertEquals(1, creator.getErrorCounter().get());
        Assertions.assertEquals(0, creator.getCompleteCounter().get());
    }
}
