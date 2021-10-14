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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class SerializingExecutorTest {

    protected static SerializingExecutor serializingExecutor;

    @BeforeAll
    public static void before() {
        ExecutorService service = Executors.newFixedThreadPool(4);
        serializingExecutor = new SerializingExecutor(service);
    }

    @Test
    public void test1() throws InterruptedException {
        int n = 2;
        int eachCount = 1000;
        int total = n * eachCount;
        int sleepMillis = 10;
        Map<String, Integer> map = new HashMap<>();
        map.put("val", 0);
        CountDownLatch downLatch = new CountDownLatch(total);
        for (int i = 0; i < total; i++) {
            final int index = i;
            Thread.sleep(ThreadLocalRandom.current().nextInt(sleepMillis));
            serializingExecutor.execute(() -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(sleepMillis));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int num = map.get("val");
                map.put("val", num + 1);
                downLatch.countDown();
                Assertions.assertEquals(num, index);
            });
        }
        downLatch.await(3, TimeUnit.SECONDS);
        Assertions.assertEquals(total, map.get("val"));
    }
}