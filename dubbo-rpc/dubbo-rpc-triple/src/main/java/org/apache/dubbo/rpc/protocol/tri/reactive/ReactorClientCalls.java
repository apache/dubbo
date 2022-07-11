package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.stub.StubInvocationUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactorClientCalls {

    private ReactorClientCalls() {
    }

    // TODO oneToOne, ManyToOne, ManyToMany

    /**
     * Mono -> Flux
     *
     */
    public static <TRequest, TResponse, TInvoker> Flux<TResponse> oneToMany(Invoker<TInvoker> invoker,
                                                                            Mono<TRequest> monoRequest,
                                                                            StubMethodDescriptor methodDescriptor) {
        try {
            return monoRequest
                .flatMapMany(request -> {
                    ClientTripleReactorPublisher<TResponse> consumerStreamObserver =
                        new ClientTripleReactorPublisher<>();
                    StubInvocationUtil.serverStreamCall(invoker, methodDescriptor , request, consumerStreamObserver);
                    return consumerStreamObserver;
                });
        } catch (Throwable throwable) {
            return Flux.error(throwable);
        }
    }
}
