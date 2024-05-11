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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class LengthFieldStreamingDecoder extends AbstractStreamingDecoder {

    private DecodeState state = DecodeState.HEADER;

    private final int lengthFieldOffset;

    private final int lengthFieldLength;

    private int requiredLength;

    private InputStream dataHeader = new ByteArrayInputStream(new byte[0]);

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
    protected void processMessage() throws IOException {
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

    @Override
    protected boolean hasEnoughBytes() {
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
