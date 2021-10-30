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
package org.apache.dubbo.rpc.filter.tps;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.support.MockInvocation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.rpc.Constants.TPS_LIMIT_INTERVAL_KEY;
import static org.apache.dubbo.rpc.Constants.TPS_LIMIT_RATE_KEY;

public class DefaultTPSLimiterTest {

    private static final int TEST_LIMIT_RATE = 2;
    private final DefaultTPSLimiter defaultTPSLimiter = new DefaultTPSLimiter();

    @Test
    public void testIsAllowable() throws Exception {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(TPS_LIMIT_RATE_KEY, TEST_LIMIT_RATE);
        url = url.addParameter(TPS_LIMIT_INTERVAL_KEY, 1000);
        for (int i = 1; i <= TEST_LIMIT_RATE; i++) {
            Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
        }
    }

    @Test
    public void testIsNotAllowable() throws Exception {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(TPS_LIMIT_RATE_KEY, TEST_LIMIT_RATE);
        url = url.addParameter(TPS_LIMIT_INTERVAL_KEY, 1000);
        for (int i = 1; i <= TEST_LIMIT_RATE + 1; i++) {
            if (i == TEST_LIMIT_RATE + 1) {
                Assertions.assertFalse(defaultTPSLimiter.isAllowable(url, invocation));
            } else {
                Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
            }
        }
    }

    @Test
    public void testTPSLimiterForMethodLevelConfig() throws Exception {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(TPS_LIMIT_RATE_KEY, TEST_LIMIT_RATE);
        int tpsConfigForMethodLevel = 3;
        url = url.addParameter("echo.tps", tpsConfigForMethodLevel);
        url = url.addParameter(TPS_LIMIT_INTERVAL_KEY, 1000);
        for (int i = 1; i <= tpsConfigForMethodLevel + 1; i++) {
            if (i == tpsConfigForMethodLevel + 1) {
                Assertions.assertFalse(defaultTPSLimiter.isAllowable(url, invocation));
            } else {
                Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
            }
        }
    }

    @Test
    public void testConfigChange() throws Exception {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(TPS_LIMIT_RATE_KEY, TEST_LIMIT_RATE);
        url = url.addParameter(TPS_LIMIT_INTERVAL_KEY, 1000);
        for (int i = 1; i <= TEST_LIMIT_RATE; i++) {
            Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
        }
        final int tenTimesLimitRate = TEST_LIMIT_RATE * 10;
        url = url.addParameter(TPS_LIMIT_RATE_KEY, tenTimesLimitRate);
        for (int i = 1; i <= tenTimesLimitRate; i++) {
            Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
        }

        Assertions.assertFalse(defaultTPSLimiter.isAllowable(url, invocation));
    }

    @Test
    public void testMultiThread() throws InterruptedException {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(TPS_LIMIT_RATE_KEY, 100);
        url = url.addParameter(TPS_LIMIT_INTERVAL_KEY, 100000);

        List<Task> taskList = new ArrayList<>();
        int threadNum = 50;
        CountDownLatch stopLatch = new CountDownLatch(threadNum);
        CountDownLatch startLatch = new CountDownLatch(1);
        for (int i = 0; i < threadNum; i++) {
            taskList.add(new Task(defaultTPSLimiter, url, invocation, startLatch, stopLatch));

        }
        startLatch.countDown();
        stopLatch.await();

        Assertions.assertEquals(taskList.stream().map(Task::getCount).reduce(Integer::sum).get(), 100);
    }

    static class Task implements Runnable {
        private final DefaultTPSLimiter defaultTPSLimiter;
        private final URL url;
        private final Invocation invocation;
        private final CountDownLatch startLatch;
        private final CountDownLatch stopLatch;
        private int count;

        public Task(DefaultTPSLimiter defaultTPSLimiter, URL url, Invocation invocation, CountDownLatch startLatch, CountDownLatch stopLatch) {
            this.defaultTPSLimiter = defaultTPSLimiter;
            this.url = url;
            this.invocation = invocation;
            this.startLatch = startLatch;
            this.stopLatch = stopLatch;
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int j = 0; j < 10000; j++) {
                count = defaultTPSLimiter.isAllowable(url, invocation) ? count + 1 : count;
            }
            stopLatch.countDown();
        }

        public int getCount() {
            return count;
        }
    }
}
