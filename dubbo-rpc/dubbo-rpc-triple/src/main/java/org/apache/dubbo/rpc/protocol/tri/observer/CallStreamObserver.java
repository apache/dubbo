package org.apache.dubbo.rpc.protocol.tri.observer;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;

public interface CallStreamObserver<T> extends StreamObserver<T> {


    /**
     * Requests the peer to produce {@code count} more messages to be delivered to the 'inbound'
     * {@link StreamObserver}.
     *
     * <p>This method is safe to call from multiple threads without external synchronization.
     *
     * @param count more messages
     */
    void request(int count);

    /**
     * Sets the compression algorithm to use for the call
     * <p>
     * For stream set compression needs to determine whether the metadata has been sent, and carry on corresponding processing
     *
     * @param compression {@link Compressor}
     */
    void setCompression(String compression);


}
