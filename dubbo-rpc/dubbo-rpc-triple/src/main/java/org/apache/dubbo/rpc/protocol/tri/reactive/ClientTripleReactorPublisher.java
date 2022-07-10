package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;

public class ClientTripleReactorPublisher<T> extends AbstractTripleReactorPublisher<T> implements ClientResponseObserver<T> {

    public ClientTripleReactorPublisher() {
    }

    public ClientTripleReactorPublisher(Runnable shutdownHook) {
        super(shutdownHook);
    }

    @Override
    public void beforeStart(ClientCallToObserverAdapter<T> clientCallToObserverAdapter) {
        super.onSubscribe(clientCallToObserverAdapter);
    }
}
