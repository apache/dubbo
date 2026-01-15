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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The middle layer between {@link CallStreamObserver} and Reactive API. <br>
 * Passing the data from Reactive producer to CallStreamObserver.
 */
public abstract class AbstractTripleMutinySubscriber<T> implements Flow.Subscriber<T> {

    private volatile boolean cancelled;

    protected volatile CallStreamObserver<T> downstream;

    private final AtomicBoolean subscribed = new AtomicBoolean();

    private final AtomicBoolean hasSubscribed = new AtomicBoolean();

    private volatile Flow.Subscription subscription;

    // complete status
    private volatile boolean done;

    /**
     * Binding the downstream, and call subscription#request(1).
     *
     * @param downstream downstream
     */
    public void subscribe(CallStreamObserver<T> downstream) {
        if (downstream == null) {
            throw new NullPointerException();
        }
        if (subscribed.compareAndSet(false, true)) {
            this.downstream = downstream;
            if (subscription != null) subscription.request(1);
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription sub) {
        if (this.subscription == null && hasSubscribed.compareAndSet(false, true)) {
            this.subscription = sub;
            return;
        }
        sub.cancel();
    }

    @Override
    public void onNext(T item) {
        if (!done && !cancelled) {
            downstream.onNext(item);
            subscription.request(1);
        }
    }

    @Override
    public void onError(Throwable t) {
        if (!cancelled) {
            done = true;
            downstream.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (!cancelled) {
            done = true;
            downstream.onCompleted();
        }
    }

    public void cancel() {
        if (!cancelled && subscription != null) {
            cancelled = true;
            subscription.cancel();
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
