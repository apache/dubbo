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
package org.apache.dubbo.cache.support.expiring;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for expiration racing with a replacement of the same key.
 *
 * @date 2026-11-01
 */
class ExpiringMapTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExpirationKeepsConcurrentReplacement(boolean background) throws Exception {
        assertReplacementPreserved(background, "old", "new");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExpirationKeepsReplacementWithSameValue(boolean background) throws Exception {
        assertReplacementPreserved(background, "same", "same");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExpirationDoesNotComparePayloads(boolean background) throws Exception {
        assertReplacementPreserved(background, new UncomparableValue(), new UncomparableValue());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExpirationRemovesUnchangedEntry(boolean background) throws Exception {
        ExpiringMap<String, String> map = new ExpiringMap<>(3600);
        map.put("key", "old");
        expireEntry(map, "key");
        if (background) {
            processExpires(map);
        } else {
            assertNull(map.get("key"));
        }
        assertFalse(map.containsKey("key"));
    }

    private static <V> void assertReplacementPreserved(boolean background, V oldValue, V replacement) throws Exception {
        BlockingKey key = new BlockingKey(background ? 1 : 2);
        ExpiringMap<BlockingKey, V> map = new ExpiringMap<>(3600);
        map.put(key, oldValue);
        expireEntry(map, key);
        FutureTask<Void> expiration = new FutureTask<>(() -> {
            if (background) {
                processExpires(map);
            } else {
                assertNull(map.get(key));
            }
            return null;
        });
        Thread worker = new Thread(expiration);
        key.expirationThread = worker;
        worker.start();
        try {
            assertTrue(key.removing.await(10, TimeUnit.SECONDS), "expiration did not reach removal");
            map.put(key, replacement);
            key.replaced.countDown();
            expiration.get(10, TimeUnit.SECONDS);
            assertSame(replacement, map.get(key));
        } finally {
            key.replaced.countDown();
            worker.interrupt();
            worker.join(10000);
        }
    }

    private static void expireEntry(ExpiringMap<?, ?> map, Object key) throws Exception {
        Field delegate = ExpiringMap.class.getDeclaredField("delegateMap");
        delegate.setAccessible(true);
        Object entry = ((Map<?, ?>) delegate.get(map)).get(key);
        Field lastAccessTime = entry.getClass().getDeclaredField("lastAccessTime");
        lastAccessTime.setAccessible(true);
        ((AtomicLong) lastAccessTime.get(entry)).set(0);
    }

    private static void processExpires(ExpiringMap<?, ?> map) throws Exception {
        Object thread = map.getExpireThread();
        Method process = thread.getClass().getDeclaredMethod("processExpires");
        process.setAccessible(true);
        process.invoke(thread);
    }

    private static final class BlockingKey {
        private final int removalHashCall;
        private final CountDownLatch removing = new CountDownLatch(1);
        private final CountDownLatch replaced = new CountDownLatch(1);
        private Thread expirationThread;
        private int hashCalls;

        private BlockingKey(int removalHashCall) {
            this.removalHashCall = removalHashCall;
        }

        @Override
        public int hashCode() {
            if (Thread.currentThread() == expirationThread && ++hashCalls == removalHashCall) {
                removing.countDown();
                try {
                    assertTrue(replaced.await(10, TimeUnit.SECONDS), "replacement did not complete");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            return 7;
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }
    }

    private static final class UncomparableValue {
        @Override
        public boolean equals(Object other) {
            throw new AssertionError("expiration must compare entries by identity, not payload equality");
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
