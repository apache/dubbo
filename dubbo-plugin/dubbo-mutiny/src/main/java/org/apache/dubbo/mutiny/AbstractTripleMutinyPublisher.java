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
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The middle layer between {@link org.apache.dubbo.common.stream.CallStreamObserver} and Mutiny API. <p>
 * 1. passing the data received by CallStreamObserver to Mutiny consumer <br>
 * 2. passing the request of Mutiny API to CallStreamObserver
 */
public abstract class AbstractTripleMutinyPublisher<T> extends CancelableStreamObserver<T>
        implements Flow.Publisher<T>, Flow.Subscription {

    private boolean canRequest;

    private long requested;

    // whether publisher has been subscribed
    private final AtomicBoolean subscribed = new AtomicBoolean();

    private volatile Flow.Subscriber<? super T> downstream;

    protected volatile CallStreamObserver<?> subscription;

    private final AtomicBoolean hasSub = new AtomicBoolean();

    // cancel status
    private volatile boolean cancelled;

    // complete status
    private volatile boolean done;

    // to help bind TripleSubscriber
    private volatile Consumer<CallStreamObserver<?>> onSubscribe;

    private volatile Runnable shutdownHook;

    private final AtomicBoolean calledShutdown = new AtomicBoolean();

    public AbstractTripleMutinyPublisher() {}

    public AbstractTripleMutinyPublisher(Consumer<CallStreamObserver<?>> onSubscribe, Runnable shutdownHook) {
        this.onSubscribe = onSubscribe;
        this.shutdownHook = shutdownHook;
    }

    protected void onSubscribe(CallStreamObserver<?> subscription) {
        if (subscription != null && this.subscription == null && hasSub.compareAndSet(false, true)) {
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
    public void subscribe(Flow.Subscriber<? super T> s) {
        if (s == null) {
            throw new NullPointerException();
        }
        if (subscribed.compareAndSet(false, true)) {
            this.downstream = s;
            s.onSubscribe(this);
            if (cancelled) this.downstream = null;
        }
    }

    @Override
    public void request(long n) {
        synchronized (this) {
            if (subscribed.get() && canRequest) {
                subscription.request(n >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) n);
            } else {
                requested += n;
            }
        }
    }

    @Override
    public void startRequest() {
        synchronized (this) {
            if (!canRequest) {
                canRequest = true;
                long n = requested;
                subscription.request(n >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) n);
            }
        }
    }

    @Override
    public void cancel() {
        if (!cancelled) {
            cancelled = true;
            doShutdown();
        }
    }

    @Override
    public void onNext(T item) {
        if (done || cancelled) {
            return;
        }
        downstream.onNext(item);
    }

    @Override
    public void onError(Throwable t) {
        if (done || cancelled) {
            return;
        }
        done = true;
        downstream.onError(t);
        doShutdown();
    }

    @Override
    public void onCompleted() {
        if (done || cancelled) {
            return;
        }
        done = true;
        downstream.onComplete();
        doShutdown();
    }

    private void doShutdown() {
        Runnable r = shutdownHook;
        // CAS to confirm shutdownHook will be run only once.
        if (r != null && calledShutdown.compareAndSet(false, true)) {
            shutdownHook = null;
            r.run();
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
