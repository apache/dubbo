package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class ReactorServerCalls {
    private ReactorServerCalls() {
    }

    /**
     * Mono -> Flux
     *
     * TODO stub (in MethodHandlers) call this
     *
     * The data stream: response -> subscriber -> responseObserver
     *
     * @param request request
     * @param responseObserver responseObserver
     * @param func service impl
     */
    public static <T, R> void oneToMany (T request,
                                         StreamObserver<R> responseObserver,
                                         Function<Mono<T>, Flux<R>> func) {
        try {
            Flux<R> response = func.apply(Mono.just(request));
            TripleReactorSubscriber<R> subscriber = response.subscribeWith(new TripleReactorSubscriber<>());
            subscriber.subscribe((ServerCallToObserverAdapter<R>) responseObserver);
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }
    }
}
