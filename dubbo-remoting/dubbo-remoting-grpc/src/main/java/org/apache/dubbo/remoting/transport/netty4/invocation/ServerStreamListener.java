package org.apache.dubbo.remoting.transport.netty4.invocation;

public interface ServerStreamListener extends StreamInboundListener {
    @Override
    default void onHeader(DataHeader header) {
    }

    @Override
    default boolean isComplete() {
        return true;
    }
}
