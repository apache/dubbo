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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InternalThreadLocalTest {

    private static final int THREADS = 10;

    private static final int PERFORMANCE_THREAD_COUNT = 1000;

    private static final int GET_COUNT = 1000000;

    @AfterEach
    public void setup() {
        InternalThreadLocalMap.remove();
    }

    @Test
    void testInternalThreadLocal() {
        final AtomicInteger index = new AtomicInteger(0);

        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>() {

            @Override
            protected Integer initialValue() {
                Integer v = index.getAndIncrement();
                System.out.println("thread : " + Thread.currentThread().getName() + " init value : " + v);
                return v;
            }
        };

        for (int i = 0; i < THREADS; i++) {
            Thread t = new Thread(internalThreadLocal::get);
            t.start();
        }

        await().until(index::get, is(THREADS));
    }

    @Test
    void testRemoveAll() {
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(1);
        Assertions.assertEquals(1, (int)internalThreadLocal.get(), "set failed");

        final InternalThreadLocal<String> internalThreadLocalString = new InternalThreadLocal<String>();
        internalThreadLocalString.set("value");
        Assertions.assertEquals("value", internalThreadLocalString.get(), "set failed");

        InternalThreadLocal.removeAll();
        Assertions.assertNull(internalThreadLocal.get(), "removeAll failed!");
        Assertions.assertNull(internalThreadLocalString.get(), "removeAll failed!");
    }

    @Test
    void testSize() {
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(1);
        Assertions.assertEquals(1, InternalThreadLocal.size(), "size method is wrong!");

        final InternalThreadLocal<String> internalThreadLocalString = new InternalThreadLocal<String>();
        internalThreadLocalString.set("value");
        Assertions.assertEquals(2, InternalThreadLocal.size(), "size method is wrong!");
        InternalThreadLocal.removeAll();
    }

    @Test
    void testSetAndGet() {
        final Integer testVal = 10;
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(testVal);
        Assertions.assertEquals(testVal, internalThreadLocal.get(), "set is not equals get");
    }

    @Test
    void testRemove() {
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(1);
        Assertions.assertEquals(1, (int)internalThreadLocal.get(), "get method false!");

        internalThreadLocal.remove();
        Assertions.assertNull(internalThreadLocal.get(), "remove failed!");
    }

    @Test
    void testOnRemove() {
        final Integer[] valueToRemove = {null};
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>() {
            @Override
            protected void onRemoval(Integer value) {
                //value calculate
                valueToRemove[0] = value + 1;
            }
        };
        internalThreadLocal.set(1);
        Assertions.assertEquals(1, (int)internalThreadLocal.get(), "get method false!");

        internalThreadLocal.remove();
        Assertions.assertEquals(2, (int)valueToRemove[0], "onRemove method failed!");
    }

    @Test
    void testMultiThreadSetAndGet() throws InterruptedException {
        final Integer testVal1 = 10;
        final Integer testVal2 = 20;
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {

                internalThreadLocal.set(testVal1);
                Assertions.assertEquals(testVal1, internalThreadLocal.get(), "set is not equals get");
                countDownLatch.countDown();
            }
        });
        t1.start();

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                internalThreadLocal.set(testVal2);
                Assertions.assertEquals(testVal2, internalThreadLocal.get(), "set is not equals get");
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
    void testPerformanceTradition() {
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
    void testPerformance() {
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

    @Test
    void testConstructionWithIndex() throws Exception {
        // reset ARRAY_LIST_CAPACITY_MAX_SIZE to speed up
        int NEW_ARRAY_LIST_CAPACITY_MAX_SIZE = 8;
        Field nextIndexField =
            InternalThreadLocalMap.class.getDeclaredField("NEXT_INDEX");

        nextIndexField.setAccessible(true);
        AtomicInteger nextIndex = (AtomicInteger) nextIndexField.get(AtomicInteger.class);
        int arrayListCapacityMaxSize = InternalThreadLocalMap.ARRAY_LIST_CAPACITY_MAX_SIZE;
        int nextIndex_before = nextIndex.get();
        nextIndex.set(0);
        final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
        try {
            InternalThreadLocalMap.ARRAY_LIST_CAPACITY_MAX_SIZE =  NEW_ARRAY_LIST_CAPACITY_MAX_SIZE;
            while (nextIndex.get() < NEW_ARRAY_LIST_CAPACITY_MAX_SIZE) {
                new InternalThreadLocal<Boolean>();
            }
            assertEquals(NEW_ARRAY_LIST_CAPACITY_MAX_SIZE - 1, InternalThreadLocalMap.lastVariableIndex());
            try {
                new InternalThreadLocal<Boolean>();
            } catch (Throwable t) {
                throwable.set(t);
            }
            // Assert the max index cannot greater than (ARRAY_LIST_CAPACITY_MAX_SIZE - 1)
            assertThat(throwable.get(), is(instanceOf(IllegalStateException.class)));
            // Assert the index was reset to ARRAY_LIST_CAPACITY_MAX_SIZE after it reaches ARRAY_LIST_CAPACITY_MAX_SIZE
            assertEquals(NEW_ARRAY_LIST_CAPACITY_MAX_SIZE - 1, InternalThreadLocalMap.lastVariableIndex());
        } finally {
            // Restore the index
            nextIndex.set(nextIndex_before);
            InternalThreadLocalMap.ARRAY_LIST_CAPACITY_MAX_SIZE = arrayListCapacityMaxSize;
        }
    }

    @Test
    void testInternalThreadLocalMapExpand() throws Exception {
        final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int expand_threshold = 1 << 30;
                try {
                    InternalThreadLocalMap.get().setIndexedVariable(expand_threshold, null);
                } catch (Throwable t) {
                    throwable.set(t);
                }
            }
        };
        InternalThread internalThread = new InternalThread(runnable);
        internalThread.start();
        internalThread.join();
        // Assert the expanded size is not overflowed to negative value
        assertThat(throwable.get(), is(not(instanceOf(NegativeArraySizeException.class))));
    }
}
