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
package org.apache.dubbo.rpc.protocol.tri.stream;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for byte counting backpressure mechanism in AbstractTripleClientStream.
 * This test class mirrors the logic used in AbstractTripleClientStream to verify
 * the correctness of the byte counting mechanism.
 */
@Timeout(10)
class AbstractTripleClientStreamByteCountingTest {

    private static final long ON_READY_THRESHOLD = 32 * 1024; // 32KB

    /**
     * Test isReady returns true when below threshold.
     */
    @Test
    void testIsReadyWhenBelowThreshold() {
        ClientStreamByteCounter counter = new ClientStreamByteCounter();

        assertTrue(counter.isReady());

        counter.onSendingBytes(1000);
        assertTrue(counter.isReady());

        counter.onSendingBytes((int) ON_READY_THRESHOLD - 1001);
        assertTrue(counter.isReady());
    }

    /**
     * Test isReady returns false when at or above threshold.
     */
    @Test
    void testIsReadyWhenAtOrAboveThreshold() {
        ClientStreamByteCounter counter = new ClientStreamByteCounter();

        counter.onSendingBytes((int) ON_READY_THRESHOLD);
        assertFalse(counter.isReady());

        counter.onSendingBytes(1000);
        assertFalse(counter.isReady());
    }

    /**
     * Test onReady is triggered when transitioning from not-ready to ready.
     */
    @Test
    void testOnReadyTriggeredOnTransition() {
        ClientStreamByteCounter counter = new ClientStreamByteCounter();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        counter.setOnReadyCallback(onReadyCount::incrementAndGet);

        // Send bytes to exceed threshold
        counter.onSendingBytes((int) ON_READY_THRESHOLD + 1000);
        assertFalse(counter.isReady());
        assertEquals(0, onReadyCount.get());

        // Complete sending - should trigger onReady when crossing threshold
        counter.onSentBytes((int) ON_READY_THRESHOLD + 1000);
        assertTrue(counter.isReady());
        assertEquals(1, onReadyCount.get());
    }

    /**
     * Test onReady is NOT triggered when staying below threshold.
     */
    @Test
    void testOnReadyNotTriggeredWhenStayingBelowThreshold() {
        ClientStreamByteCounter counter = new ClientStreamByteCounter();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        counter.setOnReadyCallback(onReadyCount::incrementAndGet);

        // Send small amount
        counter.onSendingBytes(1000);
        counter.onSentBytes(1000);
        assertEquals(0, onReadyCount.get());

        // Send another small amount
        counter.onSendingBytes(2000);
        counter.onSentBytes(2000);
        assertEquals(0, onReadyCount.get());
    }

    /**
     * Test multiple transitions trigger onReady each time.
     */
    @Test
    void testMultipleTransitions() {
        ClientStreamByteCounter counter = new ClientStreamByteCounter();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        counter.setOnReadyCallback(onReadyCount::incrementAndGet);

        // First cycle
        counter.onSendingBytes((int) ON_READY_THRESHOLD + 1000);
        counter.onSentBytes((int) ON_READY_THRESHOLD + 1000);
        assertEquals(1, onReadyCount.get());

        // Second cycle
        counter.onSendingBytes((int) ON_READY_THRESHOLD + 2000);
        counter.onSentBytes((int) ON_READY_THRESHOLD + 2000);
        assertEquals(2, onReadyCount.get());

        // Third cycle
        counter.onSendingBytes((int) ON_READY_THRESHOLD + 3000);
        counter.onSentBytes((int) ON_READY_THRESHOLD + 3000);
        assertEquals(3, onReadyCount.get());
    }

    /**
     * Test concurrent sends only trigger onReady once for single transition.
     */
    @Test
    void testConcurrentSendsOnlyTriggerOnReadyOnce() throws InterruptedException {
        ClientStreamByteCounter counter = new ClientStreamByteCounter();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        counter.setOnReadyCallback(onReadyCount::incrementAndGet);

        // Exceed threshold
        counter.onSendingBytes((int) ON_READY_THRESHOLD + 10000);

        // Simulate concurrent completions
        int threadCount = 10;
        int bytesPerThread = ((int) ON_READY_THRESHOLD + 10000) / threadCount;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    counter.onSentBytes(bytesPerThread);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Only one thread should trigger onReady
        assertEquals(1, onReadyCount.get());
        assertTrue(counter.isReady());
    }

    /**
     * Test initial state is ready.
     */
    @Test
    void testInitialStateIsReady() {
        ClientStreamByteCounter counter = new ClientStreamByteCounter();
        assertTrue(counter.isReady());
        assertEquals(0, counter.getNumSentBytesQueued());
    }

    /**
     * Test rollback does not trigger onReady.
     */
    @Test
    void testRollbackDoesNotTriggerOnReady() {
        ClientStreamByteCounter counter = new ClientStreamByteCounter();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        counter.setOnReadyCallback(onReadyCount::incrementAndGet);

        // Exceed threshold
        counter.onSendingBytes((int) ON_READY_THRESHOLD + 1000);

        // Rollback (simulating send failure)
        counter.rollbackSendingBytes((int) ON_READY_THRESHOLD + 1000);

        // Should not trigger onReady
        assertTrue(counter.isReady());
        assertEquals(0, onReadyCount.get());
    }

    /**
     * Test exact threshold boundary.
     */
    @Test
    void testExactThresholdBoundary() {
        ClientStreamByteCounter counter = new ClientStreamByteCounter();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        counter.setOnReadyCallback(onReadyCount::incrementAndGet);

        // At exactly threshold - not ready
        counter.onSendingBytes((int) ON_READY_THRESHOLD);
        assertFalse(counter.isReady());

        // Send 1 byte to go below threshold
        counter.onSentBytes(1);
        assertTrue(counter.isReady());
        assertEquals(1, onReadyCount.get());
    }

    /**
     * Simulates the byte counting logic from AbstractTripleClientStream for testing.
     */
    private static class ClientStreamByteCounter {
        private final AtomicLong numSentBytesQueued = new AtomicLong(0);
        private Runnable onReadyCallback;

        public boolean isReady() {
            return numSentBytesQueued.get() < ON_READY_THRESHOLD;
        }

        public void setOnReadyCallback(Runnable callback) {
            this.onReadyCallback = callback;
        }

        public void onSendingBytes(int numBytes) {
            numSentBytesQueued.addAndGet(numBytes);
        }

        public void rollbackSendingBytes(int numBytes) {
            numSentBytesQueued.addAndGet(-numBytes);
        }

        public void onSentBytes(int numBytes) {
            long oldValue = numSentBytesQueued.getAndAdd(-numBytes);
            long newValue = oldValue - numBytes;
            if (oldValue >= ON_READY_THRESHOLD && newValue < ON_READY_THRESHOLD) {
                if (onReadyCallback != null) {
                    onReadyCallback.run();
                }
            }
        }

        public long getNumSentBytesQueued() {
            return numSentBytesQueued.get();
        }
    }
}
