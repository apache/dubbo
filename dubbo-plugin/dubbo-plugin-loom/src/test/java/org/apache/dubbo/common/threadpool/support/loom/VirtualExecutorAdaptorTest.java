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

import org.apache.dubbo.common.timer.Timer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

public class VirtualExecutorAdaptorTest {
    @Test
    void testNewSingleThreadExecutorService() {
        ExecutorService executorService = new VirtualExecutorAdaptor().newSingleThreadExecutorService("test");
        AtomicBoolean executed = new AtomicBoolean(false);
        executorService.submit(() -> {
            if (Thread.currentThread().isVirtual()) {
                executed.set(true);
            }
        });
        Awaitility.await().untilTrue(executed);
    }

    @Test
    void testNewExecutorService() {
        ExecutorService executorService =
                new VirtualExecutorAdaptor().newExecutorService(1, 1, 0, null, null, "test", null);
        AtomicBoolean executed = new AtomicBoolean(false);
        executorService.submit(() -> {
            if (Thread.currentThread().isVirtual()) {
                executed.set(true);
            }
        });
        Awaitility.await().untilTrue(executed);
    }

    @Test
    void testNewScheduledExecutorService() {
        ExecutorService executorService = new VirtualExecutorAdaptor().newScheduledExecutorService(1, "test");
        AtomicBoolean executed = new AtomicBoolean(false);
        executorService.submit(() -> {
            if (Thread.currentThread().isVirtual()) {
                executed.set(true);
            }
        });
        Awaitility.await().untilTrue(executed);
    }

    @Test
    void testNewTimer() {
        Timer virtualTimer = new VirtualExecutorAdaptor().newTimer("test", 0, TimeUnit.SECONDS, 0, 0);
        AtomicBoolean executed = new AtomicBoolean(false);
        virtualTimer.newTimeout(
                timeout -> {
                    if (Thread.currentThread().isVirtual()) {
                        executed.set(true);
                    }
                },
                1,
                TimeUnit.MILLISECONDS);
        Awaitility.await().untilTrue(executed);
    }
}
