package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.stub.StubMethodHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class OneToManyMethodHandler<T, R> implements StubMethodHandler<T, R> {

    private final Function<Mono<T>, Flux<R>> func;

    public OneToManyMethodHandler(Function<Mono<T>, Flux<R>> func) {
        this.func = func;
    }

    @Override
    public CompletableFuture<?> invoke(Object[] arguments) {
        T request = (T) arguments[0];
        StreamObserver<R> responseObserver = (StreamObserver<R>) arguments[1];
        ReactorServerCalls.oneToMany(request, responseObserver, func);
        return null;
    }
}
