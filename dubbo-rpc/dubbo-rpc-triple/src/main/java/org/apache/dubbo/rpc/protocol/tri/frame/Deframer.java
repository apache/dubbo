package org.apache.dubbo.rpc.protocol.tri.frame;

import io.netty.buffer.ByteBuf;

public interface Deframer {

    /**
     * Adds the given data to this deframer and attempts delivery to the listener.
     *
     * @param data the raw data read from the remote endpoint. Must be non-null.
     */
    void deframe(ByteBuf data);

    /**
     * Requests up to the given number of messages from the call. No additional messages will be
     * delivered.
     *
     * <p>If {@link #close()} has been called, this method will have no effect.
     *
     * @param numMessages the requested number of messages to be delivered to the listener.
     */
    void request(int numMessages);

    /**
     * Closes this deframer and frees any resources. After this method is called, additional calls
     * will have no effect.
     */
    void close();
}
