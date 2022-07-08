package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class TripleReactorSubscriber<T> implements Subscriber<T> {

    private volatile boolean isDone;

    private volatile boolean isCanceled;

    private volatile CallStreamObserver<T> downstream;

    private volatile Subscription upstream;

    private static final AtomicReferenceFieldUpdater<TripleReactorSubscriber, CallStreamObserver> DOWNSTREAM =
        AtomicReferenceFieldUpdater.newUpdater(TripleReactorSubscriber.class, CallStreamObserver.class, "downstream");

    private static final AtomicReferenceFieldUpdater<TripleReactorSubscriber, Subscription> UPSTREAM =
        AtomicReferenceFieldUpdater.newUpdater(TripleReactorSubscriber.class, Subscription.class, "upstream");

    public void subscribe(final CallStreamObserver<T> downstream) {
        if (downstream == null) {
            throw new NullPointerException();
        }
        if (this.downstream == null && DOWNSTREAM.compareAndSet(this, null, downstream)) {
            return;
        }

        throw new IllegalStateException(getClass().getSimpleName() + " does not support multiple subscribers");
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (this.upstream == null && UPSTREAM.compareAndSet(this, null, upstream)) {
            upstream = subscription;
        }
    }

    @Override
    public void onNext(T t) {
        if (!isCanceled && !isDone) {
            downstream.onNext(t);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        isCanceled = true;
        downstream.onError(throwable);
    }

    @Override
    public void onComplete() {
        isDone = true;
        downstream.onCompleted();
    }
}
