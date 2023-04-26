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

package org.apache.dubbo.common.threadpool;

import org.apache.dubbo.common.concurrent.AbortPolicy;
import org.apache.dubbo.common.concurrent.RejectException;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemorySafeLinkedBlockingQueueTest {
    @Test
    void test() {
        ByteBuddyAgent.install();
        final Instrumentation instrumentation = ByteBuddyAgent.getInstrumentation();
        final long objectSize = instrumentation.getObjectSize((Runnable) () -> {
        });
        int maxFreeMemory = (int) MemoryLimitCalculator.maxAvailable();
        MemorySafeLinkedBlockingQueue<Runnable> queue = new MemorySafeLinkedBlockingQueue<>(maxFreeMemory);
        // all memory is reserved for JVM, so it will fail here
        assertThat(queue.offer(() -> {
        }), is(false));

        // maxFreeMemory-objectSize Byte memory is reserved for the JVM, so this will succeed
        queue.setMaxFreeMemory((int) (MemoryLimitCalculator.maxAvailable() - objectSize));
        assertThat(queue.offer(() -> {
        }), is(true));
    }

    @Test
    void testCustomReject() {
        MemorySafeLinkedBlockingQueue<Runnable> queue = new MemorySafeLinkedBlockingQueue<>(Integer.MAX_VALUE);
        queue.setRejector(new AbortPolicy<>());
        assertThrows(RejectException.class, () -> queue.offer(() -> {
        }));
    }

    @Test
    @Disabled("This test is not stable, it may fail due to performance (C1, C2)")
    void testEfficiency() throws InterruptedException {
        // if length is vert large(unit test may runs for a long time), so you may need to modify JVM param such as : -Xms=1024m -Xmx=2048m
        // if you want to test efficiency of MemorySafeLinkedBlockingQueue, you may modify following param: length and times
        int length = 1000, times = 1;

        // LinkedBlockingQueue insert Integer: 500W * 20 times
        long spent1 = spend(new LinkedBlockingQueue<>(), length, times);

        // MemorySafeLinkedBlockingQueue insert Integer: 500W * 20 times
        long spent2 = spend(newMemorySafeLinkedBlockingQueue(),  length, times);
        System.gc();

        System.out.println(String.format("LinkedBlockingQueue spent %s millis, MemorySafeLinkedBlockingQueue spent %s millis", spent1, spent2));
        // efficiency between LinkedBlockingQueue and MemorySafeLinkedBlockingQueue is very nearly the same
        Assertions.assertTrue(spent1 - spent2 <= 1);
    }

    private static long spend(LinkedBlockingQueue<Integer> lbq, int length, int times) throws InterruptedException {
        // new Queue
        if (lbq instanceof MemorySafeLinkedBlockingQueue) {
            lbq = newMemorySafeLinkedBlockingQueue();
        } else {
            lbq = new LinkedBlockingQueue<>();
        }

        long total = 0L;
        for (int i = 0; i < times; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < length; j++) {
                lbq.offer(j);
            }
            long end = System.currentTimeMillis();
            long spent = end - start;
            total += spent;
        }
        long result = total / times;

        // gc
        System.gc();

        return result;
    }

    private static MemorySafeLinkedBlockingQueue<Integer> newMemorySafeLinkedBlockingQueue() {
        ByteBuddyAgent.install();
        final Instrumentation instrumentation = ByteBuddyAgent.getInstrumentation();
        final long objectSize = instrumentation.getObjectSize((Runnable) () -> { });
        int maxFreeMemory = (int) MemoryLimitCalculator.maxAvailable();
        MemorySafeLinkedBlockingQueue<Integer> queue = new MemorySafeLinkedBlockingQueue<>(maxFreeMemory);
        queue.setMaxFreeMemory((int) (MemoryLimitCalculator.maxAvailable() - objectSize));
        queue.setRejector(new AbortPolicy<>());
        return queue;
    }
}
