package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;

public class ClientTripleReactorSubscriber<T> extends AbstractTripleReactorSubscriber<T> {

    @Override
    public void terminate() {
        if (!isTerminated()) {
            super.terminate();
            ((ClientCallToObserverAdapter<T>) downstream).cancel(new Exception("Cancelled"));
        }
    }
}
