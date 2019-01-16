/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.threadpool.support;

import org.apache.dubbo.common.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


public class AbortPolicyWithReportTest {

    private static Integer val = 0;
    @Test
    public void jStackDumpTest() throws InterruptedException {
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory=/tmp&version=1.0.0&application=morgan&noValue");
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url);

        try {
            abortPolicyWithReport.rejectedExecution(new Runnable() {
                @Override
                public void run() {
                    System.out.println("hello");
                }
            }, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
        } catch (RejectedExecutionException rj) {
            // ignore
        }

        Thread.sleep(1000);

    }


    @Test
    public void testThreadPoolShutDown() throws InterruptedException {
        Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.execute(() -> {
            try {
                //Simulated Time consuming calculation
                TimeUnit.SECONDS.sleep(5);
                val = 10;
                System.out.println("计算完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        });
        pool.shutdown();
        semaphore.acquire();
        Assertions.assertEquals(val, 10);
    }


}