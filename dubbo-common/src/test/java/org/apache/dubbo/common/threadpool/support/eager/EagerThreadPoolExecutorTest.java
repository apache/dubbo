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
import org.apache.dubbo.common.utils.NamedThreadFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class EagerThreadPoolExecutorTest {

    private static final URL URL = new URL("dubbo", "localhost", 8080);

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
    @Test
    public void testEagerThreadPool() throws Exception {
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
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println("thread number in current pool：" + executor.getPoolSize() + ",  task number in task queue：" + executor.getQueue()
                            .size() + " executor size: " + executor.getPoolSize());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        Thread.sleep(5000);
        // cores theads are all alive.
        Assertions.assertEquals(executor.getPoolSize(), cores, "more than cores threads alive!");
    }

    @Test
    public void testSPI() {
        ExecutorService executorService = (ExecutorService) ExtensionLoader.getExtensionLoader(ThreadPool.class)
                .getExtension("eager")
                .getExecutor(URL);
        Assertions.assertEquals("EagerThreadPoolExecutor", executorService.getClass()
            .getSimpleName(), "test spi fail!");
    }
}