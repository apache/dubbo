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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LengthFieldStreamingDecoderTest {

    @Test
    void closesInputReceivedAfterStreamClosed() {
        LengthFieldStreamingDecoder decoder = new LengthFieldStreamingDecoder();
        decoder.onStreamClosed();
        CloseTrackingInputStream inputStream = new CloseTrackingInputStream(new byte[] {0});

        decoder.decode(inputStream);

        assertTrue(inputStream.closed);
    }

    @Test
    void closesInputReceivedWhileDecoderClosing() {
        LengthFieldStreamingDecoder decoder = new LengthFieldStreamingDecoder();
        decoder.decode(new ByteArrayInputStream(new byte[] {0}));
        decoder.close();
        CloseTrackingInputStream inputStream = new CloseTrackingInputStream(new byte[] {0});

        decoder.decode(inputStream);

        assertTrue(inputStream.closed);
    }

    @Test
    void deliversEmptyProtobufPayload() {
        LengthFieldStreamingDecoder decoder = new LengthFieldStreamingDecoder(1, 4);
        ProtobufFragmentListener listener = new ProtobufFragmentListener();
        decoder.setFragmentListener(listener);
        decoder.request(1);

        decoder.decode(new ByteArrayInputStream(new byte[] {0, 0, 0, 0, 0}));

        assertEquals(Arrays.asList(""), listener.values);
        assertEquals(Arrays.asList(0), listener.lengths);
        assertEquals(Arrays.asList(5), listener.bytesRead);
    }

    @Test
    void emptyPayloadsConsumeOneRequestWithoutReadingAdjacentFrames() {
        LengthFieldStreamingDecoder decoder = new LengthFieldStreamingDecoder(1, 4);
        ProtobufFragmentListener listener = new ProtobufFragmentListener();
        decoder.setFragmentListener(listener);
        decoder.decode(new ByteArrayInputStream(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 10, 1, 'a', 0, 0, 0, 0, 0}));
        assertTrue(listener.values.isEmpty());
        assertTrue(listener.bytesRead.isEmpty());

        decoder.request(1);
        assertEquals(Arrays.asList(""), listener.values);
        assertEquals(Arrays.asList(5), listener.bytesRead);

        decoder.request(1);
        assertEquals(Arrays.asList("", "a"), listener.values);
        assertEquals(Arrays.asList(5, 8), listener.bytesRead);

        decoder.request(1);
        assertEquals(Arrays.asList("", "a", ""), listener.values);
        assertEquals(Arrays.asList(0, 3, 0), listener.lengths);
        assertEquals(Arrays.asList(5, 8, 5), listener.bytesRead);
        decoder.request(1);
        assertEquals(3, listener.values.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void deliversEmptyPayloadAcrossInputBoundaries(int split) {
        LengthFieldStreamingDecoder decoder = new LengthFieldStreamingDecoder(1, 4);
        ProtobufFragmentListener listener = new ProtobufFragmentListener();
        decoder.setFragmentListener(listener);
        decoder.request(2);
        byte[] frames = {0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 10, 1, 'a'};

        decoder.decode(new ByteArrayInputStream(Arrays.copyOfRange(frames, 0, split)));
        assertEquals(split == 5 ? 1 : 0, listener.values.size());
        decoder.decode(new ByteArrayInputStream(Arrays.copyOfRange(frames, split, frames.length)));

        assertEquals(Arrays.asList("", "a"), listener.values);
        assertEquals(Arrays.asList(0, 3), listener.lengths);
        assertEquals(Arrays.asList(5, 8), listener.bytesRead);
    }

    private static final class ProtobufFragmentListener implements StreamingDecoder.FragmentListener {

        private final List<String> values = new ArrayList<>();
        private final List<Integer> lengths = new ArrayList<>();
        private final List<Integer> bytesRead = new ArrayList<>();

        @Override
        public void bytesRead(int numBytes) {
            bytesRead.add(numBytes);
        }

        @Override
        public void onFragmentMessage(InputStream rawMessage, int messageLength) {
            try {
                values.add(StringValue.parseFrom(rawMessage).getValue());
                lengths.add(messageLength);
                assertEquals(0, rawMessage.available());
                assertEquals(-1, rawMessage.read());
                assertEquals(-1, rawMessage.read(new byte[1]));
            } catch (IOException e) {
                fail(e);
            }
        }
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private CloseTrackingInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }
    }
}
