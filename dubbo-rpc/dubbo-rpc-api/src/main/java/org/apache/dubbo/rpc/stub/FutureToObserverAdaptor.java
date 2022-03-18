package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.stream.StreamObserver;

import java.util.concurrent.CompletableFuture;

public class FutureToObserverAdaptor<T> implements StreamObserver<T> {
    private final CompletableFuture<T> future;

    public FutureToObserverAdaptor(CompletableFuture<T> future) {
        this.future = future;
    }

    @Override
    public void onNext(T data) {
        future.complete(data);
    }

    @Override
    public void onError(Throwable throwable) {
        future.completeExceptionally(throwable);
    }

    @Override
    public void onCompleted() {
    }
}
