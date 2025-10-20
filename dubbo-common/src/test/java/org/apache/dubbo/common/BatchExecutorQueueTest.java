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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        AtomicInteger prepareCount = new AtomicInteger(0);
        AtomicInteger flushCount = new AtomicInteger(0);

        BatchExecutorQueue<Object> batchQueue = new BatchExecutorQueue<Object>() {
            @Override
            protected void prepare(Object item) {
                prepareCount.incrementAndGet();
            }

            @Override
            protected void flush(Object item) {
                prepare(item);
                flushCount.incrementAndGet();
            }
        };

        // Enqueue multiple items
        int itemCount = 100;
        for (int i = 0; i < itemCount; i++) {
            batchQueue.enqueue(i, executor);
        }

        // Wait for processing to complete
        int i = 0;
        while (prepareCount.get() < itemCount && ++i < 10) {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(100));
        }

        // Verify that all items were processed
        assertEquals(itemCount, prepareCount.get(), "Prepare should be called");
        assertTrue(flushCount.get() > 0, "Flush should be called");
    }

    @Test
    public void testMultipleThreadEnqueue() throws InterruptedException {
        AtomicInteger prepareCount = new AtomicInteger(0);
        AtomicInteger flushCount = new AtomicInteger(0);

        BatchExecutorQueue<Object> batchQueue = new BatchExecutorQueue<Object>() {
            @Override
            protected void prepare(Object item) {
                prepareCount.incrementAndGet();
            }

            @Override
            protected void flush(Object item) {
                prepare(item);
                flushCount.incrementAndGet();
            }
        };

        int threadCount = poolSize - 1;
        int itemsPerThread = 5000000;
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
        int i = 0;
        while (prepareCount.get() < threadCount * itemsPerThread) {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(100));
            assertTrue(++i < 10000, "Total processed items should match enqueued items");
        }

        // Verify that all items were processed
        assertTrue(flushCount.get() > 0, "Flush should be called at least once");
    }
}
