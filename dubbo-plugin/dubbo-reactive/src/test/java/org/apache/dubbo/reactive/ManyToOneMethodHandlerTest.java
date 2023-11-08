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
import org.apache.dubbo.reactive.handler.ManyToOneMethodHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for ManyToOneMethodHandler
 */
public final class ManyToOneMethodHandlerTest {

    private StreamObserver<String> requestObserver;
    private CreateObserverAdapter creator;

    @BeforeEach
    void init() throws ExecutionException, InterruptedException {
        creator = new CreateObserverAdapter();
        ManyToOneMethodHandler<String, String> handler = new ManyToOneMethodHandler<>(requestFlux ->
                requestFlux.map(Integer::valueOf).reduce(Integer::sum).map(String::valueOf));
        CompletableFuture<StreamObserver<String>> future = handler.invoke(new Object[] {creator.getResponseObserver()});
        requestObserver = future.get();
    }

    @Test
    void testInvoker() {
        for (int i = 0; i < 10; i++) {
            requestObserver.onNext(String.valueOf(i));
        }
        requestObserver.onCompleted();
        Assertions.assertEquals(1, creator.getNextCounter().get());
        Assertions.assertEquals(0, creator.getErrorCounter().get());
        Assertions.assertEquals(1, creator.getCompleteCounter().get());
    }

    @Test
    void testError() {
        for (int i = 0; i < 10; i++) {
            if (i == 6) {
                requestObserver.onError(new Throwable());
            }
            requestObserver.onNext(String.valueOf(i));
        }
        requestObserver.onCompleted();
        Assertions.assertEquals(0, creator.getNextCounter().get());
        Assertions.assertEquals(1, creator.getErrorCounter().get());
        Assertions.assertEquals(0, creator.getCompleteCounter().get());
    }
}
