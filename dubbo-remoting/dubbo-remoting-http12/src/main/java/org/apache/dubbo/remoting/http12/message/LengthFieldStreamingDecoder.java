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

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.remoting.http12.CompositeInputStream;
import org.apache.dubbo.remoting.http12.exception.DecodeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    private int requiredLength;

    private InputStream dataHeader = StreamUtils.EMPTY;

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
        pendingDeliveries += numMessages;
        deliver();
    }

    @Override
    public final void close() {
        closing = true;
        deliver();
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
            if (closing) {
                if (!closed) {
                    closed = true;
                    accumulate.close();
                    listener.onClose();
                }
            }
        } catch (IOException e) {
            throw new DecodeException(e);
        } finally {
            inDelivery = false;
        }
    }

    private void processHeader() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(lengthFieldOffset + lengthFieldLength);
        byte[] offsetData = new byte[lengthFieldOffset];
        int ignore = accumulate.read(offsetData);
        bos.write(offsetData);
        processOffset(new ByteArrayInputStream(offsetData), lengthFieldOffset);
        byte[] lengthBytes = new byte[lengthFieldLength];
        ignore = accumulate.read(lengthBytes);
        bos.write(lengthBytes);
        requiredLength = bytesToInt(lengthBytes);
        this.dataHeader = new ByteArrayInputStream(bos.toByteArray());

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
        byte[] rawMessage = readRawMessage(accumulate, requiredLength);
        InputStream inputStream = new ByteArrayInputStream(rawMessage);
        invokeListener(inputStream);

        // Done with this frame, begin processing the next header.
        state = DecodeState.HEADER;
        requiredLength = lengthFieldOffset + lengthFieldLength;
    }

    protected void invokeListener(InputStream inputStream) {
        this.listener.onFragmentMessage(dataHeader, inputStream);
    }

    protected byte[] readRawMessage(InputStream inputStream, int length) throws IOException {
        byte[] data = new byte[length];
        inputStream.read(data, 0, length);
        return data;
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
