package com.alibaba.dubbo.common.threadlocal;

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

    @Test
    public void testInternalThreadLocal() throws InterruptedException {
        AtomicInteger index = new AtomicInteger(0);

        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>() {

            @Override
            protected Integer initialValue() throws Exception {
                Integer v = index.getAndIncrement();
                System.out.println("thread : " + Thread.currentThread().getName() + " init value : " + v);
                return v;
            }
        };

        for (int i = 0; i < THREADS; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    internalThreadLocal.get();
                }
            }).start();
        }

        Thread.sleep(2000);
    }

    @Test
    public void testSetAndGet() {
        final Integer testVal = 10;
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        internalThreadLocal.set(testVal);
        Assert.assertTrue("set is not equals get", Objects.equals(testVal, internalThreadLocal.get()));
    }

    @Test
    public void testMultiThreadSetAndGet() throws InterruptedException {
        final Integer testVal1 = 10;
        final Integer testVal2 = 20;
        final InternalThreadLocal<Integer> internalThreadLocal = new InternalThreadLocal<Integer>();
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        new Thread(new Runnable() {
            @Override
            public void run() {

                internalThreadLocal.set(testVal1);
                Assert.assertTrue("set is not equals get", Objects.equals(testVal1, internalThreadLocal.get()));
                countDownLatch.countDown();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                internalThreadLocal.set(testVal2);
                Assert.assertTrue("set is not equals get", Objects.equals(testVal2, internalThreadLocal.get()));
                countDownLatch.countDown();
            }
        }).start();
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
            caches1[i] = new ThreadLocal<>();
        }
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                    caches1[i].set("float.lu");
                }
                long start = System.nanoTime();
                for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                    for (int j = 0; j < 1000000; j++) {
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
            caches[i] = new InternalThreadLocal<>();
        }
        Thread t = new InternalThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                    caches[i].set("float.lu");
                }
                long start = System.nanoTime();
                for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                    for (int j = 0; j < 1000000; j++) {
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