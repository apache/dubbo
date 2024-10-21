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
package org.apache.dubbo.common.utils;

import java.lang.Thread.State;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

import static org.awaitility.Awaitility.await;

public class LockUtilsTest {
    @RepeatedTest(5)
    void testLockFailed() {
        ReentrantLock reentrantLock = new ReentrantLock();
        AtomicBoolean releaseLock = new AtomicBoolean(false);
        new Thread(() -> {
                    reentrantLock.lock();
                    while (!releaseLock.get()) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    reentrantLock.unlock();
                })
                .start();

        await().until(reentrantLock::isLocked);

        AtomicLong lockTime = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        LockUtils.safeLock(reentrantLock, 1000, () -> {
            lockTime.set(System.currentTimeMillis());
        });
        Assertions.assertTrue(lockTime.get() - startTime >= 1000);
        releaseLock.set(true);

        while (reentrantLock.isLocked()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        lockTime.set(0);
        startTime = System.currentTimeMillis();
        LockUtils.safeLock(reentrantLock, 1000, () -> {
            lockTime.set(System.currentTimeMillis());
        });
        Assertions.assertTrue(lockTime.get() - startTime < 1000);
    }

    @RepeatedTest(5)
    void testReentrant() {
        ReentrantLock reentrantLock = new ReentrantLock();
        reentrantLock.lock();

        AtomicLong lockTime = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        LockUtils.safeLock(reentrantLock, 1000, () -> {
            lockTime.set(System.currentTimeMillis());
        });
        Assertions.assertTrue(lockTime.get() - startTime < 1000);

        reentrantLock.lock();
        lockTime.set(0);
        startTime = System.currentTimeMillis();
        LockUtils.safeLock(reentrantLock, 1000, () -> {
            lockTime.set(System.currentTimeMillis());
        });
        Assertions.assertTrue(lockTime.get() - startTime < 1000);

        Assertions.assertTrue(reentrantLock.isLocked());
        reentrantLock.unlock();
        Assertions.assertTrue(reentrantLock.isLocked());
        reentrantLock.unlock();
        Assertions.assertFalse(reentrantLock.isLocked());
    }

    @RepeatedTest(5)
    void testInterrupt() {
        ReentrantLock reentrantLock = new ReentrantLock();
        reentrantLock.lock();

        AtomicBoolean locked = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            LockUtils.safeLock(reentrantLock, 10000, () -> {
                locked.set(true);
            });
        });
        thread.start();

        await().until(() -> thread.getState() == State.TIMED_WAITING);
        thread.interrupt();
        await().until(() -> thread.getState() == State.TERMINATED);

        Assertions.assertTrue(locked.get());

        reentrantLock.unlock();
    }

    @RepeatedTest(5)
    void testHoldLock() throws InterruptedException {
        ReentrantLock reentrantLock = new ReentrantLock();
        reentrantLock.lock();

        AtomicLong lockTime = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        Thread thread = new Thread(() -> {
            LockUtils.safeLock(reentrantLock, 10000, () -> {
                lockTime.set(System.currentTimeMillis());
            });
        });
        thread.start();

        await().until(() -> thread.getState() == State.TIMED_WAITING);
        Thread.sleep(1000);
        reentrantLock.unlock();

        await().until(() -> thread.getState() == State.TERMINATED);
        Assertions.assertTrue(lockTime.get() - startTime > 1000);
        Assertions.assertTrue(lockTime.get() - startTime < 10000);
    }

    @RepeatedTest(5)
    void testInterrupted() throws InterruptedException {
        ReentrantLock reentrantLock = new ReentrantLock();
        reentrantLock.lock();

        AtomicLong lockTime = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        Thread thread = new Thread(() -> {
            Thread.currentThread().interrupt();
            LockUtils.safeLock(reentrantLock, 10000, () -> {
                lockTime.set(System.currentTimeMillis());
            });
        });
        thread.start();

        await().until(() -> thread.getState() == State.TERMINATED);
        Assertions.assertTrue(lockTime.get() >= startTime);
        Assertions.assertTrue(lockTime.get() - startTime < 10000);
    }
}
