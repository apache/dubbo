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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for AbstractTripleMutinyPublisher
 */
public class TripleMutinyPublisherTest {

    @Test
    public void testSubscribeAndRequest() {
        AtomicBoolean subscribed = new AtomicBoolean(false);

        AbstractTripleMutinyPublisher<String> publisher = new AbstractTripleMutinyPublisher<>() {
            @Override
            protected void onSubscribe(CallStreamObserver<?> subscription) {
                subscribed.set(true);
                this.subscription = Mockito.mock(CallStreamObserver.class);
            }
        };

        publisher.onSubscribe(Mockito.mock(CallStreamObserver.class));

        Flow.Subscriber<String> subscriber = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(String item) {}

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {}
        };

        publisher.subscribe(subscriber);
        assertTrue(subscribed.get());
    }

    @Test
    public void testRequestBeforeStartRequest() {
        CallStreamObserver<?> mockObserver = Mockito.mock(CallStreamObserver.class);

        AbstractTripleMutinyPublisher<String> publisher = new AbstractTripleMutinyPublisher<>() {};
        publisher.onSubscribe(mockObserver);
        publisher.request(5L); // should accumulate, not call request()
        Mockito.verify(mockObserver, Mockito.never()).request(Mockito.anyInt());

        publisher.startRequest(); // now should flush request
        Mockito.verify(mockObserver).request(5);
    }

    @Test
    public void testCancelTriggersShutdownHook() {
        AtomicBoolean shutdown = new AtomicBoolean(false);

        AbstractTripleMutinyPublisher<String> publisher =
                new AbstractTripleMutinyPublisher<>(null, () -> shutdown.set(true)) {};

        publisher.cancel();
        assertTrue(publisher.isCancelled());
        assertTrue(shutdown.get());
    }

    @Test
    public void testOnNextAndComplete() {
        List<String> received = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean();

        AbstractTripleMutinyPublisher<String> publisher = new AbstractTripleMutinyPublisher<>() {};

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {}

            @Override
            public void onNext(String item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        publisher.onNext("hello");
        publisher.onNext("world");
        publisher.onCompleted();

        assertEquals(List.of("hello", "world"), received);
        assertTrue(completed.get());
    }
}
