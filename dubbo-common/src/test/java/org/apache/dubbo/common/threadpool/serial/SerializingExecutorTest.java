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

package org.apache.dubbo.common.threadpool.serial;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

class SerializingExecutorTest {

    private ExecutorService service;
    private SerializingExecutor serializingExecutor;

    @BeforeEach
    public void before() {
        service = Executors.newFixedThreadPool(4);
        serializingExecutor = new SerializingExecutor(service);
    }

    @Test
    void testSerial() throws InterruptedException {
        int total = 10000;

        Map<String, Integer> map = new HashMap<>();
        map.put("val", 0);

        Semaphore semaphore = new Semaphore(1);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);

        for (int i = 0; i < total; i++) {
            final int index = i;
            serializingExecutor.execute(() -> {
                if (!semaphore.tryAcquire()) {
                    System.out.println("Concurrency");
                    failed.set(true);
                }
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                int num = map.get("val");
                map.put("val", num + 1);
                if (num != index) {
                    System.out.println("Index error. Excepted :" + index + " but actual: " + num);
                    failed.set(true);
                }
                semaphore.release();
            });
        }

        startLatch.countDown();
        await().until(() -> map.get("val") == total);
        Assertions.assertFalse(failed.get());
    }

    @Test
    void testNonSerial() {
        int total = 10;

        Map<String, Integer> map = new HashMap<>();
        map.put("val", 0);

        Semaphore semaphore = new Semaphore(1);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);

        for (int i = 0; i < total; i++) {
            final int index = i;
            service.execute(() -> {
                if (!semaphore.tryAcquire()) {
                    failed.set(true);
                }
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                int num = map.get("val");
                map.put("val", num + 1);
                if (num != index) {
                    failed.set(true);
                }
                semaphore.release();
            });
        }

        await().until(() -> ((ThreadPoolExecutor) service).getActiveCount() == 4);
        startLatch.countDown();
        await().until(() -> ((ThreadPoolExecutor) service).getCompletedTaskCount() == total);
        Assertions.assertTrue(failed.get());
    }
}
