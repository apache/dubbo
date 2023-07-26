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
package org.apache.dubbo.common.threadpool.support.eager;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.threadpool.support.AbortPolicyWithReport;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;

class EagerThreadPoolExecutorTest {

    private static final URL URL = new ServiceConfigURL("dubbo", "localhost", 8080);

    /**
     * It print like this:
     * thread number in current pool：1,  task number in task queue：0 executor size: 1
     * thread number in current pool：2,  task number in task queue：0 executor size: 2
     * thread number in current pool：3,  task number in task queue：0 executor size: 3
     * thread number in current pool：4,  task number in task queue：0 executor size: 4
     * thread number in current pool：5,  task number in task queue：0 executor size: 5
     * thread number in current pool：6,  task number in task queue：0 executor size: 6
     * thread number in current pool：7,  task number in task queue：0 executor size: 7
     * thread number in current pool：8,  task number in task queue：0 executor size: 8
     * thread number in current pool：9,  task number in task queue：0 executor size: 9
     * thread number in current pool：10,  task number in task queue：0 executor size: 10
     * thread number in current pool：10,  task number in task queue：4 executor size: 10
     * thread number in current pool：10,  task number in task queue：3 executor size: 10
     * thread number in current pool：10,  task number in task queue：2 executor size: 10
     * thread number in current pool：10,  task number in task queue：1 executor size: 10
     * thread number in current pool：10,  task number in task queue：0 executor size: 10
     * <p>
     * We can see , when the core threads are in busy,
     * the thread pool create thread (but thread nums always less than max) instead of put task into queue.
     */
    @Disabled("replaced to testEagerThreadPoolFast for performance")
    @Test
    void testEagerThreadPool() throws Exception {
        String name = "eager-tf";
        int queues = 5;
        int cores = 5;
        int threads = 10;
        // alive 1 second
        long alive = 1000;

        //init queue and executor
        TaskQueue<Runnable> taskQueue = new TaskQueue<Runnable>(queues);
        final EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(cores,
            threads,
            alive,
            TimeUnit.MILLISECONDS,
            taskQueue,
            new NamedThreadFactory(name, true),
            new AbortPolicyWithReport(name, URL));
        taskQueue.setExecutor(executor);

        for (int i = 0; i < 15; i++) {
            Thread.sleep(50);
            executor.execute(() -> {
                System.out.println("thread number in current pool：" + executor.getPoolSize() + ",  task number in task queue：" + executor.getQueue()
                    .size() + " executor size: " + executor.getPoolSize());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        Thread.sleep(5000);
        // cores theads are all alive.
        Assertions.assertEquals(executor.getPoolSize(), cores, "more than cores threads alive!");
    }

    @Test
    void testEagerThreadPoolFast() {
        String name = "eager-tf";
        int queues = 5;
        int cores = 5;
        int threads = 10;
        // alive 1 second
        long alive = 1000;

        //init queue and executor
        TaskQueue<Runnable> taskQueue = new TaskQueue<>(queues);
        final EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(cores,
            threads,
            alive,
            TimeUnit.MILLISECONDS,
            taskQueue,
            new NamedThreadFactory(name, true),
            new AbortPolicyWithReport(name, URL));
        taskQueue.setExecutor(executor);

        CountDownLatch countDownLatch1 = new CountDownLatch(1);
        for (int i = 0; i < 10; i++) {
            executor.execute(() -> {
                try {
                    countDownLatch1.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        await().until(() -> executor.getPoolSize() == 10);
        Assertions.assertEquals(10, executor.getActiveCount());

        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        AtomicBoolean started = new AtomicBoolean(false);
        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                started.set(true);
                try {
                    countDownLatch2.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        await().until(() -> executor.getQueue().size() == 5);
        Assertions.assertEquals(10, executor.getActiveCount());
        Assertions.assertEquals(10, executor.getPoolSize());
        Assertions.assertFalse(started.get());
        countDownLatch1.countDown();

        await().until(() -> executor.getActiveCount() == 5);
        Assertions.assertTrue(started.get());

        countDownLatch2.countDown();
        await().until(() -> executor.getActiveCount() == 0);

        await().until(() -> executor.getPoolSize() == cores);
    }

    @Test
    void testSPI() {
        ExtensionLoader<ThreadPool> extensionLoader = ApplicationModel.defaultModel().getDefaultModule().getExtensionLoader(ThreadPool.class);

        ExecutorService executorService = (ExecutorService) extensionLoader
            .getExtension("eager")
            .getExecutor(URL);

        Assertions.assertEquals("EagerThreadPoolExecutor", executorService.getClass()
            .getSimpleName(), "test spi fail!");
    }

    @Test
    void testEagerThreadPool_rejectExecution1() {
        String name = "eager-tf";
        int cores = 1;
        int threads = 3;
        int queues = 2;
        long alive = 1000;

        // init queue and executor
        TaskQueue<Runnable> taskQueue = new TaskQueue<>(queues);
        final EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(cores,
            threads,
            alive, TimeUnit.MILLISECONDS,
            taskQueue,
            new NamedThreadFactory(name, true),
            new AbortPolicyWithReport(name, URL));
        taskQueue.setExecutor(executor);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Runnable runnable = () -> {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < 5; i++) {
            executor.execute(runnable);
        }

        await().until(() -> executor.getPoolSize() == threads);
        await().until(() -> executor.getQueue().size() == queues);

        Assertions.assertThrows(RejectedExecutionException.class, () -> executor.execute(runnable));

        countDownLatch.countDown();
        await().until(() -> executor.getActiveCount() == 0);

        executor.execute(runnable);
    }


    @Test
    void testEagerThreadPool_rejectExecution2() {
        String name = "eager-tf";
        int cores = 1;
        int threads = 3;
        int queues = 2;
        long alive = 1000;

        // init queue and executor
        AtomicReference<Runnable> runnableWhenRetryOffer = new AtomicReference<>();
        TaskQueue<Runnable> taskQueue = new TaskQueue<Runnable>(queues) {
            @Override
            public boolean retryOffer(Runnable o, long timeout, TimeUnit unit) throws InterruptedException {
                if (runnableWhenRetryOffer.get() != null) {
                    runnableWhenRetryOffer.get().run();
                }
                return super.retryOffer(o, timeout, unit);
            }
        };
        final EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(cores,
            threads,
            alive, TimeUnit.MILLISECONDS,
            taskQueue,
            new NamedThreadFactory(name, true),
            new AbortPolicyWithReport(name, URL));
        taskQueue.setExecutor(executor);

        Semaphore semaphore = new Semaphore(0);
        Runnable runnable = () -> {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < 5; i++) {
            executor.execute(runnable);
        }

        await().until(() -> executor.getPoolSize() == threads);
        await().until(() -> executor.getQueue().size() == queues);

        Assertions.assertThrows(RejectedExecutionException.class, () -> executor.execute(runnable));

        runnableWhenRetryOffer.set(() -> {
            semaphore.release();
            await().until(() -> executor.getCompletedTaskCount() == 1);
        });
        executor.execute(runnable);
        semaphore.release(5);
        await().until(() -> executor.getActiveCount() == 0);
    }
}
