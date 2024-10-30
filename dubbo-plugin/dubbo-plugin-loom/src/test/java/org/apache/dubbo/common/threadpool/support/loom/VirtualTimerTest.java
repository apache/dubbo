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

import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.TimerTask;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VirtualTimerTest {
    @Test
    void testThreadKind() {
        VirtualTimer virtualTimer = new VirtualTimer("test");
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicReference<Timeout> timeoutRef = new AtomicReference<>();
        TimerTask timerTask = timeout -> {
            if (!Thread.currentThread().isVirtual()) {
                throw new IllegalStateException("The thread should be virtual");
            }
            timeoutRef.set(timeout);
            executed.set(true);
        };
        virtualTimer.newTimeout(timerTask, 1, TimeUnit.MILLISECONDS);
        Awaitility.await().untilTrue(executed);

        Timeout timeout = timeoutRef.get();
        Assertions.assertEquals(virtualTimer, timeout.timer());
        Assertions.assertEquals(timerTask, timeout.task());
        Assertions.assertFalse(timeout.isExpired());
        Assertions.assertFalse(timeout.isCancelled());
    }

    @Test
    void testCancel() {
        VirtualTimer virtualTimer = new VirtualTimer("test");
        AtomicBoolean executed = new AtomicBoolean(false);
        TimerTask timerTask = timeout -> executed.set(true);
        Timeout timeout = virtualTimer.newTimeout(timerTask, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
        timeout.cancel();

        Assertions.assertTrue(timeout.isCancelled());
        Assertions.assertFalse(executed.get());

        Awaitility.await().until(() -> virtualTimer.getTimeouts().isEmpty());
        Set<Timeout> rested = virtualTimer.stop();
        Assertions.assertTrue(rested.isEmpty());
    }

    @Test
    void testStop() {
        VirtualTimer virtualTimer = new VirtualTimer("test");
        AtomicBoolean executed = new AtomicBoolean(false);
        TimerTask timerTask = timeout -> executed.set(true);
        Timeout timeout = virtualTimer.newTimeout(timerTask, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);

        Set<Timeout> timeouts = virtualTimer.stop();
        Assertions.assertEquals(Collections.singleton(timeout), timeouts);
        Assertions.assertFalse(executed.get());

        Assertions.assertTrue(virtualTimer.isStop());
        timeouts = virtualTimer.stop();
        Assertions.assertTrue(timeouts.isEmpty());
    }
}
