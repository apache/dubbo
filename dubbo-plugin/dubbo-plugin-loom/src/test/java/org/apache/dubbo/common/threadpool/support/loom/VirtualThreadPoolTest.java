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
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.apache.dubbo.common.constants.CommonConstants.QUEUES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_VIRTUAL_CORE;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VirtualThreadPoolTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void getExecutor1() throws Exception {
        URL url = URL.valueOf("dubbo://10.20.130.230:20880/context/path?" + THREAD_NAME_KEY + "=demo");
        ThreadPool threadPool = new VirtualThreadPool();
        Executor executor = threadPool.getExecutor(url);

        final CountDownLatch latch = new CountDownLatch(1);
        executor.execute(() -> {
            Thread thread = Thread.currentThread();
            assertTrue(thread.isVirtual());
            assertThat(thread.getName(), startsWith("demo"));
            latch.countDown();
        });

        latch.await();
        assertThat(latch.getCount(), is(0L));
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void getExecutor2() {
        URL url = URL.valueOf("dubbo://10.20.130.230:20880/context/path?" + QUEUES_KEY + "=1");
        ThreadPool threadPool = new VirtualThreadPool();
        assertThat(
                threadPool.getExecutor(url).getClass().getName(),
                Matchers.is("java.util.concurrent.ThreadPerTaskExecutor"));
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void getExecutor3() throws Exception {
        URL url = URL.valueOf(
                "dubbo://10.20.130.230:20880/context/path?" + THREADS_VIRTUAL_CORE + "=2&" + THREAD_NAME_KEY + "=demo");
        ThreadPool threadPool = new VirtualThreadPool();
        Executor executor = threadPool.getExecutor(url);

        assertThat(executor, instanceOf(ThreadPoolExecutor.class));
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
        assertThat(tpe.getCorePoolSize(), is(2));
        assertThat(tpe.getMaximumPoolSize(), is(Integer.MAX_VALUE));
        assertThat(tpe.getQueue(), instanceOf(SynchronousQueue.class));

        // Regression guard for issue #16174: keepAliveTime must be > 0 so that non-core
        // virtual threads stay alive long enough to reuse ThreadLocal-cached state.
        assertTrue(
                tpe.getKeepAliveTime(TimeUnit.SECONDS) > 0,
                "keepAliveTime must be > 0 to enable ThreadLocal reuse; "
                        + "a value of 0 causes threads to die immediately after each task, "
                        + "defeating the purpose of the pooled mode.");
        assertEquals(VirtualThreadPool.KEEP_ALIVE_SECONDS, tpe.getKeepAliveTime(TimeUnit.SECONDS));

        final CountDownLatch latch = new CountDownLatch(1);
        executor.execute(() -> {
            Thread thread = Thread.currentThread();
            assertTrue(thread.isVirtual());
            assertThat(thread.getName(), startsWith("demo"));
            latch.countDown();
        });

        latch.await();
        assertThat(latch.getCount(), is(0L));
    }

    /**
     * Verifies that in pooled mode, a warm virtual thread can be reused for a second task,
     * making ThreadLocal-cached values visible across consecutive submissions.
     *
     * <p>This is the key property that justifies the pooled mode (see issue #16042): libraries
     * like FastJSON and Aerospike Java client store large byte-buffers in ThreadLocals. If threads
     * are reused, those buffers survive across requests (reducing GC pressure). If every task
     * gets a fresh thread the cache is useless.
     *
     * <p>The test submits two tasks sequentially to a pooled executor with corePoolSize=1.
     * The first task sets a ThreadLocal value; the second task checks whether the same thread
     * ran it and whether the ThreadLocal value is still present.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void getExecutor4_threadLocalReuseInPooledMode() throws Exception {
        URL url = URL.valueOf("dubbo://10.20.130.230:20880/context/path?" + THREADS_VIRTUAL_CORE + "=1&"
                + THREAD_NAME_KEY + "=pool-reuse-test");
        ThreadPool threadPool = new VirtualThreadPool();
        Executor executor = threadPool.getExecutor(url);

        // A ThreadLocal that the first task populates and the second task reads.
        ThreadLocal<String> tl = new ThreadLocal<>();

        AtomicReference<Thread> firstThread = new AtomicReference<>();

        // Task 1: record the executing thread and set a ThreadLocal value.
        CountDownLatch task1Done = new CountDownLatch(1);
        executor.execute(() -> {
            firstThread.set(Thread.currentThread());
            tl.set("cached-value");
            task1Done.countDown();
        });
        task1Done.await();

        // Task 2: check whether the same thread is reused and the ThreadLocal survived.
        AtomicBoolean sameThread = new AtomicBoolean(false);
        AtomicBoolean tlValuePresent = new AtomicBoolean(false);

        CountDownLatch task2Done = new CountDownLatch(1);
        executor.execute(() -> {
            sameThread.set(Thread.currentThread() == firstThread.get());
            tlValuePresent.set("cached-value".equals(tl.get()));
            task2Done.countDown();
        });
        task2Done.await();

        // With corePoolSize=1 and a 60-second keepAlive, the same thread should be reused
        // for two back-to-back tasks, making the ThreadLocal value visible in task 2.
        assertTrue(
                sameThread.get(),
                "Pooled executor with corePoolSize=1 should reuse the same thread for "
                        + "back-to-back tasks, enabling ThreadLocal cache reuse.");
        assertTrue(
                tlValuePresent.get(),
                "ThreadLocal value set by task 1 should be visible in task 2 when the "
                        + "same thread is reused (the core rationale for pooled mode).");
    }
}
