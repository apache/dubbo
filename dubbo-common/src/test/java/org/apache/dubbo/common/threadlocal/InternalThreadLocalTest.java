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

import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertTrue("set failed", internalThreadLocal.get() == 1);

        final InternalThreadLocal<String> internalThreadLocalString = new InternalThreadLocal<String>();
        internalThreadLocalString.set("value");
        Assert.assertTrue("set failed", "value".equals(internalThreadLocalString.get()));

        InternalThreadLocal.removeAll();
        Assert.assertTrue("removeAll failed!", internalThreadLocal.get() == null);
        Assert.assertTrue("removeAll failed!", internalThreadLocalString.get() == null);
    }

    @Test
    public void testSize() throws InterruptedException {
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(1);
        Assert.assertTrue("size method is wrong!", InternalThreadLocal.size() == 1);

        final InternalThreadLocal<String> internalThreadLocalString = new InternalThreadLocal<String>();
        internalThreadLocalString.set("value");
        Assert.assertTrue("size method is wrong!", InternalThreadLocal.size() == 2);
    }

    @Test
    public void testSetAndGet() {
        final Integer testVal = 10;
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(testVal);
        Assert.assertTrue("set is not equals get",
                Objects.equals(testVal, internalThreadLocal.get()));
    }

    @Test
    public void testRemove() {
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(1);
        Assert.assertTrue("get method false!", internalThreadLocal.get() == 1);

        internalThreadLocal.remove();
        Assert.assertTrue("remove failed!", internalThreadLocal.get() == null);
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
        Assert.assertTrue("get method false!", internalThreadLocal.get() == 1);

        internalThreadLocal.remove();
        Assert.assertTrue("onRemove method failed!", valueToRemove[0] == 2);
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
                Assert.assertTrue("set is not equals get",
                        Objects.equals(testVal1, internalThreadLocal.get()));
                countDownLatch.countDown();
            }
        });
        t1.start();

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                internalThreadLocal.set(testVal2);
                Assert.assertTrue("set is not equals get",
                        Objects.equals(testVal2, internalThreadLocal.get()));
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