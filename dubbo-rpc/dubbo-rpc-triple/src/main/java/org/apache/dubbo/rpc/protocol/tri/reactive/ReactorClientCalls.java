package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.common.stream.StreamObserver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiConsumer;

public class ReactorClientCalls {

    private ReactorClientCalls() {
    }

    /**
     * Mono -> Flux
     *
     * TODO stub call this to bind responseStream to TripleReactorPublisher
     * @param monoSource request mono
     * @param delegate the function of call remote method
     * @return a flux contains StreamObserver
     */
    public static <TRequest, TResponse> Flux<TResponse> oneToMany(Mono<TRequest> monoSource,
                                                                  BiConsumer<TRequest, StreamObserver<TResponse>> delegate) {
        try {
            return monoSource
                .flatMapMany(request -> {
                    TripleReactorPublisher<TResponse> consumerStreamObserver =
                        new TripleReactorPublisher<>(null, null);
                    delegate.accept(request, consumerStreamObserver);
                    return consumerStreamObserver;
                });
        } catch (Throwable throwable) {
            return Flux.error(throwable);
        }
    }
}
