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
package org.apache.dubbo.rpc.cluster.directory;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.SingleRouterChain;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class AbstractDirectoryConcurrencyTest {

    private TestDirectory directory;
    private URL url;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        url = URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService");
        directory = new TestDirectory(url);
        executor = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void tearDown() {
        if (directory != null) {
            directory.destroy();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void testMultipleReadLocks() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicBoolean failed = new AtomicBoolean(false);

        // Setup the directory with a slow list implementation to simulate work holding the read lock
        directory.setListAction(() -> {
            try {
                // Wait for the latch to ensure all threads are in doList
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    directory.list(mock(Invocation.class));
                } catch (Exception e) {
                    e.printStackTrace();
                    failed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Give threads time to start and acquire read lock
        Thread.sleep(100);
        // Release the latch, letting them proceed
        latch.countDown();

        Assertions.assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All list calls should complete");
        Assertions.assertFalse(failed.get(), "No exceptions should occur during concurrent reads");
    }

    @Test
    void testWriteBlocksRead() throws InterruptedException {
        CountDownLatch writeLockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch releaseWriteLockLatch = new CountDownLatch(1);
        AtomicReference<Boolean> readBlocked = new AtomicReference<>(false);

        // Thread to hold write lock
        executor.submit(() -> {
            directory.simulateWriteLock(writeLockAcquiredLatch, releaseWriteLockLatch);
        });

        // Wait for write lock to be acquired
        Assertions.assertTrue(writeLockAcquiredLatch.await(5, TimeUnit.SECONDS));

        // Try to read in another thread
        Future<?> readFuture = executor.submit(() -> {
            long start = System.currentTimeMillis();
            directory.list(mock(Invocation.class));
            long duration = System.currentTimeMillis() - start;
            // If duration is > 100ms, we assume it was blocked
            readBlocked.set(duration >= 100);
        });

        // Sleep to ensure read thread tries to acquire lock and blocks
        Thread.sleep(200);

        // Release write lock
        releaseWriteLockLatch.countDown();

        try {
            readFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assertions.fail("Read execution failed");
        }

        Assertions.assertTrue(readBlocked.get(), "Read operation should be blocked by write lock");
    }

    @Test
    void testConcurrentReadAndWrite() throws InterruptedException {
        int readThreads = 10;
        int writeThreads = 2;
        int iterations = 100;
        CountDownLatch doneLatch = new CountDownLatch(readThreads + writeThreads);
        AtomicBoolean failed = new AtomicBoolean(false);

        directory.setListAction(() -> {
            // Simulate some work
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        });

        // Start read threads
        for (int i = 0; i < readThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        directory.list(mock(Invocation.class));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    failed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start write threads
        for (int i = 0; i < writeThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        // Use setInvokers to trigger write lock
                        directory.setInvokers(new BitList<>(Collections.emptyList()));
                        Thread.sleep(2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    failed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        Assertions.assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All operations should complete");
        Assertions.assertFalse(failed.get(), "No exceptions should occur during concurrent read/write");
    }

    // Helper class to expose protected methods and hook into list()
    static class TestDirectory extends AbstractDirectory<Object> {
        private Runnable listAction = () -> {};

        public TestDirectory(URL url) {
            super(url);
            // Initialize with empty router chain to avoid NPE
            setRouterChain(RouterChain.buildChain(Object.class, url));
        }

        public void setListAction(Runnable listAction) {
            this.listAction = listAction;
        }

        @Override
        public Class<Object> getInterface() {
            return Object.class;
        }

        @Override
        public List<Invoker<Object>> getAllInvokers() {
            return Collections.emptyList();
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        protected List<Invoker<Object>> doList(
                SingleRouterChain<Object> singleRouterChain, BitList<Invoker<Object>> invokers, Invocation invocation)
                throws RpcException {
            listAction.run();
            return Collections.emptyList();
        }

        // Helper to simulate holding write lock
        public void simulateWriteLock(CountDownLatch acquired, CountDownLatch release) {
            // We use refreshInvoker to acquire write lock, but we need to inject our blocking logic
            // Since we can't easily inject into refreshInvoker without complex mocking,
            // we'll use a trick: override setInvokers logic? No, setInvokers uses lock internally.
            // But we can use the fact that addRouters/etc might not use the same lock? No.
            // We can't access the lock directly.
            // However, we can use 'addInvalidateInvoker' or similar if we can hook into it.

            // Actually, we can use a method that holds the lock and calls something we can override?
            // AbstractDirectory doesn't call many overridable methods inside the lock.
            // refreshInvoker calls refreshInvokerInternal (private).

            // Wait, we can use reflection to get the lock and lock it manually for this test helper.
            try {
                java.lang.reflect.Field lockField = AbstractDirectory.class.getDeclaredField("invokerRefreshLock");
                lockField.setAccessible(true);
                java.util.concurrent.locks.ReadWriteLock lock =
                        (java.util.concurrent.locks.ReadWriteLock) lockField.get(this);

                lock.writeLock().lock();
                try {
                    acquired.countDown();
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.writeLock().unlock();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Expose setInvokers for test
        @Override
        public void setInvokers(BitList<Invoker<Object>> invokers) {
            super.setInvokers(invokers);
        }
    }
}
