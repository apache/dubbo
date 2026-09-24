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
package org.apache.dubbo.common.threadpool.support.cached;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadlocal.InternalThread;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.threadpool.support.AbortPolicyWithReport;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.awaitility.Durations;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.ALIVE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CORE_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.QUEUES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

class CachedThreadPoolTest {
    @Test
    void getExecutor1() throws Exception {
        URL url = URL.valueOf("dubbo://10.20.130.230:20880/context/path?" + THREAD_NAME_KEY
                + "=demo&" + CORE_THREADS_KEY
                + "=1&" + THREADS_KEY
                + "=2&" + ALIVE_KEY
                + "=1000&" + QUEUES_KEY
                + "=0");
        ThreadPool threadPool = new CachedThreadPool();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) threadPool.getExecutor(url);
        assertThat(executor.getCorePoolSize(), is(1));
        assertThat(executor.getMaximumPoolSize(), is(2));
        // idle core threads must be allowed to time out, see #8342
        assertThat(executor.allowsCoreThreadTimeOut(), is(true));
        assertThat(executor.getQueue(), Matchers.<BlockingQueue<Runnable>>instanceOf(SynchronousQueue.class));
        assertThat(
                executor.getRejectedExecutionHandler(),
                Matchers.<RejectedExecutionHandler>instanceOf(AbortPolicyWithReport.class));

        final CountDownLatch latch = new CountDownLatch(1);
        executor.execute(() -> {
            Thread thread = Thread.currentThread();
            assertThat(thread, instanceOf(InternalThread.class));
            assertThat(thread.getName(), startsWith("demo"));
            latch.countDown();
        });

        latch.await();
        assertThat(latch.getCount(), is(0L));
    }

    @Test
    void getExecutor2() {
        URL url = URL.valueOf("dubbo://10.20.130.230:20880/context/path?" + QUEUES_KEY + "=1");
        ThreadPool threadPool = new CachedThreadPool();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) threadPool.getExecutor(url);
        assertThat(executor.getQueue(), Matchers.<BlockingQueue<Runnable>>instanceOf(LinkedBlockingQueue.class));
    }

    // Reproduces https://github.com/apache/dubbo/issues/8342:
    // a cached pool with explicit core threads must recycle idle threads (core ones included)
    // once they stay idle longer than 'alive', as its javadoc promises.
    @Test
    void idleCoreThreadsShouldBeRecycledAfterKeepAlive() throws Exception {
        URL url = URL.valueOf("dubbo://10.20.130.230:20880/context/path?" + THREAD_NAME_KEY
                + "=recycle&" + CORE_THREADS_KEY
                + "=2&" + THREADS_KEY
                + "=5&" + ALIVE_KEY
                + "=300&" + QUEUES_KEY
                + "=0");
        ThreadPoolExecutor executor = (ThreadPoolExecutor) new CachedThreadPool().getExecutor(url);

        int taskCount = 5;
        CyclicBarrier barrier = new CyclicBarrier(taskCount);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            executor.execute(() -> {
                try {
                    // hold every worker thread so the pool has to grow to maximumPoolSize
                    barrier.await();
                    release.await();
                } catch (InterruptedException | java.util.concurrent.BrokenBarrierException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }

        // pool must have expanded to 5 worker threads
        await().atMost(Durations.FIVE_SECONDS).until(() -> executor.getPoolSize() == taskCount);
        assertThat(executor.getPoolSize(), is(taskCount));

        // release all tasks, workers become idle
        release.countDown();
        assertThat(finished.await(5, TimeUnit.SECONDS), is(true));

        // a self-tuned cached pool must shrink back to zero after keep-alive, core threads included
        await().atMost(Durations.TEN_SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> executor.getPoolSize() == 0);
        assertThat(executor.getPoolSize(), is(0));
        executor.shutdownNow();
    }
}
