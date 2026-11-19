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
package org.apache.dubbo.rpc.protocol.tri.h12.grpc;

import org.apache.dubbo.remoting.http12.message.StreamingDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrpcStreamingDecoderTest {

    @Test
    void continueDecodingWithBufferedPartialFrameAfterListenerSwitch() {
        GrpcStreamingDecoder decoder = new GrpcStreamingDecoder();
        List<String> messages = new ArrayList<>();

        StreamingDecoder.FragmentListener normalListener = new StreamingDecoder.FragmentListener() {
            @Override
            public void bytesRead(int numBytes) {}

            @Override
            public void onFragmentMessage(InputStream rawMessage, int messageLength) {
                messages.add(readString(rawMessage));
            }
        };
        decoder.setFragmentListener(new StreamingDecoder.FragmentListener() {
            @Override
            public void bytesRead(int numBytes) {}

            @Override
            public void onFragmentMessage(InputStream rawMessage, int messageLength) {
                messages.add(readString(rawMessage));
                decoder.setFragmentListener(normalListener);
            }
        });
        decoder.request(Integer.MAX_VALUE);

        byte[] firstMessage = frame("first");
        byte[] secondMessage = frame("two");
        byte[] firstData = concat(firstMessage, Arrays.copyOf(secondMessage, secondMessage.length - 1));
        byte[] secondData = new byte[] {secondMessage[secondMessage.length - 1]};

        decoder.decode(new ByteArrayInputStream(firstData));
        decoder.decode(new ByteArrayInputStream(secondData));

        assertEquals(Arrays.asList("first", "two"), messages);
    }

    private static byte[] frame(String value) {
        byte[] payload = value.getBytes(StandardCharsets.UTF_8);
        byte[] frame = new byte[5 + payload.length];
        frame[0] = 0;
        frame[1] = (byte) ((payload.length >>> 24) & 0xFF);
        frame[2] = (byte) ((payload.length >>> 16) & 0xFF);
        frame[3] = (byte) ((payload.length >>> 8) & 0xFF);
        frame[4] = (byte) (payload.length & 0xFF);
        System.arraycopy(payload, 0, frame, 5, payload.length);
        return frame;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] data = new byte[first.length + second.length];
        System.arraycopy(first, 0, data, 0, first.length);
        System.arraycopy(second, 0, data, first.length, second.length);
        return data;
    }

    private static String readString(InputStream inputStream) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8];
        int read;
        try {
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }
}
