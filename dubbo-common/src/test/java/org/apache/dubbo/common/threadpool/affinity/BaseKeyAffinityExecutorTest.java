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

package org.apache.dubbo.common.threadpool.affinity;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public abstract class BaseKeyAffinityExecutorTest {

    protected static KeyAffinityExecutor<String> keyAffinityExecutor;

    @Test
    public void test() throws InterruptedException {
        int n = 5;
        int eachCount = 1000;
        int total = n * eachCount;
        int sleepMillis = 10;
        int[] count = new int[n];
        CountDownLatch downLatch = new CountDownLatch(total);
        for (int i = 0; i < total; i++) {
            final int val = i % n;
            Thread.sleep(ThreadLocalRandom.current().nextInt(sleepMillis));
            keyAffinityExecutor.execute(val + "", () -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(sleepMillis));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                downLatch.countDown();
                count[val]++;
            });
        }
        Assertions.assertTrue(downLatch.await(3, TimeUnit.SECONDS));
        for (int i : count) {
            Assertions.assertEquals(i, eachCount);
        }
    }


    @AfterAll
    public static void after() {
        keyAffinityExecutor.destroyAll();
    }


}