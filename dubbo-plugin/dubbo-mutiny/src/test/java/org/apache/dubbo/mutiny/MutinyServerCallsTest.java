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
package org.apache.dubbo.mutiny;

import org.apache.dubbo.common.stream.CallStreamObserver;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.mutiny.calls.MutinyServerCalls;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit test for MutinyServerCalls
 */
public class MutinyServerCallsTest {

    @Test
    void testOneToOne_success() {
        StreamObserver<String> responseObserver = mock(StreamObserver.class);

        Function<Uni<String>, Uni<String>> func = reqUni -> reqUni.onItem().transform(i -> i + "-resp");

        MutinyServerCalls.oneToOne("req", responseObserver, func);

        // responseObserver
        verify(responseObserver, times(1)).onNext("req-resp");
        verify(responseObserver, times(1)).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @Test
    void testOneToOne_exception() {
        StreamObserver<String> responseObserver = mock(StreamObserver.class);

        // mock func error
        Function<Uni<String>, Uni<String>> func = reqUni -> {
            throw new RuntimeException("fail");
        };

        MutinyServerCalls.oneToOne("req", responseObserver, func);

        verify(responseObserver, times(1)).onError(any());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    void testOneToMany_success() throws ExecutionException, InterruptedException {
        CallStreamObserver<String> responseObserver = mock(CallStreamObserver.class);

        // multi results
        Function<Uni<String>, Multi<String>> func = reqUni -> Multi.createFrom().items("a", "b", "c");

        CompletableFuture<List<String>> future = MutinyServerCalls.oneToMany("req", responseObserver, func);

        List<String> results = future.get();

        assertEquals(3, results.size());

        // test responseObserver
        verify(responseObserver, atLeastOnce()).onNext(any());
        verify(responseObserver, times(1)).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @Test
    void testOneToMany_exception() {
        CallStreamObserver<String> responseObserver = mock(CallStreamObserver.class);

        Function<Uni<String>, Multi<String>> func = reqUni -> {
            throw new RuntimeException("fail");
        };

        CompletableFuture<List<String>> future = MutinyServerCalls.oneToMany("req", responseObserver, func);

        assertTrue(future.isCompletedExceptionally());

        verify(responseObserver, times(1)).onError(any());
    }

    @Test
    void testManyToOne_success() throws InterruptedException {
        CallStreamObserver<String> responseObserver = mock(CallStreamObserver.class);

        // return uni
        Function<Multi<String>, Uni<String>> func =
                multi -> multi.collect().asList().onItem().transform(list -> "size:" + list.size());

        StreamObserver<String> requestObserver = MutinyServerCalls.manyToOne(responseObserver, func);

        // mock onNext/onCompleted
        requestObserver.onNext("a");
        requestObserver.onNext("b");
        requestObserver.onNext("c");
        requestObserver.onCompleted();

        Thread.sleep(200);

        verify(responseObserver, times(1)).onNext("size:3");
        verify(responseObserver, times(1)).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @Test
    void testManyToOne_funcThrows() {
        CallStreamObserver<String> responseObserver = mock(CallStreamObserver.class);

        Function<Multi<String>, Uni<String>> func = multi -> {
            throw new RuntimeException("fail");
        };

        StreamObserver<String> requestObserver = MutinyServerCalls.manyToOne(responseObserver, func);

        verify(responseObserver, times(1)).onError(any());
    }

    @Test
    void testManyToMany_success() throws InterruptedException {
        CallStreamObserver<String> responseObserver = mock(CallStreamObserver.class);

        Function<Multi<String>, Multi<String>> func = multi -> multi.map(s -> s + "-resp");

        StreamObserver<String> requestObserver = MutinyServerCalls.manyToMany(responseObserver, func);

        // mock onNext/onCompleted
        requestObserver.onNext("x");
        requestObserver.onNext("y");
        requestObserver.onCompleted();

        Thread.sleep(200);

        verify(responseObserver, atLeastOnce()).onNext(any());
        verify(responseObserver, times(1)).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @Test
    void testManyToMany_funcThrows() {
        CallStreamObserver<String> responseObserver = mock(CallStreamObserver.class);

        Function<Multi<String>, Multi<String>> func = multi -> {
            throw new RuntimeException("fail");
        };

        StreamObserver<String> requestObserver = MutinyServerCalls.manyToMany(responseObserver, func);

        verify(responseObserver, times(1)).onError(any());
    }
}
