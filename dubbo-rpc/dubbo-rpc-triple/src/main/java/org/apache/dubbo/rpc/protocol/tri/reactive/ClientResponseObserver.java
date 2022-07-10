package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;

public interface ClientResponseObserver <T> extends StreamObserver<T> {

    void beforeStart(final ClientCallToObserverAdapter<T> clientCallToObserverAdapter);
}
