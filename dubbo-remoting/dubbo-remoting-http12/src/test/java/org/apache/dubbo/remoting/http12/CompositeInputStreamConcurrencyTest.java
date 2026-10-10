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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * close() racing with an in-progress read. Most cases are simulated deterministically:
 * the underlying stream closes the composite from inside its own read(), which is the
 * same interleaving as a close() arriving on another thread mid-read.
 */
class CompositeInputStreamConcurrencyTest {

    @Test
    void readInterruptedByCloseReportsEndOfStream() throws IOException {
        CompositeInputStream in = new CompositeInputStream();
        in.addInputStream(new CloseDuringReadStream(in, new byte[8], true));

        Assertions.assertEquals(-1, in.read(new byte[8], 0, 8));
    }

    @Test
    void singleByteReadInterruptedByCloseReportsEndOfStream() throws IOException {
        CompositeInputStream in = new CompositeInputStream();
        in.addInputStream(new CloseDuringReadStream(in, new byte[8], true));

        Assertions.assertEquals(-1, in.read());
    }

    @Test
    void alreadyBufferedDataIsStillDeliveredWhenCloseHappensDuringRead() throws IOException {
        byte[] expected = {1, 2, 3, 4, 5, 6, 7, 8};
        CompositeInputStream in = new CompositeInputStream();
        in.addInputStream(new CloseDuringReadStream(in, expected, false));

        byte[] buf = new byte[8];
        Assertions.assertEquals(8, in.read(buf, 0, 8));
        Assertions.assertArrayEquals(expected, buf);
        Assertions.assertEquals(-1, in.read(buf, 0, 8));
    }

    @Test
    void availableReturnsZeroWhileCloseIsInProgress() throws IOException {
        CompositeInputStream in = new CompositeInputStream();
        AtomicInteger seenDuringClose = new AtomicInteger(-1);
        in.addInputStream(new ByteArrayInputStream(new byte[10]) {
            @Override
            public void close() {
                seenDuringClose.set(in.available());
            }
        });

        in.close();

        Assertions.assertEquals(0, seenDuringClose.get());
    }

    @Test
    void closeWaitsForReadInProgress() throws Exception {
        CountDownLatch insideRead = new CountDownLatch(1);
        CountDownLatch finishRead = new CountDownLatch(1);
        CompositeInputStream in = new CompositeInputStream();
        in.addInputStream(new InputStream() {
            @Override
            public int available() {
                return 1;
            }

            @Override
            public int read() {
                insideRead.countDown();
                try {
                    finishRead.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return 42;
            }
        });

        AtomicInteger readResult = new AtomicInteger(-2);
        Thread reader = new Thread(() -> {
            try {
                readResult.set(in.read());
            } catch (IOException e) {
                readResult.set(-3);
            }
        });
        Thread closer = new Thread(() -> {
            try {
                in.close();
            } catch (IOException ignored) {
                // not under test
            }
        });

        reader.start();
        Assertions.assertTrue(insideRead.await(5, TimeUnit.SECONDS));
        closer.start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (closer.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        // close() must be waiting for the in-flight read, not tearing the stream down under it
        Assertions.assertEquals(Thread.State.BLOCKED, closer.getState());

        finishRead.countDown();
        reader.join(5000);
        closer.join(5000);
        Assertions.assertEquals(42, readResult.get());
    }

    /** Closes the composite from inside read(), then either fails or delivers its data. */
    private static final class CloseDuringReadStream extends InputStream {
        private final CompositeInputStream owner;
        private final byte[] data;
        private final boolean failAfterClose;
        private int pos;

        CloseDuringReadStream(CompositeInputStream owner, byte[] data, boolean failAfterClose) {
            this.owner = owner;
            this.data = data;
            this.failAfterClose = failAfterClose;
        }

        @Override
        public int available() {
            return data.length - pos;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n == -1 ? -1 : (one[0] & 0xFF);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            owner.close();
            if (failAfterClose) {
                throw new IOException("Stream already closed");
            }
            int n = Math.min(len, data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }
    }
}
