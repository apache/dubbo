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
package org.apache.dubbo.remoting.websocket;

import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalFragmentStreamingDecoderTest {

    @Test
    void closesInputReceivedAfterStreamClosed() {
        FinalFragmentStreamingDecoder decoder = new FinalFragmentStreamingDecoder();
        decoder.onStreamClosed();
        CloseTrackingInputStream inputStream = new CloseTrackingInputStream(new byte[] {0});

        decoder.decode(inputStream);

        assertTrue(inputStream.closed);
    }

    @Test
    void closesInputReceivedAfterClose() {
        FinalFragmentStreamingDecoder decoder = new FinalFragmentStreamingDecoder();
        decoder.setFragmentListener(new StreamingDecoder.FragmentListener() {
            @Override
            public void bytesRead(int numBytes) {}

            @Override
            public void onFragmentMessage(InputStream rawMessage, int messageLength) {}
        });
        decoder.close();
        CloseTrackingInputStream inputStream = new CloseTrackingInputStream(new byte[] {0});

        decoder.decode(inputStream);

        assertTrue(inputStream.closed);
    }

    @Test
    void propagatesCloseFailureAsDecodeException() {
        FinalFragmentStreamingDecoder decoder = new FinalFragmentStreamingDecoder();
        decoder.onStreamClosed();
        IOExceptionCloseInputStream inputStream = new IOExceptionCloseInputStream(new byte[] {0});

        DecodeException ex = assertThrows(DecodeException.class, () -> decoder.decode(inputStream));
        assertEquals(IOException.class, ex.getCause().getClass());
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

    private static final class IOExceptionCloseInputStream extends ByteArrayInputStream {

        private IOExceptionCloseInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            throw new IOException("simulated close failure");
        }
    }
}
