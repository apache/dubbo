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

import org.apache.dubbo.rpc.protocol.tri.ClientStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.SafeRequestObserver;
import org.apache.dubbo.rpc.protocol.tri.ServerStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

/**
 * The middle layer between {@link org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver} and Reactive API. <p>
 * 1. passing the data received by CallStreamObserver to Reactive consumer <br>
 * 2. passing the request of Reactive API to CallStreamObserver
 */
public abstract class AbstractTripleReactorPublisher<T> implements Publisher<T>, SafeRequestObserver<T>, Subscription {

    private static final Subscription EMPTY_SUBSCRIPTION = new Subscription() {
        @Override
        public void cancel() {
        }
        @Override
        public void request(long n) {
        }
    };

    private volatile long requested;

    private static final AtomicLongFieldUpdater<AbstractTripleReactorPublisher> REQUESTED =
        AtomicLongFieldUpdater.newUpdater(AbstractTripleReactorPublisher.class, "requested");

    // weather CallStreamObserver#request can be called
    private final AtomicBoolean CAN_REQUEST = new AtomicBoolean();

    // weather publisher has been subscribed
    private final AtomicBoolean SUBSCRIBED = new AtomicBoolean();

    private volatile Subscriber<? super T> downstream;

    protected volatile CallStreamObserver<?> subscription;

    private static final AtomicReferenceFieldUpdater<AbstractTripleReactorPublisher, CallStreamObserver> SUBSCRIPTION =
        AtomicReferenceFieldUpdater.newUpdater(AbstractTripleReactorPublisher.class, CallStreamObserver.class, "subscription");

    // cancel status
    private volatile boolean isCancelled;

    // complete status
    private volatile boolean isDone;

    // to help bind TripleSubscriber
    private volatile Consumer<CallStreamObserver<?>> onSubscribe;

    private volatile Runnable shutdownHook;

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractTripleReactorPublisher, Runnable> SHUTDOWN_HOOK =
        AtomicReferenceFieldUpdater.newUpdater(AbstractTripleReactorPublisher.class, Runnable.class, "shutdownHook");

    public AbstractTripleReactorPublisher() {
    }

    public AbstractTripleReactorPublisher(Consumer<CallStreamObserver<?>> onSubscribe, Runnable shutdownHook) {
        this.onSubscribe = onSubscribe;
        this.shutdownHook = shutdownHook;
    }

    protected void onSubscribe(final CallStreamObserver<?> subscription) {
        if (subscription != null && SUBSCRIPTION.compareAndSet(this, null, subscription)) {
            this.subscription = subscription;
            if (subscription instanceof ClientStreamObserver<?>) {
                ((ClientStreamObserver<?>) subscription).disableAutoRequest();
            } else if (subscription instanceof ServerStreamObserver<?>) {
                ((ServerStreamObserver<?>) subscription).disableAutoInboundFlowControl();
            }
            if (onSubscribe != null) {
                onSubscribe.accept(subscription);
            }
            return;
        }

        throw new IllegalStateException(getClass().getSimpleName() + " supports only a single subscription");
    }

    @Override
    public void onNext(T data) {
        if (isDone || isCancelled) {
            return;
        }
        downstream.onNext(data);
    }

    @Override
    public void onError(Throwable throwable) {
        if (isDone || isCancelled) {
            return;
        }
        downstream.onError(throwable);
        isDone = true;
        doPostShutdown();
    }

    @Override
    public void onCompleted() {
        if (isDone || isCancelled) {
            return;
        }
        isDone = true;
        downstream.onComplete();
        doPostShutdown();
    }

    private void doPostShutdown() {
        Runnable r = shutdownHook;
        // CAS to confirm shutdownHook will be run only once.
        if (r != null && SHUTDOWN_HOOK.compareAndSet(this, r, null)) {
            r.run();
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException();
        }

        if (SUBSCRIBED.compareAndSet(false, true)) {
            subscriber.onSubscribe(this);
            this.downstream = subscriber;
            if (isCancelled) {
                this.downstream = null;
            }
        } else {
            subscriber.onSubscribe(EMPTY_SUBSCRIPTION);
            subscriber.onError(new IllegalStateException(getClass().getSimpleName() + " can't be subscribed repeatedly"));
        }
    }

    @Override
    public void request(long l) {
        if (SUBSCRIBED.get() && CAN_REQUEST.get()) {
            subscription.request(l >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) l);
        } else {
            REQUESTED.getAndAdd(this, l);
        }
    }

    @Override
    public void startRequest() {
        if (CAN_REQUEST.compareAndSet(false, true)) {
            long count = requested;
            subscription.request(count >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count);
        }
    }

    @Override
    public void cancel() {
        if (isCancelled) {
            return;
        }
        isCancelled = true;
        doPostShutdown();
    }

    public boolean isCancelled() {
        return isCancelled;
    }
}
