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

import java.util.concurrent.Flow;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for AbstractTripleMutinySubscriber
 */
public class TripleMutinySubscriberTest {

    @Test
    void testSubscribeBindsDownstreamAndRequests() {
        TestingSubscriber<String> subscriber = new TestingSubscriber<>();
        Flow.Subscription subscription = Mockito.mock(Flow.Subscription.class);
        CallStreamObserver<String> downstream = Mockito.mock(CallStreamObserver.class);

        subscriber.onSubscribe(subscription); // bind subscription
        subscriber.subscribe(downstream); // bind downstream

        Mockito.verify(subscription).request(1);
    }

    @Test
    void testOnNextPassesItemAndRequestsNext() {
        TestingSubscriber<String> subscriber = new TestingSubscriber<>();
        Flow.Subscription subscription = Mockito.mock(Flow.Subscription.class);
        CallStreamObserver<String> downstream = Mockito.mock(CallStreamObserver.class);

        subscriber.onSubscribe(subscription);
        subscriber.subscribe(downstream);

        subscriber.onNext("hello");

        Mockito.verify(downstream).onNext("hello");
        Mockito.verify(subscription, Mockito.times(2)).request(1); // 1st in subscribe, 2nd in onNext
    }

    @Test
    void testOnErrorMarksDoneAndPropagates() {
        TestingSubscriber<String> subscriber = new TestingSubscriber<>();
        Flow.Subscription subscription = Mockito.mock(Flow.Subscription.class);
        CallStreamObserver<String> downstream = Mockito.mock(CallStreamObserver.class);
        RuntimeException error = new RuntimeException("boom");

        subscriber.onSubscribe(subscription);
        subscriber.subscribe(downstream);

        subscriber.onError(error);

        Mockito.verify(downstream).onError(error);
    }

    @Test
    void testOnCompleteMarksDoneAndNotifies() {
        TestingSubscriber<String> subscriber = new TestingSubscriber<>();
        Flow.Subscription subscription = Mockito.mock(Flow.Subscription.class);
        CallStreamObserver<String> downstream = Mockito.mock(CallStreamObserver.class);

        subscriber.onSubscribe(subscription);
        subscriber.subscribe(downstream);

        subscriber.onComplete();

        Mockito.verify(downstream).onCompleted();
    }

    @Test
    void testCancelCancelsSubscription() {
        TestingSubscriber<String> subscriber = new TestingSubscriber<>();
        Flow.Subscription subscription = Mockito.mock(Flow.Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.cancel();

        assertTrue(subscriber.isCancelled());
        Mockito.verify(subscription).cancel();
    }

    @Test
    void testSubscribeTwiceDoesNotRebind() {
        TestingSubscriber<String> subscriber = new TestingSubscriber<>();
        Flow.Subscription subscription = Mockito.mock(Flow.Subscription.class);
        CallStreamObserver<String> downstream1 = Mockito.mock(CallStreamObserver.class);
        CallStreamObserver<String> downstream2 = Mockito.mock(CallStreamObserver.class);

        subscriber.onSubscribe(subscription);
        subscriber.subscribe(downstream1);
        subscriber.subscribe(downstream2);

        subscriber.onNext("test");
        Mockito.verify(downstream1).onNext("test");
        Mockito.verify(downstream2, Mockito.never()).onNext(Mockito.any());
    }

    @Test
    void testOnSubscribeTwiceCancelsSecond() {
        TestingSubscriber<String> subscriber = new TestingSubscriber<>();
        Flow.Subscription sub1 = Mockito.mock(Flow.Subscription.class);
        Flow.Subscription sub2 = Mockito.mock(Flow.Subscription.class);

        subscriber.onSubscribe(sub1);
        subscriber.onSubscribe(sub2); // should cancel sub2

        Mockito.verify(sub2).cancel();
        Mockito.verify(sub1, Mockito.never()).cancel();
    }

    @Test
    void testOnNextAfterDoneDoesNothing() {
        TestingSubscriber<String> subscriber = new TestingSubscriber<>();
        Flow.Subscription subscription = Mockito.mock(Flow.Subscription.class);
        CallStreamObserver<String> downstream = Mockito.mock(CallStreamObserver.class);

        subscriber.onSubscribe(subscription);
        subscriber.subscribe(downstream);
        subscriber.onComplete();

        subscriber.onNext("after-done"); // should be ignored

        Mockito.verify(downstream, Mockito.never()).onNext("after-done");
    }

    static class TestingSubscriber<T> extends AbstractTripleMutinySubscriber<T> {}
}
