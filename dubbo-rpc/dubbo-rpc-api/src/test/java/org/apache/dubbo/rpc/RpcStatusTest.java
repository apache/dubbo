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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.support.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link RpcStatus}
 */
public class RpcStatusTest {

    @Test
    public void testBeginCountEndCount() {
        URL url = new ServiceConfigURL("dubbo", "127.0.0.1", 91031, DemoService.class.getName());
        String methodName = "testBeginCountEndCount";
        int max = 2;
        boolean flag = RpcStatus.beginCount(url, methodName, max);
        RpcStatus urlRpcStatus = RpcStatus.getStatus(url);
        RpcStatus methodRpcStatus = RpcStatus.getStatus(url, methodName);

        Assertions.assertTrue(flag);
        Assertions.assertNotNull(urlRpcStatus);
        Assertions.assertNotNull(methodRpcStatus);
        Assertions.assertEquals(urlRpcStatus.getActive(), 1);
        Assertions.assertEquals(methodRpcStatus.getActive(), 1);

        RpcStatus.endCount(url, methodName, 1000, true);
        Assertions.assertEquals(urlRpcStatus.getActive(), 0);
        Assertions.assertEquals(methodRpcStatus.getActive(), 0);

        flag = RpcStatus.beginCount(url, methodName, max);
        Assertions.assertTrue(flag);
        flag = RpcStatus.beginCount(url, methodName, max);
        Assertions.assertTrue(flag);
        flag = RpcStatus.beginCount(url, methodName, max);
        Assertions.assertFalse(flag);

    }

    @Test
    public void testBeginCountEndCountInMultiThread() throws Exception {
        URL url = new ServiceConfigURL("dubbo", "127.0.0.1", 91032, DemoService.class.getName());
        String methodName = "testBeginCountEndCountInMultiThread";
        int max = 50;
        int threadNum = 10;
        AtomicInteger successCount = new AtomicInteger();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadNum);
        List<Thread> threadList = new ArrayList<>(threadNum);
        for (int i = 0; i < threadNum; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        boolean flag = RpcStatus.beginCount(url, methodName, max);
                        if (flag) {
                            successCount.incrementAndGet();
                        }
                    }
                    endLatch.countDown();
                } catch (Exception e) {
                    // ignore
                }
            });
            threadList.add(thread);
        }
        threadList.forEach(Thread::start);
        startLatch.countDown();
        endLatch.await();
        Assertions.assertEquals(successCount.get(), max);

    }

    @Test
    public void testStatistics() {
        URL url = new ServiceConfigURL("dubbo", "127.0.0.1", 91033, DemoService.class.getName());
        String methodName = "testStatistics";
        int max = 0;
        RpcStatus.beginCount(url, methodName, max);
        RpcStatus.beginCount(url, methodName, max);
        RpcStatus.beginCount(url, methodName, max);
        RpcStatus.beginCount(url, methodName, max);
        RpcStatus.endCount(url, methodName, 1000, true);
        RpcStatus.endCount(url, methodName, 2000, true);
        RpcStatus.endCount(url, methodName, 3000, false);
        RpcStatus.endCount(url, methodName, 4000, false);

        RpcStatus urlRpcStatus = RpcStatus.getStatus(url);
        RpcStatus methodRpcStatus = RpcStatus.getStatus(url, methodName);
        for (RpcStatus rpcStatus : Arrays.asList(urlRpcStatus, methodRpcStatus)) {
            Assertions.assertEquals(rpcStatus.getActive(), 0);
            Assertions.assertEquals(rpcStatus.getTotal(), 4);
            Assertions.assertEquals(rpcStatus.getTotalElapsed(), 10000);
            Assertions.assertEquals(rpcStatus.getMaxElapsed(), 4000);
            Assertions.assertEquals(rpcStatus.getAverageElapsed(), 2500);
            Assertions.assertEquals(rpcStatus.getAverageTps(), 0);

            Assertions.assertEquals(rpcStatus.getSucceeded(), 2);
            Assertions.assertEquals(rpcStatus.getSucceededElapsed(), 3000);
            Assertions.assertEquals(rpcStatus.getSucceededMaxElapsed(), 2000);
            Assertions.assertEquals(rpcStatus.getSucceededAverageElapsed(), 1500);

            Assertions.assertEquals(rpcStatus.getFailed(), 2);
            Assertions.assertEquals(rpcStatus.getFailedElapsed(), 7000);
            Assertions.assertEquals(rpcStatus.getFailedMaxElapsed(), 4000);
            Assertions.assertEquals(rpcStatus.getFailedAverageElapsed(), 3500);
        }
    }
}
