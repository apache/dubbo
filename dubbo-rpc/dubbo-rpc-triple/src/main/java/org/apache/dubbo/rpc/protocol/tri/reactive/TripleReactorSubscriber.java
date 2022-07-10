package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class TripleReactorSubscriber<T> implements Subscriber<T>, CoreSubscriber<T> {

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
            upstream.request(1);
            return;
        }

        throw new IllegalStateException(getClass().getSimpleName() + " does not support multiple subscribers");
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (subscription == null) {
            throw new NullPointerException();
        }
        if (this.upstream == null) {
            UPSTREAM.compareAndSet(this, null, subscription);
        }
    }

    @Override
    public void onNext(T t) {
        System.out.println(t);
        if (!isCanceled && !isDone) {
            downstream.onNext(t);
            upstream.request(1);
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
