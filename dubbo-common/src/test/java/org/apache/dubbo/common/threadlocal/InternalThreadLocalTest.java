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

package org.apache.dubbo.common.threadlocal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class InternalThreadLocalTest {

    private static final int THREADS = 10;

    private static final int PERFORMANCE_THREAD_COUNT = 1000;

    private static final int GET_COUNT = 1000000;

    @Test
    public void testInternalThreadLocal() throws InterruptedException {
        final AtomicInteger index = new AtomicInteger(0);

        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>() {

            @Override
            protected Integer initialValue() throws Exception {
                Integer v = index.getAndIncrement();
                System.out.println("thread : " + Thread.currentThread().getName() + " init value : " + v);
                return v;
            }
        };

        for (int i = 0; i < THREADS; i++) {
            Thread t = new Thread(internalThreadLocal::get);
            t.start();
        }

        Thread.sleep(2000);
    }

    @Test
    public void testRemoveAll() throws InterruptedException {
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(1);
        Assertions.assertTrue(internalThreadLocal.get() == 1, "set failed");

        final InternalThreadLocal<String> internalThreadLocalString = new InternalThreadLocal<String>();
        internalThreadLocalString.set("value");
        Assertions.assertTrue("value".equals(internalThreadLocalString.get()), "set failed");

        InternalThreadLocal.removeAll();
        Assertions.assertTrue(internalThreadLocal.get() == null, "removeAll failed!");
        Assertions.assertTrue(internalThreadLocalString.get() == null, "removeAll failed!");
    }

    @Test
    public void testSize() throws InterruptedException {
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(1);
        Assertions.assertTrue(InternalThreadLocal.size() == 1, "size method is wrong!");

        final InternalThreadLocal<String> internalThreadLocalString = new InternalThreadLocal<String>();
        internalThreadLocalString.set("value");
        Assertions.assertTrue(InternalThreadLocal.size() == 2, "size method is wrong!");
    }

    @Test
    public void testSetAndGet() {
        final Integer testVal = 10;
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(testVal);
        Assertions.assertTrue(
                Objects.equals(testVal, internalThreadLocal.get()), "set is not equals get");
    }

    @Test
    public void testRemove() {
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(1);
        Assertions.assertTrue(internalThreadLocal.get() == 1, "get method false!");

        internalThreadLocal.remove();
        Assertions.assertTrue(internalThreadLocal.get() == null, "remove failed!");
    }

    @Test
    public void testOnRemove() {
        final Integer[] valueToRemove = {null};
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>() {
            @Override
            protected void onRemoval(Integer value) throws Exception {
                //value calculate
                valueToRemove[0] = value + 1;
            }
        };
        internalThreadLocal.set(1);
        Assertions.assertTrue(internalThreadLocal.get() == 1, "get method false!");

        internalThreadLocal.remove();
        Assertions.assertTrue(valueToRemove[0] == 2, "onRemove method failed!");
    }

    @Test
    public void testMultiThreadSetAndGet() throws InterruptedException {
        final Integer testVal1 = 10;
        final Integer testVal2 = 20;
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {

                internalThreadLocal.set(testVal1);
                Assertions.assertTrue(
                        Objects.equals(testVal1, internalThreadLocal.get()), "set is not equals get");
                countDownLatch.countDown();
            }
        });
        t1.start();

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                internalThreadLocal.set(testVal2);
                Assertions.assertTrue(
                        Objects.equals(testVal2, internalThreadLocal.get()), "set is not equals get");
                countDownLatch.countDown();
            }
        });
        t2.start();
        countDownLatch.await();
    }

    /**
     * print
     * take[2689]ms
     * <p></p>
     * This test is based on a Machine with 4 core and 16g memory.
     */
    @Test
    public void testPerformanceTradition() {
        final ThreadLocal<String>[] caches1 = new ThreadLocal[PERFORMANCE_THREAD_COUNT];
        final Thread mainThread = Thread.currentThread();
        for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
            caches1[i] = new ThreadLocal<String>();
        }
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                    caches1[i].set("float.lu");
                }
                long start = System.nanoTime();
                for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                    for (int j = 0; j < GET_COUNT; j++) {
                        caches1[i].get();
                    }
                }
                long end = System.nanoTime();
                System.out.println("take[" + TimeUnit.NANOSECONDS.toMillis(end - start) +
                        "]ms");
                LockSupport.unpark(mainThread);
            }
        });
        t1.start();
        LockSupport.park(mainThread);
    }

    /**
     * print
     * take[14]ms
     * <p></p>
     * This test is based on a Machine with 4 core and 16g memory.
     */
    @Test
    public void testPerformance() {
        final InternalThreadLocal<String>[] caches = new InternalThreadLocal[PERFORMANCE_THREAD_COUNT];
        final Thread mainThread = Thread.currentThread();
        for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
            caches[i] = new InternalThreadLocal<String>();
        }
        Thread t = new InternalThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                    caches[i].set("float.lu");
                }
                long start = System.nanoTime();
                for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                    for (int j = 0; j < GET_COUNT; j++) {
                        caches[i].get();
                    }
                }
                long end = System.nanoTime();
                System.out.println("take[" + TimeUnit.NANOSECONDS.toMillis(end - start) +
                        "]ms");
                LockSupport.unpark(mainThread);
            }
        });
        t.start();
        LockSupport.park(mainThread);
    }
}