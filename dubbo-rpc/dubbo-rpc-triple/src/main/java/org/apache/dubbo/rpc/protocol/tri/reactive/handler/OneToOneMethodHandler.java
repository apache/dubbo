package org.apache.dubbo.rpc.protocol.tri.reactive.handler;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.reactive.calls.ReactorServerCalls;
import org.apache.dubbo.rpc.stub.FutureToObserverAdaptor;
import org.apache.dubbo.rpc.stub.StubMethodHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class OneToOneMethodHandler<T, R> implements StubMethodHandler<T, R> {

    private final Function<Mono<T>, Mono<R>> func;

    public OneToOneMethodHandler(Function<Mono<T>, Mono<R>> func) {
        this.func = func;
    }

    @Override
    public CompletableFuture<?> invoke(Object[] arguments) {
        T request = (T) arguments[0];
        CompletableFuture<R> future = new CompletableFuture<>();
        StreamObserver<R> responseObserver = new FutureToObserverAdaptor<>(future);
        ReactorServerCalls.oneToOne(request, responseObserver, func);
        return future;
    }
}
