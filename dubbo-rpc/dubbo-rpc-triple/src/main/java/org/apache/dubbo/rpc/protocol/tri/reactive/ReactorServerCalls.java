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
     * @param delegate service impl
     */
    public static <TRequest, TResponse> void oneToMany (TRequest request,
                                                        StreamObserver<TResponse> responseObserver,
                                                        Function<Mono<TRequest>, Flux<TResponse>> delegate) {
        try {
            Flux<TResponse> response = delegate.apply(Mono.just(request));
            TripleReactorSubscriber<TResponse> subscriber = response.subscribeWith(new TripleReactorSubscriber<>());
            subscriber.subscribe((ServerCallToObserverAdapter<TResponse>) responseObserver);
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }
    }
}
