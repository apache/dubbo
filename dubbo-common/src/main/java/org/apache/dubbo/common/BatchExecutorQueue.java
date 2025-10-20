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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchExecutorQueue<T> {
    static final int DEFAULT_QUEUE_SIZE = 128;
    private volatile Queue<T> queue;
    private volatile Queue<T> readQueue;
    private final AtomicBoolean scheduled;
    private final int chunkSize;

    public BatchExecutorQueue() {
        this(DEFAULT_QUEUE_SIZE);
    }

    public BatchExecutorQueue(int chunkSize) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.readQueue = new ConcurrentLinkedQueue<>();
        this.scheduled = new AtomicBoolean(false);
        this.chunkSize = chunkSize;
    }

    public void enqueue(T message, Executor executor) {
        queue.add(message);
        scheduleFlush(executor);
    }

    protected void scheduleFlush(Executor executor) {
        if (scheduled.compareAndSet(false, true)) {
            executor.execute(() -> this.run(executor));
        }
    }

    private Queue<T> swapQueue() {
        // Swaps the active write queue with the standby read queue.
        // This lock-free handoff allows producers to continue writing to a fresh queue
        // while the consumer processes the accumulated batch from the swapped-out queue.
        // Volatile variables ensure safe publication between threads.
        Queue<T> snapshot = queue;
        queue = readQueue;
        readQueue = snapshot;
        return snapshot;
    }

    private void run(Executor executor) {
        try {
            T item;
            int i = 1;
            Queue<T> snapshotQueue = swapQueue();
            while ((item = snapshotQueue.poll()) != null) {
                if (i == chunkSize) {
                    flush(item);
                    i = 1;
                } else if (snapshotQueue.isEmpty()) {
                    flush(item);
                } else {
                    prepare(item);
                    i++;
                }
            }
        } finally {
            scheduled.set(false);
            if (!queue.isEmpty()) {
                scheduleFlush(executor);
            }
        }
    }

    protected void prepare(T item) {}

    protected void flush(T item) {}
}
