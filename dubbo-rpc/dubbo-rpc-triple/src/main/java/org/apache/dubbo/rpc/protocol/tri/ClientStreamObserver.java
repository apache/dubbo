package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.stream.StreamObserver;

public interface ClientStreamObserver<T> extends StreamObserver<T> {

    /**
     * Requests the peer to produce {@code count} more messages to be delivered to the 'inbound'
     * {@link StreamObserver}.
     *
     * @param count more messages
     */
    default void request(int count) {
        // todo support
    }

    /**
     * Swaps to manual flow control where no message will be delivered to {@link
     * StreamObserver#onNext(Object)} unless it is {@link #request request()}ed.
     */
    default void disableAutoRequestWithInitial(int request) {
        //  todo support
    }

    /**
     * Sets the compression algorithm to use for the call
     *
     * @param compression {@link Compressor}
     */
    void setCompression(String compression);


}
