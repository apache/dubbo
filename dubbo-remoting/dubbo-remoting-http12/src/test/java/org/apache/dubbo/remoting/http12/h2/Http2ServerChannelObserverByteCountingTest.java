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
package org.apache.dubbo.remoting.http12.h2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for byte counting backpressure mechanism in Http2ServerChannelObserver.
 */
@Timeout(10)
class Http2ServerChannelObserverByteCountingTest {

    /**
     * Test isReady returns true when below threshold.
     */
    @Test
    void testIsReadyWhenBelowThreshold() {
        TestableHttp2ServerChannelObserver observer = createObserver();

        assertTrue(observer.isReady());

        observer.onSendingBytes(1000);
        assertTrue(observer.isReady());

        observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD - 1001);
        assertTrue(observer.isReady());
    }

    /**
     * Test isReady returns false when at or above threshold.
     */
    @Test
    void testIsReadyWhenAtOrAboveThreshold() {
        TestableHttp2ServerChannelObserver observer = createObserver();

        observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD);
        assertFalse(observer.isReady());

        observer.onSendingBytes(1000);
        assertFalse(observer.isReady());
    }

    /**
     * Test onReady is triggered when transitioning from not-ready to ready.
     */
    @Test
    void testOnReadyTriggeredOnTransition() {
        TestableHttp2ServerChannelObserver observer = createObserver();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        observer.setOnReadyHandler(onReadyCount::incrementAndGet);

        // Send bytes to exceed threshold
        observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);
        assertFalse(observer.isReady());
        assertEquals(0, onReadyCount.get());

        // Complete sending - should trigger onReady when crossing threshold
        observer.onSentBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);
        assertTrue(observer.isReady());
        assertEquals(1, onReadyCount.get());
    }

    /**
     * Test onReady is NOT triggered when staying below threshold.
     */
    @Test
    void testOnReadyNotTriggeredWhenStayingBelowThreshold() {
        TestableHttp2ServerChannelObserver observer = createObserver();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        observer.setOnReadyHandler(onReadyCount::incrementAndGet);

        // Send small amount
        observer.onSendingBytes(1000);
        observer.onSentBytes(1000);
        assertEquals(0, onReadyCount.get());

        // Send another small amount
        observer.onSendingBytes(2000);
        observer.onSentBytes(2000);
        assertEquals(0, onReadyCount.get());
    }

    /**
     * Test multiple transitions trigger onReady each time.
     */
    @Test
    void testMultipleTransitions() {
        TestableHttp2ServerChannelObserver observer = createObserver();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        observer.setOnReadyHandler(onReadyCount::incrementAndGet);

        // First cycle
        observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);
        observer.onSentBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);
        assertEquals(1, onReadyCount.get());

        // Second cycle
        observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 2000);
        observer.onSentBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 2000);
        assertEquals(2, onReadyCount.get());

        // Third cycle
        observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 3000);
        observer.onSentBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 3000);
        assertEquals(3, onReadyCount.get());
    }

    /**
     * Test concurrent sends only trigger onReady once for single transition.
     */
    @Test
    void testConcurrentSendsOnlyTriggerOnReadyOnce() throws InterruptedException {
        TestableHttp2ServerChannelObserver observer = createObserver();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        observer.setOnReadyHandler(onReadyCount::incrementAndGet);

        // Exceed threshold
        observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 10000);

        // Simulate concurrent completions
        int threadCount = 10;
        int bytesPerThread = ((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 10000) / threadCount;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    observer.onSentBytes(bytesPerThread);
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
        assertTrue(observer.isReady());
    }

    /**
     * Test initial state is ready.
     */
    @Test
    void testInitialStateIsReady() {
        TestableHttp2ServerChannelObserver observer = createObserver();
        assertTrue(observer.isReady());
        assertEquals(0, observer.getNumSentBytesQueued());
    }

    /**
     * Test rollback does not trigger onReady.
     */
    @Test
    void testRollbackDoesNotTriggerOnReady() {
        TestableHttp2ServerChannelObserver observer = createObserver();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        observer.setOnReadyHandler(onReadyCount::incrementAndGet);

        // Exceed threshold
        observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);

        // Rollback (simulating send failure)
        observer.rollbackSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);

        // Should not trigger onReady
        assertTrue(observer.isReady());
        assertEquals(0, onReadyCount.get());
    }

    /**
     * Test exact threshold boundary.
     */
    @Test
    void testExactThresholdBoundary() {
        TestableHttp2ServerChannelObserver observer = createObserver();
        AtomicInteger onReadyCount = new AtomicInteger(0);
        observer.setOnReadyHandler(onReadyCount::incrementAndGet);

        // At exactly threshold - not ready
        observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD);
        assertFalse(observer.isReady());

        // Send 1 byte to go below threshold
        observer.onSentBytes(1);
        assertTrue(observer.isReady());
        assertEquals(1, onReadyCount.get());
    }

    // ==================== Helper Methods ====================

    private TestableHttp2ServerChannelObserver createObserver() {
        H2StreamChannel mockChannel = mock(H2StreamChannel.class);
        return new TestableHttp2ServerChannelObserver(mockChannel);
    }

    /**
     * Testable subclass that exposes protected methods for testing.
     */
    private static class TestableHttp2ServerChannelObserver extends Http2ServerChannelObserver {

        public TestableHttp2ServerChannelObserver(H2StreamChannel h2StreamChannel) {
            super(h2StreamChannel);
        }

        @Override
        public void onSendingBytes(int numBytes) {
            super.onSendingBytes(numBytes);
        }

        @Override
        public void rollbackSendingBytes(int numBytes) {
            super.rollbackSendingBytes(numBytes);
        }

        @Override
        public void onSentBytes(int numBytes) {
            super.onSentBytes(numBytes);
        }

        @Override
        public long getNumSentBytesQueued() {
            return super.getNumSentBytesQueued();
        }
    }
}
