package org.apache.dubbo.rpc.protocol.tri.transport;

import io.netty.buffer.ByteBuf;
import io.netty.incubator.codec.http3.Http3Headers;

public interface H3TransportListener {
    /**
     * Transport metadata
     *
     * @param headers   metadata KV paris
     */
    void onHeader(Http3Headers headers);

    /**
     * Transport data
     *
     * @param data      raw byte array
     * @param endStream whether this data should terminate the stream
     */
    void onData(ByteBuf data, boolean endStream);
}
