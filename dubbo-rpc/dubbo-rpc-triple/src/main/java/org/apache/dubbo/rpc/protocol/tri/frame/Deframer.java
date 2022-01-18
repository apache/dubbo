package org.apache.dubbo.rpc.protocol.tri.frame;

import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;

import io.netty.buffer.ByteBuf;

public interface Deframer {

    /**
     * Sets the decompressor available to use. The message encoding for the stream comes later in
     * time, and thus will not be available at the time of construction. This should only be set once,
     * since the compression codec cannot change after the headers have been sent.
     *
     * @param decompressor the decompressing wrapper.
     */
    void setDecompressor(DeCompressor decompressor);

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
