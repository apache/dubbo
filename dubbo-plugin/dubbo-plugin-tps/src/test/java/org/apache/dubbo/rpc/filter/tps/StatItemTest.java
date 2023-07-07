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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatItemTest {

    private StatItem statItem;

    @AfterEach
    public void tearDown() throws Exception {
        statItem = null;
    }

    @Test
    void testIsAllowable() throws Exception {
        statItem = new StatItem("test", 5, 1000L);
        long lastResetTime = statItem.getLastResetTime();
        assertTrue(statItem.isAllowable());
        Thread.sleep(1100L);
        assertTrue(statItem.isAllowable());
        assertTrue(lastResetTime != statItem.getLastResetTime());
        assertEquals(4, statItem.getToken());
    }

    @Test
    void testAccuracy() throws Exception {
        final int EXPECTED_RATE = 5;
        statItem = new StatItem("test", EXPECTED_RATE, 60_000L);
        for (int i = 1; i <= EXPECTED_RATE; i++) {
            assertTrue(statItem.isAllowable());
        }

        // Must block the 6th item
        assertFalse(statItem.isAllowable());
    }

    @Test
    void testConcurrency() throws Exception {
        statItem = new StatItem("test", 100, 100000);

        List<Task> taskList = new ArrayList<>();
        int threadNum = 50;
        CountDownLatch stopLatch = new CountDownLatch(threadNum);
        CountDownLatch startLatch = new CountDownLatch(1);
        for (int i = 0; i < threadNum; i++) {
            taskList.add(new Task(statItem, startLatch, stopLatch));

        }
        startLatch.countDown();
        stopLatch.await();

        Assertions.assertEquals(taskList.stream().map(Task::getCount).reduce(Integer::sum).get(), 100);
    }


    static class Task implements Runnable {
        private final StatItem statItem;
        private final CountDownLatch startLatch;
        private final CountDownLatch stopLatch;
        private int count;

        public Task(StatItem statItem, CountDownLatch startLatch, CountDownLatch stopLatch) {
            this.statItem = statItem;
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
                count = statItem.isAllowable() ? count + 1 : count;
            }
            stopLatch.countDown();
        }

        public int getCount() {
            return count;
        }
    }
}
