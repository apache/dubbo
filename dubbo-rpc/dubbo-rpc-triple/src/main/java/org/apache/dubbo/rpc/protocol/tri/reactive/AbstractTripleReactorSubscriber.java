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

package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.util.annotation.NonNull;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * The middle layer between {@link org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver} and Reactive API. <br>
 * Passing the data from Reactive producer to CallStreamObserver.
 */
public abstract class AbstractTripleReactorSubscriber<T> implements Subscriber<T>, CoreSubscriber<T> {

    private static final Subscription CANCELLED_SUBSCRIPTION = new Subscription() {
        @Override
        public void cancel() {}
        @Override
        public void request(long n) {}
    };

    protected volatile CallStreamObserver<T> downstream;

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractTripleReactorSubscriber, CallStreamObserver> DOWNSTREAM =
        AtomicReferenceFieldUpdater.newUpdater(AbstractTripleReactorSubscriber.class, CallStreamObserver.class, "downstream");

    private volatile Subscription subscription;

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractTripleReactorSubscriber, Subscription> SUBSCRIPTION =
        AtomicReferenceFieldUpdater.newUpdater(AbstractTripleReactorSubscriber.class, Subscription.class, "subscription");

    // complete status
    private volatile boolean isDone;

    /**
     * Binding the downstream, and call subscription#request(1).
     *
     * @param downstream downstream
     */
    public void subscribe(final CallStreamObserver<T> downstream) {
        if (downstream == null) {
            throw new NullPointerException();
        }
        if (this.downstream == null && DOWNSTREAM.compareAndSet(this, null, downstream)) {
            subscription.request(1);
            return;
        }

        throw new IllegalStateException(getClass().getSimpleName() + " does not support multiple subscribers");
    }

    @Override
    public void onSubscribe(@NonNull final Subscription subscription) {
        if (this.subscription == null && SUBSCRIPTION.compareAndSet(this, null, subscription)) {
            return;
        }
        // onSubscribe cannot be called repeatedly
        subscription.cancel();
    }

    @Override
    public void onNext(T t) {
        if (!isDone && !isCanceled()) {
            downstream.onNext(t);
            subscription.request(1);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (!isCanceled()) {
            isDone = true;
            downstream.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        if (!isCanceled()) {
            isDone = true;
            downstream.onCompleted();
        }
    }

    public void cancel() {
        Subscription subscription = SUBSCRIPTION.getAndSet(this, CANCELLED_SUBSCRIPTION);
        if (subscription != null && subscription != CANCELLED_SUBSCRIPTION) {
            subscription.cancel();
        }
    }

    public boolean isCanceled() {
        return subscription == CANCELLED_SUBSCRIPTION;
    }
}
