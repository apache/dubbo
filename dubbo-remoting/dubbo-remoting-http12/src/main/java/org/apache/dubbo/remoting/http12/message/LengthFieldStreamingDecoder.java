/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.remoting.http12.message;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.remoting.http12.CompositeInputStream;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LengthFieldStreamingDecoder implements StreamingDecoder {

    private long pendingDeliveries;

    private boolean inDelivery = false;

    private boolean closing;

    private boolean closed;

    private DecodeState state = DecodeState.HEADER;

    private final CompositeInputStream accumulate = new CompositeInputStream();

    private FragmentListener listener;

    private final int lengthFieldOffset;

    private final int lengthFieldLength;

    private final int maxMessageSize;

    private int requiredLength;

    public LengthFieldStreamingDecoder() {
        this(4);
    }

    public LengthFieldStreamingDecoder(int lengthFieldLength) {
        this(0, lengthFieldLength);
    }

    public LengthFieldStreamingDecoder(int lengthFieldOffset, int lengthFieldLength) {
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.requiredLength = lengthFieldOffset + lengthFieldLength;
        Configuration conf = ConfigurationUtils.getEnvConfiguration(ApplicationModel.defaultModel());
        this.maxMessageSize = conf.getInt(Constants.H2_SETTINGS_MAX_MESSAGE_SIZE, 50 * 1024 * 1024);
    }

    @Override
    public final void decode(InputStream inputStream) throws DecodeException {
        if (closing || closed) {
            // ignored
            return;
        }
        accumulate.addInputStream(inputStream);
        deliver();
    }

    @Override
    public final void request(int numMessages) {
        if (isClosed()) {
            return;
        }
        pendingDeliveries += numMessages;
        deliver();
    }

    /**
     * Marks the decoder for closing. The decoder will actually close when all
     * requested messages have been delivered and no more data is available (stalled).
     */
    @Override
    public final void close() {
        if (isClosed()) {
            return;
        }
        if (isStalled()) {
            // No more data available, close immediately
            doClose();
            return;
        }
        // Mark for closing, will close when stalled
        closing = true;
        deliver();
    }

    /**
     * Actually close the decoder and notify the listener.
     */
    private void doClose() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            accumulate.close();
        } catch (IOException e) {
            // ignore
        }
        if (listener != null) {
            listener.onClose();
        }
    }

    /**
     * Returns true if the decoder has been closed.
     */
    private boolean isClosed() {
        return closed;
    }

    @Override
    public final void onStreamClosed() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            accumulate.close();
        } catch (IOException e) {
            throw new DecodeException(e);
        }
    }

    @Override
    public final void setFragmentListener(FragmentListener listener) {
        this.listener = listener;
    }

    private void deliver() {
        // We can have reentrancy here when using a direct executor, triggered by calls to
        // request more messages. This is safe as we simply loop until pendingDelivers = 0
        if (inDelivery) {
            return;
        }
        if (closed) {
            return;
        }
        inDelivery = true;
        try {
            // Process the uncompressed bytes.
            while (pendingDeliveries > 0 && hasEnoughBytes()) {
                switch (state) {
                    case HEADER:
                        processHeader();
                        break;
                    case PAYLOAD:
                        // Read the body and deliver the message.
                        processBody();

                        // Since we've delivered a message, decrement the number of pending
                        // deliveries remaining.
                        pendingDeliveries--;
                        break;
                    default:
                        throw new AssertionError("Invalid state: " + state);
                }
            }
            // only close when stalled (no more data available).
            // This ensures that when disableAutoRequest() is used and more messages are
            // still buffered, the stream won't close prematurely. The application needs
            // to call request() to receive remaining messages.
            if (closing && isStalled()) {
                doClose();
            }
        } catch (IOException e) {
            throw new DecodeException(e);
        } finally {
            inDelivery = false;
        }
    }

    /**
     * Returns true if there's no more data available to process.
     */
    private boolean isStalled() {
        return accumulate.available() == 0;
    }

    private void processHeader() throws IOException {
        byte[] offsetData = new byte[lengthFieldOffset];
        int ignore = accumulate.read(offsetData);
        processOffset(new ByteArrayInputStream(offsetData), lengthFieldOffset);
        byte[] lengthBytes = new byte[lengthFieldLength];
        ignore = accumulate.read(lengthBytes);
        requiredLength = bytesToInt(lengthBytes);

        // Validate bounds
        if (requiredLength < 0) {
            throw new RpcException("Invalid message length: " + requiredLength);
        }
        if (requiredLength > maxMessageSize) {
            throw new RpcException(String.format("Message size %d exceeds limit %d", requiredLength, maxMessageSize));
        }

        // Continue reading the frame body.
        state = DecodeState.PAYLOAD;
    }

    protected void processOffset(InputStream inputStream, int lengthFieldOffset) throws IOException {
        // default skip offset
        skipOffset(inputStream, lengthFieldOffset);
    }

    private void skipOffset(InputStream inputStream, int lengthFieldOffset) throws IOException {
        if (lengthFieldOffset != 0) {
            return;
        }
        int ignore = inputStream.read(new byte[lengthFieldOffset]);
    }

    private void processBody() throws IOException {
        // Calculate total bytes read: header (offset + length field) + payload
        int totalBytesRead = lengthFieldOffset + lengthFieldLength + requiredLength;

        MessageStream messageStream;
        try {
            messageStream = readMessageStream(accumulate, requiredLength);
        } finally {
            // Notify listener about bytes read for flow control immediately after reading bytes
            // This must be in finally block to ensure flow control works even if reading fails
            // Following gRPC's pattern: bytesRead is called as soon as bytes are consumed from input
            listener.bytesRead(totalBytesRead);
        }

        invokeListener(messageStream.inputStream, messageStream.length);

        // Done with this frame, begin processing the next header.
        state = DecodeState.HEADER;
        requiredLength = lengthFieldOffset + lengthFieldLength;
    }

    public void invokeListener(InputStream inputStream, int messageLength) {
        this.listener.onFragmentMessage(inputStream, messageLength);
    }

    /**
     * Read message from the input stream and return it as a MessageStream.
     */
    protected MessageStream readMessageStream(InputStream inputStream, int length) throws IOException {
        InputStream boundedStream = new BoundedInputStream(inputStream, length);
        return new MessageStream(boundedStream, length);
    }

    protected byte[] readRawMessage(InputStream inputStream, int length) throws IOException {
        byte[] data = new byte[length];
        inputStream.read(data, 0, length);
        return data;
    }

    protected static class MessageStream {

        public final InputStream inputStream;
        public final int length;

        public MessageStream(InputStream inputStream, int length) {
            this.inputStream = inputStream;
            this.length = length;
        }
    }

    /**
     * A bounded InputStream that reads at most 'limit' bytes from the source stream.
     * Extends BufferedInputStream to support mark/reset, which is required by
     * deserializers like Hessian2.
     */
    private static class BoundedInputStream extends BufferedInputStream {

        private final int limit;
        private int remaining;
        private int markedRemaining;

        public BoundedInputStream(InputStream source, int limit) {
            super(source, limit);
            this.limit = limit;
            this.remaining = limit;
            this.markedRemaining = limit;
        }

        @Override
        public synchronized int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int result = super.read();
            if (result != -1) {
                remaining--;
            }
            return result;
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int toRead = Math.min(len, remaining);
            int result = super.read(b, off, toRead);
            if (result > 0) {
                remaining -= result;
            }
            return result;
        }

        @Override
        public synchronized int available() throws IOException {
            return Math.min(super.available(), remaining);
        }

        @Override
        public synchronized void mark(int readlimit) {
            // Force readlimit to be at least the remaining message length.
            // This ensures mark is always valid within the bounded stream,
            // regardless of what readlimit is passed by the deserializer (e.g., Hessian2).
            super.mark(Math.max(readlimit, limit));
            markedRemaining = remaining;
        }

        @Override
        public synchronized void reset() throws IOException {
            super.reset();
            // Restore the remaining count to the value at mark time
            remaining = markedRemaining;
        }
    }

    private boolean hasEnoughBytes() {
        return requiredLength - accumulate.available() <= 0;
    }

    protected static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3]) & 0xFF;
    }

    private enum DecodeState {
        HEADER,
        PAYLOAD
    }
}
