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
package org.apache.dubbo.common.threadpool.support.loom;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScheduledVirtualExecutorServiceTest {
    @Test
    void testSchedule() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");

        // normal result
        AtomicBoolean testPassed = new AtomicBoolean(false);
        long startTime1 = System.currentTimeMillis();
        ScheduledFuture<?> future = scheduledVirtualExecutorService.schedule(
                () -> {
                    if (!Thread.currentThread().isVirtual()) {
                        throw new IllegalStateException("Thread is not virtual");
                    }
                    long endTime = System.currentTimeMillis();
                    if (endTime - startTime1 < 1000) {
                        throw new IllegalStateException("Time is less than 1000");
                    }
                    testPassed.set(true);
                },
                1,
                TimeUnit.SECONDS);

        Awaitility.await().until(testPassed::get);
        assertTrue(future.isDone());

        // cancel task - 1
        testPassed.set(true);
        long startTime2 = System.currentTimeMillis();
        future = scheduledVirtualExecutorService.schedule(
                () -> {
                    testPassed.set(false);
                },
                10,
                TimeUnit.SECONDS);
        future.cancel(false);

        Awaitility.await().until(future::isDone);
        assertTrue(testPassed.get());
        assertTrue(future::isCancelled);
        assertTrue(System.currentTimeMillis() - startTime2 < 10_000);

        // cancel task - 2
        testPassed.set(false);
        AtomicBoolean entered = new AtomicBoolean(false);
        future = scheduledVirtualExecutorService.schedule(
                () -> {
                    try {
                        entered.set(true);
                        Thread.sleep(Integer.MAX_VALUE);
                    } catch (InterruptedException e) {
                        testPassed.set(true);
                    }
                },
                0,
                TimeUnit.SECONDS);
        Awaitility.await().until(entered::get);
        Thread.sleep(100);
        future.cancel(true);

        Awaitility.await().until(future::isDone);
        assertTrue(testPassed.get());
        assertTrue(future::isCancelled);

        scheduledVirtualExecutorService.shutdown();
        Assertions.assertThrows(
                RejectedExecutionException.class,
                () -> scheduledVirtualExecutorService.schedule(() -> {}, 1, TimeUnit.SECONDS));
        assertTrue(scheduledVirtualExecutorService.isShutdown());
    }

    @Test
    void testScheduleCallcable() throws ExecutionException, InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");

        // normal result
        long startTime1 = System.currentTimeMillis();
        ScheduledFuture<Boolean> future = scheduledVirtualExecutorService.schedule(
                () -> {
                    if (!Thread.currentThread().isVirtual()) {
                        throw new IllegalStateException("Thread is not virtual");
                    }
                    long endTime = System.currentTimeMillis();
                    if (endTime - startTime1 < 1000) {
                        throw new IllegalStateException("Time is less than 1000");
                    }
                    return true;
                },
                1,
                TimeUnit.SECONDS);

        assertTrue(future.get());
        assertTrue(future.isDone());

        // cancel task - 1
        AtomicBoolean testPassed = new AtomicBoolean(true);
        long startTime2 = System.currentTimeMillis();
        ScheduledFuture<Boolean> future1 = scheduledVirtualExecutorService.schedule(
                () -> {
                    testPassed.set(false);
                    return false;
                },
                10,
                TimeUnit.SECONDS);
        future1.cancel(false);

        Awaitility.await().until(future1::isDone);
        assertTrue(testPassed.get());
        assertTrue(future1::isCancelled);
        assertTrue(System.currentTimeMillis() - startTime2 < 10_000);
        Assertions.assertThrows(CancellationException.class, future1::get);

        // cancel task - 2
        testPassed.set(false);
        AtomicBoolean entered = new AtomicBoolean(false);
        ScheduledFuture<Boolean> future2 = scheduledVirtualExecutorService.schedule(
                () -> {
                    try {
                        entered.set(true);
                        Thread.sleep(Integer.MAX_VALUE);
                    } catch (InterruptedException e) {
                        testPassed.set(true);
                        return true;
                    }
                    return false;
                },
                0,
                TimeUnit.SECONDS);
        Awaitility.await().until(entered::get);
        Thread.sleep(100);
        future2.cancel(true);

        Awaitility.await().until(future2::isDone);
        assertTrue(testPassed.get());
        assertTrue(future2::isCancelled);
        Assertions.assertThrows(CancellationException.class, future2::get);

        scheduledVirtualExecutorService.shutdown();
        Assertions.assertThrows(
                RejectedExecutionException.class,
                () -> scheduledVirtualExecutorService.schedule(() -> {}, 1, TimeUnit.SECONDS));
        assertTrue(scheduledVirtualExecutorService.isShutdown());
    }

    @Test
    void testScheduleAtFixedRate() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");

        Queue<String> queue = new ConcurrentLinkedQueue<>();

        scheduledVirtualExecutorService.scheduleAtFixedRate(
                () -> {
                    queue.add(Thread.currentThread().getName());
                },
                0,
                100,
                TimeUnit.MILLISECONDS);

        Awaitility.await().until(() -> queue.size() > 3);

        scheduledVirtualExecutorService.shutdown();
        Assertions.assertThrows(
                RejectedExecutionException.class,
                () -> scheduledVirtualExecutorService.schedule(() -> {}, 1, TimeUnit.SECONDS));
        assertTrue(scheduledVirtualExecutorService.isShutdown());

        int latestSize = queue.size();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            assertEquals(latestSize, queue.size());
        }
    }

    @Test
    void testScheduleAtFixedRateBlock() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");

        Queue<Thread> queue = new ConcurrentLinkedQueue<>();

        scheduledVirtualExecutorService.scheduleAtFixedRate(
                () -> {
                    queue.add(Thread.currentThread());
                    try {
                        Thread.sleep(Integer.MAX_VALUE);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                0,
                100,
                TimeUnit.MILLISECONDS);

        Awaitility.await().until(() -> queue.size() > 3);

        for (Thread thread : queue) {
            assertTrue(thread.isVirtual());
            assertTrue(thread.isAlive());
        }

        scheduledVirtualExecutorService.shutdownNow();
        Assertions.assertThrows(
                RejectedExecutionException.class,
                () -> scheduledVirtualExecutorService.schedule(() -> {}, 1, TimeUnit.SECONDS));
        assertTrue(scheduledVirtualExecutorService.isShutdown());

        int latestSize = queue.size();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            assertEquals(latestSize, queue.size());
        }
        for (Thread thread : queue) {
            assertFalse(thread.isAlive());
        }
    }

    @Test
    void testScheduleAtFixedRateCancel() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");

        Queue<String> queue = new ConcurrentLinkedQueue<>();

        ScheduledFuture<?> future = scheduledVirtualExecutorService.scheduleAtFixedRate(
                () -> {
                    queue.add(Thread.currentThread().getName());
                },
                0,
                100,
                TimeUnit.MILLISECONDS);

        Awaitility.await().until(() -> queue.size() > 3);

        Set<Long> delay = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(20);
            delay.add(future.getDelay(TimeUnit.MILLISECONDS));
        }
        assertTrue(delay.size() > 2);

        future.cancel(false);
        Awaitility.await().until(() -> future.getDelay(TimeUnit.MILLISECONDS) == 0);
        for (int i = 0; i < 10; i++) {
            Thread.sleep(20);
            assertEquals(0, future.getDelay(TimeUnit.MILLISECONDS));
        }

        scheduledVirtualExecutorService.shutdown();
        Assertions.assertThrows(
                RejectedExecutionException.class,
                () -> scheduledVirtualExecutorService.schedule(() -> {}, 1, TimeUnit.SECONDS));
        assertTrue(scheduledVirtualExecutorService.isShutdown());

        int latestSize = queue.size();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            assertEquals(latestSize, queue.size());
        }
    }

    @Test
    void testScheduleWithFixedDelay() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");

        Queue<String> queue = new ConcurrentLinkedQueue<>();

        scheduledVirtualExecutorService.scheduleWithFixedDelay(
                () -> {
                    queue.add(Thread.currentThread().getName());
                },
                0,
                100,
                TimeUnit.MILLISECONDS);

        Awaitility.await().until(() -> queue.size() > 3);

        scheduledVirtualExecutorService.shutdown();
        Assertions.assertThrows(
                RejectedExecutionException.class,
                () -> scheduledVirtualExecutorService.schedule(() -> {}, 1, TimeUnit.SECONDS));
        assertTrue(scheduledVirtualExecutorService.isShutdown());

        assertTrue(scheduledVirtualExecutorService.awaitTermination(10, TimeUnit.SECONDS));

        int latestSize = queue.size();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            assertEquals(latestSize, queue.size());
        }
    }

    @Test
    void testScheduleWithFixedDelayBlock() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");

        Queue<Thread> queue = new ConcurrentLinkedQueue<>();
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);

        scheduledVirtualExecutorService.scheduleWithFixedDelay(
                () -> {
                    queue.add(Thread.currentThread());
                    while (atomicBoolean.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                },
                0,
                100,
                TimeUnit.MILLISECONDS);

        Awaitility.await().until(() -> !queue.isEmpty());
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            assertEquals(1, queue.size());
        }
        Thread thread1 = queue.peek();
        assertTrue(thread1.isVirtual());
        assertTrue(thread1.isAlive());

        atomicBoolean.set(false);
        Awaitility.await().until(() -> queue.size() > 3);
        for (Thread thread : queue) {
            assertEquals(thread1, thread);
        }

        atomicBoolean.set(true);

        Thread.sleep(200);

        int blockedSize = queue.size();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            assertEquals(blockedSize, queue.size());
        }

        atomicBoolean.set(false);

        scheduledVirtualExecutorService.shutdownNow();
        Assertions.assertThrows(
                RejectedExecutionException.class,
                () -> scheduledVirtualExecutorService.schedule(() -> {}, 1, TimeUnit.SECONDS));
        assertTrue(scheduledVirtualExecutorService.isShutdown());

        int latestSize = queue.size();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            assertEquals(latestSize, queue.size());
        }
        for (Thread thread : queue) {
            assertFalse(thread.isAlive());
        }
    }

    @Test
    void testScheduleWithFixedDelayCancel() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");

        Queue<String> queue = new ConcurrentLinkedQueue<>();

        ScheduledFuture<?> future = scheduledVirtualExecutorService.scheduleWithFixedDelay(
                () -> {
                    queue.add(Thread.currentThread().getName());
                },
                0,
                100,
                TimeUnit.MILLISECONDS);

        Awaitility.await().until(() -> queue.size() > 3);

        Set<Long> delay = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(20);
            delay.add(future.getDelay(TimeUnit.MILLISECONDS));
        }
        assertTrue(delay.size() > 2);

        future.cancel(false);
        Awaitility.await().until(() -> future.getDelay(TimeUnit.MILLISECONDS) == 0);
        for (int i = 0; i < 10; i++) {
            Thread.sleep(20);
            assertEquals(0, future.getDelay(TimeUnit.MILLISECONDS));
        }

        scheduledVirtualExecutorService.shutdown();
        Assertions.assertThrows(
                RejectedExecutionException.class,
                () -> scheduledVirtualExecutorService.schedule(() -> {}, 1, TimeUnit.SECONDS));
        assertTrue(scheduledVirtualExecutorService.isShutdown());

        int latestSize = queue.size();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            assertEquals(latestSize, queue.size());
        }
    }

    @Test
    void testShutdown() {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        scheduledVirtualExecutorService.shutdown();
        assertTrue(scheduledVirtualExecutorService.isShutdown());
    }

    @Test
    void testIsShutdown() {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        assertFalse(scheduledVirtualExecutorService.isShutdown());
        scheduledVirtualExecutorService.shutdown();
        assertTrue(scheduledVirtualExecutorService.isShutdown());
    }

    @Test
    void testIsTerminated() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        assertFalse(scheduledVirtualExecutorService.isTerminated());
        scheduledVirtualExecutorService.shutdown();
        scheduledVirtualExecutorService.awaitTermination(1, TimeUnit.SECONDS);
        assertTrue(scheduledVirtualExecutorService.isTerminated());
    }

    @Test
    void testAwaitTermination() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        scheduledVirtualExecutorService.shutdown();
        assertTrue(scheduledVirtualExecutorService.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    void testExecute() {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        scheduledVirtualExecutorService.execute(() -> {});
    }

    @Test
    void testSubmit() throws ExecutionException, InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        Future<String> future = scheduledVirtualExecutorService.submit(() -> "Hello, World!");
        assertEquals("Hello, World!", future.get());
    }

    @Test
    void testSubmitWithResult() throws ExecutionException, InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        Future<String> future = scheduledVirtualExecutorService.submit(() -> {}, "Hello, World!");
        assertEquals("Hello, World!", future.get());
    }

    @Test
    void testInvokeAll() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        List<Callable<String>> tasks =
                Arrays.asList(() -> "Hello,", () -> " World!", () -> " How", () -> " are", () -> " you?");
        List<Future<String>> futures = scheduledVirtualExecutorService.invokeAll(tasks);
        StringBuilder result = new StringBuilder();
        for (Future<String> future : futures) {
            try {
                result.append(future.get());
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        assertEquals("Hello, World! How are you?", result.toString());
    }

    @Test
    void testInvokeAny() throws ExecutionException, InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        List<Callable<String>> tasks =
                Arrays.asList(() -> "Hello,", () -> " World!", () -> " How", () -> " are", () -> " you?");
        String result = scheduledVirtualExecutorService.invokeAny(tasks);
        assertTrue(Arrays.asList("Hello,", " World!", " How", " are", " you?").contains(result));
    }

    @Test
    void testInvokeAllWithTimeout() throws InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        List<Callable<String>> tasks =
                Arrays.asList(() -> "Hello,", () -> " World!", () -> " How", () -> " are", () -> " you?");
        List<Future<String>> futures = scheduledVirtualExecutorService.invokeAll(tasks, 1, TimeUnit.SECONDS);
        StringBuilder result = new StringBuilder();
        for (Future<String> future : futures) {
            try {
                result.append(future.get());
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        assertEquals("Hello, World! How are you?", result.toString());
    }

    @Test
    void testInvokeAnyWithTimeout() throws ExecutionException, InterruptedException, TimeoutException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        List<Callable<String>> tasks =
                Arrays.asList(() -> "Hello,", () -> " World!", () -> " How", () -> " are", () -> " you?");
        String result = scheduledVirtualExecutorService.invokeAny(tasks, 1, TimeUnit.SECONDS);
        assertTrue(Arrays.asList("Hello,", " World!", " How", " are", " you?").contains(result));
    }

    @Test
    void testSubmitRunnable() throws ExecutionException, InterruptedException {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        AtomicBoolean ran = new AtomicBoolean(false);
        Future<?> future = scheduledVirtualExecutorService.submit(() -> ran.set(true));
        future.get();
        assertTrue(ran.get());
    }
}
