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
import org.apache.dubbo.mutiny.calls.MutinyClientCalls;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.stub.StubInvocationUtil;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit test for MutinyClientCalls
 */
public class MutinyClientCallsTest {

    @Test
    void testOneToOneSuccess() {
        Invoker<Object> invoker = Mockito.mock(Invoker.class);
        StubMethodDescriptor method = Mockito.mock(StubMethodDescriptor.class);

        try (MockedStatic<StubInvocationUtil> mocked = Mockito.mockStatic(StubInvocationUtil.class)) {
            mocked.when(() -> StubInvocationUtil.unaryCall(
                            Mockito.eq(invoker), Mockito.eq(method), Mockito.eq("req"), Mockito.any()))
                    .thenAnswer(invocation -> {
                        StreamObserver<String> observer = invocation.getArgument(3);
                        observer.onNext("resp");
                        observer.onCompleted();
                        return null;
                    });

            Uni<String> request = Uni.createFrom().item("req");
            Uni<String> response = MutinyClientCalls.oneToOne(invoker, request, method);

            String result = response.await().indefinitely();

            Assertions.assertEquals("resp", result);
        }
    }

    @Test
    void testOneToOneThrowsErrorWithMutinyAwait() {
        Invoker<Object> invoker = Mockito.mock(Invoker.class);
        StubMethodDescriptor method = Mockito.mock(StubMethodDescriptor.class);

        try (MockedStatic<StubInvocationUtil> mocked = Mockito.mockStatic(StubInvocationUtil.class)) {
            mocked.when(() -> StubInvocationUtil.unaryCall(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                    .thenThrow(new RuntimeException("boom"));

            Uni<String> request = Uni.createFrom().item("req");
            Uni<String> response = MutinyClientCalls.oneToOne(invoker, request, method);

            RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> {
                response.await().indefinitely();
            });
            Assertions.assertTrue(ex.getMessage().contains("boom"));
        }
    }

    @Test
    void testOneToManyReturnsMultiAndEmitsItems() {
        Invoker<Object> invoker = Mockito.mock(Invoker.class);
        StubMethodDescriptor method = Mockito.mock(StubMethodDescriptor.class);

        try (MockedStatic<StubInvocationUtil> mocked = Mockito.mockStatic(StubInvocationUtil.class)) {
            AtomicBoolean stubCalled = new AtomicBoolean(false);
            CountDownLatch subscribed = new CountDownLatch(1);

            mocked.when(() -> StubInvocationUtil.serverStreamCall(
                            Mockito.eq(invoker), Mockito.eq(method), Mockito.eq("testRequest"), Mockito.any()))
                    .thenAnswer(invocation -> {
                        stubCalled.set(true);
                        ClientTripleMutinyPublisher<String> publisher = invocation.getArgument(3);

                        CallStreamObserver<String> fakeSubscription = new CallStreamObserver<>() {
                            @Override
                            public void request(int n) {
                                /* no-op */
                            }

                            @Override
                            public void setCompression(String compression) {}

                            @Override
                            public void disableAutoFlowControl() {}

                            @Override
                            public boolean isReady() {
                                return true;
                            }

                            @Override
                            public void setOnReadyHandler(Runnable onReadyHandler) {
                                /* no-op for test */
                            }

                            @Override
                            public void onNext(String v) {
                                publisher.onNext(v);
                            }

                            @Override
                            public void onError(Throwable t) {
                                publisher.onError(t);
                            }

                            @Override
                            public void onCompleted() {
                                publisher.onCompleted();
                            }
                        };
                        publisher.onSubscribe(fakeSubscription);

                        // Wait for downstream subscription to complete before emitting data
                        new Thread(() -> {
                                    try {
                                        if (subscribed.await(5, TimeUnit.SECONDS)) {
                                            publisher.onNext("item1");
                                            publisher.onNext("item2");
                                            publisher.onCompleted();
                                        } else {
                                            publisher.onError(
                                                    new IllegalStateException("Downstream subscription timeout"));
                                        }
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        publisher.onError(e);
                                    }
                                })
                                .start();

                        return null;
                    });

            Uni<String> uniRequest = Uni.createFrom().item("testRequest");
            Multi<String> multiResponse = MutinyClientCalls.oneToMany(invoker, uniRequest, method);

            // Use AssertSubscriber to ensure proper subscription timing
            AssertSubscriber<String> subscriber = AssertSubscriber.create(Long.MAX_VALUE);
            multiResponse.subscribe().withSubscriber(subscriber);

            // Wait for subscription to be established
            subscriber.awaitSubscription();
            subscribed.countDown(); // Signal that data emission can begin

            // Wait for completion
            subscriber.awaitCompletion(Duration.ofSeconds(5));

            // Verify results
            Assertions.assertTrue(stubCalled.get(), "StubInvocationUtil.serverStreamCall should be called");
            Assertions.assertEquals(List.of("item1", "item2"), subscriber.getItems());
            subscriber.assertCompleted();
        }
    }

    @Test
    void testManyToOneSuccess() {
        Invoker<Object> invoker = Mockito.mock(Invoker.class);
        StubMethodDescriptor method = Mockito.mock(StubMethodDescriptor.class);

        Multi<String> multiRequest = Multi.createFrom().items("a", "b", "c");

        try (MockedStatic<StubInvocationUtil> mocked = Mockito.mockStatic(StubInvocationUtil.class)) {
            AtomicBoolean stubCalled = new AtomicBoolean(false);

            mocked.when(() -> StubInvocationUtil.biOrClientStreamCall(
                            Mockito.eq(invoker), Mockito.eq(method), Mockito.any()))
                    .thenAnswer(invocation -> {
                        stubCalled.set(true);
                        return null;
                    });

            Uni<String> uniResponse = MutinyClientCalls.manyToOne(invoker, multiRequest, method);

            AtomicReference<String> resultHolder = new AtomicReference<>();
            AtomicReference<Throwable> errorHolder = new AtomicReference<>();

            CountDownLatch latch = new CountDownLatch(1);

            uniResponse
                    .subscribe()
                    .with(
                            item -> {
                                resultHolder.set(item);
                                latch.countDown();
                            },
                            failure -> {
                                errorHolder.set(failure);
                                latch.countDown();
                            });

            try {
                latch.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Assertions.assertTrue(stubCalled.get(), "StubInvocationUtil.biOrClientStreamCall should be called");
            Assertions.assertNull(errorHolder.get(), "No error expected");
        }
    }
}
