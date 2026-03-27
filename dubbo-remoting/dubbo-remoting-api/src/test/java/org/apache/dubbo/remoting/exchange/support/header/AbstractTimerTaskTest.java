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
package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.remoting.Channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.remoting.Constants.HEARTBEAT_CHECK_TICK;

class AbstractTimerTaskTest {

    private HashedWheelTimer timer;
    private MockChannel channel;
    private AtomicInteger taskExecutionCount;

    @BeforeEach
    public void setup() {
        long tickDuration = 1000;
        timer = new HashedWheelTimer(tickDuration / HEARTBEAT_CHECK_TICK, TimeUnit.MILLISECONDS);
        channel = new MockChannel() {
            @Override
            public URL getUrl() {
                return URL.valueOf("dubbo://localhost:20880");
            }
        };
        taskExecutionCount = new AtomicInteger(0);
    }

    @AfterEach
    public void teardown() {
        timer.stop();
    }

    @Test
    void testAutoCancelWhenAllChannelsClosed() throws Exception {
        long tick = 1000 / HEARTBEAT_CHECK_TICK;
        AbstractTimerTask task = new AbstractTimerTask(
                () -> Collections.singleton(channel), timer, tick) {
            @Override
            protected void doTask(Channel channel) {
                taskExecutionCount.incrementAndGet();
            }
        };
        task.start();

        // Let the task run a few times while channel is open
        Thread.sleep(1500L);
        Assertions.assertTrue(taskExecutionCount.get() > 0, "Task should have executed at least once");

        int countBeforeClose = taskExecutionCount.get();

        // Close the channel
        channel.close();

        // Wait for the task to detect closure and auto-cancel
        Thread.sleep(1500L);

        int countAfterClose = taskExecutionCount.get();

        // Task should not have executed after channel was closed
        Assertions.assertEquals(countBeforeClose, countAfterClose,
                "Task should not execute after all channels are closed");

        // Verify the task was cancelled
        Assertions.assertTrue(task.cancel, "Task should be cancelled when all channels are closed");
    }

    @Test
    void testTaskContinuesWhenChannelIsOpen() throws Exception {
        long tick = 1000 / HEARTBEAT_CHECK_TICK;
        AbstractTimerTask task = new AbstractTimerTask(
                () -> Collections.singleton(channel), timer, tick) {
            @Override
            protected void doTask(Channel channel) {
                taskExecutionCount.incrementAndGet();
            }
        };
        task.start();

        Thread.sleep(2000L);

        Assertions.assertTrue(taskExecutionCount.get() > 1,
                "Task should keep executing when channel is open");
        Assertions.assertFalse(task.cancel, "Task should not be cancelled when channel is open");

        task.cancel();
    }

    @Test
    void testTaskNotCancelledWhenChannelCollectionIsEmpty() throws Exception {
        long tick = 1000 / HEARTBEAT_CHECK_TICK;
        // Server-side scenario: ChannelProvider returns empty collection when no clients are connected
        AbstractTimerTask task = new AbstractTimerTask(
                ArrayList::new, timer, tick) {
            @Override
            protected void doTask(Channel channel) {
                taskExecutionCount.incrementAndGet();
            }
        };
        task.start();

        // Let the task run several ticks with empty channel collection
        Thread.sleep(2000L);

        // Task should NOT be cancelled — empty collection is not the same as all-closed
        Assertions.assertFalse(task.cancel,
                "Task should not be cancelled when channel collection is empty");
        Assertions.assertEquals(0, taskExecutionCount.get(),
                "doTask should not be called when there are no channels");

        task.cancel();
    }
}
