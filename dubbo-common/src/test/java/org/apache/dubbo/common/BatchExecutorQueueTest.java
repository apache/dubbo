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
package org.apache.dubbo.common;

import org.apache.dubbo.common.utils.NamedThreadFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BatchExecutorQueueTest {

    private ExecutorService executor;
    private final int poolSize = 8;

    @BeforeEach
    public void init() {
        executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20),
                new NamedThreadFactory("BatchExecutorQueueTest", true));
    }

    @AfterEach
    public void cleanup() {
        executor.shutdown();
    }

    @Test
    public void testEnqueueAndProcess() throws InterruptedException {
        int itemCount = 10000;
        int chunkSize = 256;
        CountDownLatch prepareCount = new CountDownLatch(itemCount);
        AtomicInteger flushCount = new AtomicInteger(0);
        AtomicInteger flushNum = new AtomicInteger(0);
        AtomicInteger flushNumMax = new AtomicInteger(0);

        BatchExecutorQueue<Object> batchQueue = new BatchExecutorQueue<Object>(chunkSize) {
            @Override
            protected void prepare(Object item) {
                prepareCount.countDown();
                flushNum.incrementAndGet();
            }

            @Override
            protected void flush(Object item) {
                prepare(item);
                flushCount.incrementAndGet();
                flushNumMax.set(Math.max(flushNumMax.get(), flushNum.get()));
                flushNum.set(0);
            }
        };

        // Enqueue multiple items
        for (int i = 0; i < itemCount; i++) {
            batchQueue.enqueue(i, executor);
        }

        // Wait for processing to complete
        boolean allFlush = prepareCount.await(10, TimeUnit.SECONDS);

        // Verify that all items were processed
        assertTrue(allFlush, "Total processed items should match enqueued items");
        assertTrue(flushCount.get() > 0, "Flush should be called");
        assertTrue(flushNumMax.get() <= chunkSize, "Flush should be called with chunk size");
    }

    @Test
    public void testMultipleThreadEnqueue() throws InterruptedException {
        int threadCount = poolSize - 1;
        int itemsPerThread = 1000000;
        int itemCount = threadCount * itemsPerThread;
        int chunkSize = 256;
        CountDownLatch prepareCount = new CountDownLatch(itemCount);
        AtomicInteger flushCount = new AtomicInteger(0);
        AtomicInteger flushNum = new AtomicInteger(0);
        AtomicInteger flushNumMax = new AtomicInteger(0);

        BatchExecutorQueue<Object> batchQueue = new BatchExecutorQueue<Object>(chunkSize) {
            @Override
            protected void prepare(Object item) {
                prepareCount.countDown();
                flushNum.incrementAndGet();
            }

            @Override
            protected void flush(Object item) {
                prepare(item);
                flushCount.incrementAndGet();
                flushNumMax.set(Math.max(flushNumMax.get(), flushNum.get()));
                flushNum.set(0);
            }
        };

        Runnable enqueueTask = () -> {
            for (int i = 0; i < itemsPerThread; i++) {
                batchQueue.enqueue(i, executor);
            }
        };

        // Submit tasks from multiple threads
        for (int i = 0; i < threadCount; i++) {
            executor.submit(enqueueTask);
        }

        // Wait for processing to complete
        boolean allFlush = prepareCount.await(1, TimeUnit.MINUTES);

        // Verify that all items were processed
        assertTrue(allFlush, "Total processed items should match enqueued items");
        assertTrue(flushCount.get() > 0, "Flush should be called at least once");
        assertTrue(flushNumMax.get() <= chunkSize, "Flush should be called with chunk size");
    }
}
