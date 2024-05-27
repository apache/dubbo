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

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.apache.dubbo.common.constants.CommonConstants.QUEUES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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
}
