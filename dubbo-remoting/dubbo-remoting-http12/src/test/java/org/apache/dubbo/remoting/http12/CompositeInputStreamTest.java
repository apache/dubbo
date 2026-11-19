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
package org.apache.dubbo.remoting.http12;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CompositeInputStreamTest {

    @Test
    void closeClosesRemainingStreamsWhenOneCloseFails() {
        CompositeInputStream compositeInputStream = new CompositeInputStream();
        CloseTrackingInputStream first = new CloseTrackingInputStream(new IOException("first"));
        CloseTrackingInputStream second = new CloseTrackingInputStream(null);

        compositeInputStream.addInputStream(first);
        compositeInputStream.addInputStream(second);

        IOException thrown = Assertions.assertThrows(IOException.class, compositeInputStream::close);
        Assertions.assertSame(first.closeException, thrown);
        Assertions.assertTrue(first.closed);
        Assertions.assertTrue(second.closed);
    }

    @Test
    void closeAddsLaterCloseFailuresAsSuppressedExceptions() {
        CompositeInputStream compositeInputStream = new CompositeInputStream();
        CloseTrackingInputStream first = new CloseTrackingInputStream(new IOException("first"));
        CloseTrackingInputStream second = new CloseTrackingInputStream(new IOException("second"));

        compositeInputStream.addInputStream(first);
        compositeInputStream.addInputStream(second);

        IOException thrown = Assertions.assertThrows(IOException.class, compositeInputStream::close);
        Assertions.assertSame(first.closeException, thrown);
        Assertions.assertArrayEquals(new Throwable[] {second.closeException}, thrown.getSuppressed());
    }

    private static final class CloseTrackingInputStream extends InputStream {
        private final IOException closeException;
        private boolean closed;

        private CloseTrackingInputStream(IOException closeException) {
            this.closeException = closeException;
        }

        @Override
        public int read() {
            return -1;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            if (closeException != null) {
                throw closeException;
            }
        }
    }
}
