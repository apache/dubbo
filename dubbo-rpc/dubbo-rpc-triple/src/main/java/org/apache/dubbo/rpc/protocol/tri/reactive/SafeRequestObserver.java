package org.apache.dubbo.rpc.protocol.tri.reactive;

import org.apache.dubbo.common.stream.StreamObserver;

public interface SafeRequestObserver<T> extends StreamObserver<T> {

    /**
     * In {@code startRequest}, {@link org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver#request(int)} of {@link org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver} can be used safely,
     * because the lower-level call has already started.
     */
    void startRequest();
}
