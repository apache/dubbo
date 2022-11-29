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

package org.apache.dubbo.common.timer;

import org.apache.dubbo.common.utils.NamedThreadFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

class HashedWheelTimerTest {
    private CountDownLatch tryStopTaskCountDownLatch = new CountDownLatch(1);
    private CountDownLatch errorTaskCountDownLatch = new CountDownLatch(1);

    private static class EmptyTask implements TimerTask {
        @Override
        public void run(Timeout timeout) {
        }
    }

    private static class BlockTask implements TimerTask {
        @Override
        public void run(Timeout timeout) throws InterruptedException {
            this.wait();
        }
    }

    private class ErrorTask implements TimerTask {
        @Override
        public void run(Timeout timeout) {
            errorTaskCountDownLatch.countDown();
            throw new RuntimeException("Test");
        }
    }

    private class TryStopTask implements TimerTask {
        private Timer timer;

        public TryStopTask(Timer timer) {
            this.timer = timer;
        }

        @Override
        public void run(Timeout timeout) {
            Assertions.assertThrows(RuntimeException.class, () -> timer.stop());
            tryStopTaskCountDownLatch.countDown();
        }
    }

    @Test
    void constructorTest() {
        // use weak reference to let gc work every time
        // which can check finalize method and reduce memory usage in time
        WeakReference<Timer> timer = new WeakReference<>(new HashedWheelTimer());
        timer = new WeakReference<>(new HashedWheelTimer(100, TimeUnit.MILLISECONDS));
        timer = new WeakReference<>(new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 8));

        // to cover arg check branches
        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(
                    null,
                    100,
                    TimeUnit.MILLISECONDS,
                    8, -1);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(
                    new NamedThreadFactory("dubbo-future-timeout", true),
                    0,
                    TimeUnit.MILLISECONDS,
                    8, -1);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(
                    new NamedThreadFactory("dubbo-future-timeout", true),
                    100,
                    null,
                    8, -1);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(
                    new NamedThreadFactory("dubbo-future-timeout", true),
                    100,
                    TimeUnit.MILLISECONDS,
                    0, -1);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(
                    new NamedThreadFactory("dubbo-future-timeout", true),
                    Long.MAX_VALUE,
                    TimeUnit.MILLISECONDS,
                    8, -1);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(
                    new NamedThreadFactory("dubbo-future-timeout", true),
                    100,
                    TimeUnit.MILLISECONDS,
                    Integer.MAX_VALUE, -1);
        });

        for (int i = 0; i < 128; i++) {
            // to trigger INSTANCE_COUNT_LIMIT
            timer = new WeakReference<>(new HashedWheelTimer());
        }

        System.gc();
    }

    @Test
    void createTaskTest() throws InterruptedException {
        HashedWheelTimer timer = new HashedWheelTimer(
                new NamedThreadFactory("dubbo-future-timeout", true),
                10,
                TimeUnit.MILLISECONDS,
                8, 8);

        EmptyTask emptyTask = new EmptyTask();
        Assertions.assertThrows(RuntimeException.class,
                () -> timer.newTimeout(null, 5, TimeUnit.SECONDS));
        Assertions.assertThrows(RuntimeException.class,
                () -> timer.newTimeout(emptyTask, 5, null));

        Timeout timeout = timer.newTimeout(new ErrorTask(), 10, TimeUnit.MILLISECONDS);
        errorTaskCountDownLatch.await();
        Assertions.assertFalse(timeout.cancel());
        Assertions.assertFalse(timeout.isCancelled());
        Assertions.assertNotNull(timeout.toString());
        Assertions.assertEquals(timeout.timer(), timer);

        timeout = timer.newTimeout(emptyTask, 1000, TimeUnit.SECONDS);
        timeout.cancel();
        Assertions.assertTrue(timeout.isCancelled());

        List<Timeout> timeouts = new LinkedList<>();
        BlockTask blockTask = new BlockTask();
        while (timer.pendingTimeouts() < 8) {
            // to trigger maxPendingTimeouts
            timeout = timer.newTimeout(blockTask, -1, TimeUnit.MILLISECONDS);
            timeouts.add(timeout);
            Assertions.assertNotNull(timeout.toString());
        }
        Assertions.assertEquals(8, timer.pendingTimeouts());

        // this will throw an exception because of maxPendingTimeouts
        Assertions.assertThrows(RuntimeException.class,
                () -> timer.newTimeout(blockTask, 1, TimeUnit.MILLISECONDS));

        Timeout secondTimeout = timeouts.get(2);
        // wait until the task expired
        await().until(secondTimeout::isExpired);

        timer.stop();
    }

    @Test
    void stopTaskTest() throws InterruptedException {
        Timer timer = new HashedWheelTimer(new NamedThreadFactory("dubbo-future-timeout", true));
        timer.newTimeout(new TryStopTask(timer), 10, TimeUnit.MILLISECONDS);
        tryStopTaskCountDownLatch.await();

        for (int i = 0; i < 8; i++) {
            timer.newTimeout(new EmptyTask(), 0, TimeUnit.SECONDS);
        }
        // stop timer
        timer.stop();
        Assertions.assertTrue(timer.isStop());

        // this will throw an exception
        Assertions.assertThrows(RuntimeException.class,
                () -> timer.newTimeout(new EmptyTask(), 5, TimeUnit.SECONDS));

    }
}
