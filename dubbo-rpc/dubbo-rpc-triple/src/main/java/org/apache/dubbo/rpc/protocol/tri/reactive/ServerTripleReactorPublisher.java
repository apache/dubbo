package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;

public class ServerTripleReactorPublisher<T> extends AbstractTripleReactorPublisher<T> {

    public ServerTripleReactorPublisher(CallStreamObserver<?> callStreamObserver) {
        super.onSubscribe(callStreamObserver);
    }
}
