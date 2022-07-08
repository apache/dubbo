package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.ClientStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class TripleReactorPublisher<T> implements Publisher<T>, StreamObserver<T>, Subscription {

    private static final Subscription EMPTY_SUBSCRIPTION = new Subscription() {
        @Override
        public void cancel() {
        }

        @Override
        public void request(long n) {
        }
    };

    private volatile int state;

    private static final int UNSUBSCRIBED = 0;

    private static final int SUBSCRIBED = 1;

    private volatile Subscriber<? super T> downstream;

    protected volatile CallStreamObserver<?> upstream;

    private volatile boolean cancelled;

    private volatile boolean done;

    private volatile Runnable shutdownHook;

    private static final AtomicReferenceFieldUpdater<TripleReactorPublisher, Runnable> ON_SHUTDOWN =
        AtomicReferenceFieldUpdater.newUpdater(TripleReactorPublisher.class, Runnable.class, "shutdownHook");

    private static final AtomicIntegerFieldUpdater<TripleReactorPublisher> STATE =
        AtomicIntegerFieldUpdater.newUpdater(TripleReactorPublisher.class, "state");

    public TripleReactorPublisher(final ClientStreamObserver<?> upstream) {
        this(upstream, null);
    }

    public TripleReactorPublisher(final ClientStreamObserver<?> upstream, Runnable shutdownHook) {
        this.upstream = upstream;
        this.shutdownHook = shutdownHook;
        upstream.disableAutoRequest();
    }

    @Override
    public void onNext(T data) {
        if (done || cancelled) {
            return;
        }

        downstream.onNext(data);
    }

    @Override
    public void onError(Throwable throwable) {
        if (done || cancelled) {
            return;
        }
        downstream.onError(throwable);
        done = true;
        doPostShutdown();
    }

    @Override
    public void onCompleted() {
        if (done || cancelled) {
            return;
        }
        done = true;
        doPostShutdown();
    }

    private void doPostShutdown() {
        Runnable r = shutdownHook;
        // CAS to confirm shutdownHook will be run only once.
        if (r != null && ON_SHUTDOWN.compareAndSet(this, r, null)) {
            r.run();
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException();
        }

        if (state == UNSUBSCRIBED && STATE.compareAndSet(this, UNSUBSCRIBED, SUBSCRIBED)) {
            subscriber.onSubscribe(this);
            this.downstream = subscriber;
            if (cancelled) {
                this.downstream = null;
            }
        } else {
            subscriber.onSubscribe(EMPTY_SUBSCRIPTION);
            subscriber.onError(new IllegalStateException(getClass().getSimpleName() + " allows only a single Subscriber"));
        }
    }

    @Override
    public void request(long l) {
        if (l > 0 && l <= Integer.MAX_VALUE) {
            upstream.request((int) l);
        }
    }

    @Override
    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        doPostShutdown();
    }
}
