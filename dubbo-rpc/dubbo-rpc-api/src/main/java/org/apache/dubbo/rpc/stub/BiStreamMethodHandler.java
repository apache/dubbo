package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.stream.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class BiStreamMethodHandler<T, R> implements StubMethodHandler<T, R> {
    private final Function<StreamObserver<R>, StreamObserver<T>> func;

    public BiStreamMethodHandler(Function<StreamObserver<R>, StreamObserver<T>> func) {
        this.func = func;
    }

    @Override
    public CompletableFuture<?> invoke(Object[] arguments) {
        StreamObserver<R> responseObserver = (StreamObserver<R>) arguments[0];
        StreamObserver<T> requestObserver = func.apply(responseObserver);
        return CompletableFuture.completedFuture(requestObserver);
    }
}
