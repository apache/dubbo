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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.ThreadPool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.apache.dubbo.common.constants.CommonConstants.THREADS_VIRTUAL_CORE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Benchmark-methodology correctness tests for {@link VirtualThreadPool}.
 *
 * <h2>Background (issue #16174)</h2>
 *
 * <p>The benchmark in issue #16042 / PR #16055 contained a synchronization bug: the timing code
 * awaited {@code countDownLatch1} (the <em>start gate</em>) rather than {@code countDownLatch2}
 * (the <em>completion latch</em>). This meant the elapsed time was measured before all tasks had
 * finished, making the pooled executor appear faster than the non-pooled one even though both
 * modes complete all tasks in roughly the same wall-clock time.
 *
 * <h2>Correct two-latch pattern</h2>
 *
 * <pre>{@code
 * CountDownLatch startGate       = new CountDownLatch(1);  // latch 1 - release all tasks together
 * CountDownLatch completionLatch = new CountDownLatch(N);  // latch 2 - await ALL task completions
 *
 * for (int i = 0; i < N; i++) {
 *     executor.execute(() -> {
 *         startGate.await();            // wait until everyone is ready
 *         doWork();
 *         completionLatch.countDown();  // signal completion
 *     });
 * }
 *
 * long t0 = System.nanoTime();
 * startGate.countDown();              // release all tasks simultaneously
 * completionLatch.await();            // MUST await the COMPLETION latch, NOT the start gate
 * long elapsed = System.nanoTime() - t0;
 * }</pre>
 *
 * <p>These tests verify that:
 * <ol>
 *   <li>Both pooled and non-pooled executors complete <em>all</em> tasks before the timing window
 *       closes (i.e. they never return early due to the wrong latch being awaited).
 *   <li>The task completion count exactly equals the number of submitted tasks in both modes.
 * </ol>
 */
public class VirtualThreadPoolBenchmarkTest {

    private static final int TASK_COUNT = 200;

    /**
     * Verifies that the non-pooled (default) executor runs all tasks to completion when measured
     * with the corrected two-latch pattern.
     *
     * <p>The start gate ({@code startGate}) releases all submitted tasks at the same time so they
     * compete for the scheduler simultaneously. The completion latch ({@code completionLatch}) is
     * decremented by every task when it finishes. Only after <em>completionLatch</em> reaches zero
     * do we stop the clock, ensuring the elapsed time reflects actual end-to-end execution.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void unpooledExecutor_allTasksComplete_withCorrectTwoLatchPattern() throws InterruptedException {
        URL url = URL.valueOf("dubbo://10.20.130.230:20880/context/path");
        ThreadPool threadPool = new VirtualThreadPool();
        Executor executor = threadPool.getExecutor(url);

        runBenchmark(executor, TASK_COUNT, "unpooled");
    }

    /**
     * Verifies that the pooled executor also runs all tasks to completion when measured with the
     * corrected two-latch pattern.
     *
     * <p>Previously, a flawed benchmark awaited the start gate a second time instead of the
     * completion latch, returning immediately after releasing tasks. This test would have caught
     * that bug because {@code completedTasks} would be far less than {@code TASK_COUNT}.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void pooledExecutor_allTasksComplete_withCorrectTwoLatchPattern() throws InterruptedException {
        URL url = URL.valueOf("dubbo://10.20.130.230:20880/context/path?" + THREADS_VIRTUAL_CORE + "="
                + Runtime.getRuntime().availableProcessors());
        ThreadPool threadPool = new VirtualThreadPool();
        Executor executor = threadPool.getExecutor(url);

        runBenchmark(executor, TASK_COUNT, "pooled");
    }

    /**
     * Validates the two-latch timing pattern itself: awaiting the completion latch means elapsed
     * time is always >= the time to complete all tasks (trivially verifiable because the task
     * counter equals {@code taskCount} when the method returns).
     *
     * @param executor      the executor under test
     * @param taskCount     number of tasks to submit
     * @param executorLabel human-readable label for assertion messages
     */
    private static void runBenchmark(Executor executor, int taskCount, String executorLabel)
            throws InterruptedException {
        // latch 1: start gate - holds all tasks until released together (simulates concurrent load)
        CountDownLatch startGate = new CountDownLatch(1);
        // latch 2: completion latch - counts down when each task finishes
        CountDownLatch completionLatch = new CountDownLatch(taskCount);

        AtomicInteger completedTasks = new AtomicInteger(0);

        for (int i = 0; i < taskCount; i++) {
            executor.execute(() -> {
                try {
                    // Wait until all tasks are queued and the start gate opens.
                    startGate.await();
                    // Simulate a minimal unit of work (e.g. an RPC handler body).
                    simulateWork();
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // Always signal completion so the benchmark can drain.
                    // IMPORTANT: this must countDown on completionLatch (latch 2),
                    // NOT on startGate (latch 1). Decrementing latch 1 here was the
                    // bug in the original benchmark reported in issue #16174.
                    completionLatch.countDown();
                }
            });
        }

        long startNanos = System.nanoTime();
        // Release all tasks simultaneously.
        startGate.countDown();
        // Correct: await completionLatch (latch 2), NOT startGate (latch 1).
        // Awaiting startGate here would return immediately (it is already at 0) and make
        // the measurement appear artificially fast - exactly the flaw in #16174.
        completionLatch.await();
        long elapsedNanos = System.nanoTime() - startNanos;

        // All tasks must have finished before we get here.
        assertEquals(
                taskCount,
                completedTasks.get(),
                executorLabel + " executor: expected all " + taskCount
                        + " tasks to complete before completionLatch.await() returned, "
                        + "but only " + completedTasks.get() + " finished. "
                        + "This indicates the benchmark awaited the wrong latch.");

        assertTrue(elapsedNanos > 0, executorLabel + " executor: elapsed time must be positive");
    }

    /**
     * Simulates a lightweight unit of work inside each task.
     * Intentionally minimal to keep the test fast while still being non-trivial.
     */
    private static void simulateWork() {
        // Trivial CPU work: a small loop that the JIT won't optimise away entirely.
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i;
        }
        // Prevent dead-code elimination.
        if (sum < 0) {
            throw new IllegalStateException("impossible");
        }
    }
}
