package org.apache.dubbo.rpc.protocol.tri.reactive.handler;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.reactive.calls.ReactorServerCalls;
import org.apache.dubbo.rpc.stub.StubMethodHandler;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ManyToManyMethodHandler<T, R> implements StubMethodHandler<T, R> {

    private final Function<Flux<T>, Flux<R>> func;

    public ManyToManyMethodHandler(Function<Flux<T>, Flux<R>> func) {
        this.func = func;
    }

    // 返回的是requestObserver，一返回就会被监听，然后请求会往里面打
    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<?> invoke(Object[] arguments) {
        CallStreamObserver<R> responseObserver = (CallStreamObserver<R>) arguments[0];
        StreamObserver<T> requestObserver = ReactorServerCalls.manyToMany(responseObserver, func);
        return CompletableFuture.completedFuture(requestObserver);
    }
}
