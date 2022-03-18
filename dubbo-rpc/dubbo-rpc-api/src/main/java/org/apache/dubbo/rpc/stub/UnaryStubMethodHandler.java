package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.stream.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class UnaryStubMethodHandler<T, R> implements StubMethodHandler<T, R> {
    private final BiConsumer<T, StreamObserver<R>> func;

    public UnaryStubMethodHandler(BiConsumer<T, StreamObserver<R>> func) {
        this.func = func;
    }

    @Override
    public CompletableFuture<?> invoke(Object[] arguments) {
        T request = (T) arguments[0];
        CompletableFuture<R> future = new CompletableFuture<>();
        StreamObserver<R> responseObserver = new FutureToObserverAdaptor<>(future);
        func.accept(request, responseObserver);
        return future;
    }
}

