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

import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The middle layer between {@link org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver} and Reactive API. <p>
 * 1. passing the data received by CallStreamObserver to Reactive consumer <br>
 * 2. passing the request of Reactive API to CallStreamObserver
 */
public abstract class AbstractTripleReactorPublisher<T> extends CancelableStreamObserver<T> implements Publisher<T>, Subscription {

    private boolean canRequest;

    private long requested;

    // weather publisher has been subscribed
    private final AtomicBoolean SUBSCRIBED = new AtomicBoolean();

    private volatile Subscriber<? super T> downstream;

    protected volatile CallStreamObserver<?> subscription;

    private final AtomicBoolean HAS_SUBSCRIPTION = new AtomicBoolean();

    // cancel status
    private volatile boolean isCancelled;

    // complete status
    private volatile boolean isDone;

    // to help bind TripleSubscriber
    private volatile Consumer<CallStreamObserver<?>> onSubscribe;

    private volatile Runnable shutdownHook;

    private final AtomicBoolean CALLED_SHUT_DOWN_HOOK = new AtomicBoolean();

    public AbstractTripleReactorPublisher() {
    }

    public AbstractTripleReactorPublisher(Consumer<CallStreamObserver<?>> onSubscribe, Runnable shutdownHook) {
        this.onSubscribe = onSubscribe;
        this.shutdownHook = shutdownHook;
    }

    protected void onSubscribe(final CallStreamObserver<?> subscription) {
        if (subscription != null && this.subscription == null && HAS_SUBSCRIPTION.compareAndSet(false, true)) {
            this.subscription = subscription;
            subscription.disableAutoFlowControl();
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
        isDone = true;
        downstream.onError(throwable);
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
        if (r != null && CALLED_SHUT_DOWN_HOOK.compareAndSet(false, true)) {
            shutdownHook = null;
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
        }
    }

    @Override
    public void request(long l) {
        synchronized (this) {
            if (SUBSCRIBED.get() && canRequest) {
                subscription.request(l >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) l);
            } else {
                requested += l;
            }
        }
    }

    @Override
    public void startRequest() {
        synchronized (this) {
            if (!canRequest) {
                canRequest = true;
                long count = requested;
                subscription.request(count >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count);
            }
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
