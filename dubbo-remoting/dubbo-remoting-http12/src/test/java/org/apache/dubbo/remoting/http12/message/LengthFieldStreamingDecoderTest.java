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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
