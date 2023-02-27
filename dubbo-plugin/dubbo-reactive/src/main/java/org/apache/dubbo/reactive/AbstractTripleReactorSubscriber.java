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

import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.util.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The middle layer between {@link org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver} and Reactive API. <br>
 * Passing the data from Reactive producer to CallStreamObserver.
 */
public abstract class AbstractTripleReactorSubscriber<T> implements Subscriber<T>, CoreSubscriber<T> {

    private volatile boolean isCancelled;

    protected volatile CallStreamObserver<T> downstream;

    private final AtomicBoolean SUBSCRIBED = new AtomicBoolean();

    private volatile Subscription subscription;

    private final AtomicBoolean HAS_SUBSCRIBED = new AtomicBoolean();

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
        if (this.downstream == null && SUBSCRIBED.compareAndSet(false, true)) {
            this.downstream = downstream;
            subscription.request(1);
        }
    }

    @Override
    public void onSubscribe(@NonNull final Subscription subscription) {
        if (this.subscription == null && HAS_SUBSCRIBED.compareAndSet(false, true)) {
            this.subscription = subscription;
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
        if (!isCancelled && subscription != null) {
            isCancelled = true;
            subscription.cancel();
        }
    }

    public boolean isCanceled() {
        return isCancelled;
    }
}
