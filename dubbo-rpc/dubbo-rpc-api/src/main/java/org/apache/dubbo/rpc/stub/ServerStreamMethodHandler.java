package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.stream.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class ServerStreamMethodHandler<T, R> implements StubMethodHandler<T, R> {
    private final BiConsumer<T, StreamObserver<R>> func;

    public ServerStreamMethodHandler(BiConsumer<T, StreamObserver<R>> func) {
        this.func = func;
    }

    @Override
    public CompletableFuture<?> invoke(Object[] arguments) {
        T request = (T) arguments[0];
        StreamObserver<R> responseObserver = (StreamObserver<R>) arguments[1];
        func.accept(request, responseObserver);
        return CompletableFuture.completedFuture(null);
    }
}
