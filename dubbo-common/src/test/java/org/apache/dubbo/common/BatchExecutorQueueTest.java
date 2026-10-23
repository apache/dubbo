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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatchExecutorQueueTest {

    private static class RecordingQueue extends BatchExecutorQueue<String> {
        final List<String> processed = new ArrayList<>();
        private final Set<String> failing;

        RecordingQueue(int chunkSize, String... failing) {
            super(chunkSize);
            this.failing = new HashSet<>(Arrays.asList(failing));
        }

        @Override
        protected void prepare(String item) {
            handle(item);
        }

        @Override
        protected void flush(String item) {
            handle(item);
        }

        private void handle(String item) {
            if (failing.contains(item)) {
                throw new IllegalStateException("boom");
            }
            processed.add(item);
        }
    }

    /** Enqueues everything before the batch runs, then runs the single scheduled batch. */
    private static void runAsOneBatch(BatchExecutorQueue<String> queue, String... items) {
        List<Runnable> tasks = new ArrayList<>();
        for (String item : items) {
            queue.enqueue(item, r -> tasks.add(r));
        }
        Assertions.assertFalse(tasks.isEmpty());
        for (Runnable task : new ArrayList<>(tasks)) {
            try {
                task.run();
            } catch (RuntimeException processingFailure) {
                // whether a processing failure escapes the scheduled task is not part of the contract
            }
        }
    }

    @Test
    void itemsAfterAThrowingItemAreStillProcessed() {
        RecordingQueue queue = new RecordingQueue(256, "bad");
        runAsOneBatch(queue, "a", "bad", "c", "d");
        Assertions.assertEquals(Arrays.asList("a", "c", "d"), queue.processed);
    }

    @Test
    void throwingLastItemDoesNotAffectEarlierItems() {
        RecordingQueue queue = new RecordingQueue(256, "bad");
        runAsOneBatch(queue, "a", "b", "bad");
        Assertions.assertEquals(Arrays.asList("a", "b"), queue.processed);
    }

    @Test
    void throwingItemAtChunkBoundaryDoesNotDropTheRest() {
        RecordingQueue queue = new RecordingQueue(2, "bad");
        runAsOneBatch(queue, "a", "b", "bad", "d", "e");
        Assertions.assertEquals(Arrays.asList("a", "b", "d", "e"), queue.processed);
    }

    @Test
    void severalThrowingItemsInOneBatch() {
        RecordingQueue queue = new RecordingQueue(256, "bad1", "bad2");
        runAsOneBatch(queue, "a", "bad1", "b", "bad2", "c");
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), queue.processed);
    }

    @Test
    void queueKeepsWorkingAfterExecutorRejectsTheTask() {
        RecordingQueue queue = new RecordingQueue(256);
        AtomicBoolean reject = new AtomicBoolean(true);
        Executor executor = task -> {
            if (reject.getAndSet(false)) {
                throw new RejectedExecutionException("rejected");
            }
            task.run();
        };
        try {
            queue.enqueue("a", executor);
        } catch (RejectedExecutionException ignored) {
            // whether the rejection reaches the caller is not part of the contract
        }
        queue.enqueue("b", executor);
        Assertions.assertEquals(Arrays.asList("a", "b"), queue.processed);
    }
}
