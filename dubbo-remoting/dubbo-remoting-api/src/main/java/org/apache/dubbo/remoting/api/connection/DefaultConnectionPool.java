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

package org.apache.dubbo.remoting.api.connection;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.api.ConnectionPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultConnectionPool implements ConnectionPool<DefaultConnectionPoolEntry> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConnectionPool.class);

    private volatile State state = State.ACTIVE;

    private final int maxTotal = 8;

    private final int maxIdle = 8;

    private final int minIdle = 0;

    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    private final Queue<DefaultConnectionPoolEntry> cache = new ConcurrentLinkedQueue<>();

    private final Queue<DefaultConnectionPoolEntry> all = new ConcurrentLinkedQueue<>();

    private final AtomicInteger objectCount = new AtomicInteger();

    private final AtomicInteger objectsInCreationCount = new AtomicInteger();

    private final AtomicInteger idleCount = new AtomicInteger();

    private final URL url;

    public DefaultConnectionPool(URL url) {
        this.url = url;
    }


    @Override
    public ConnectionPoolEntry acquire() {
        DefaultConnectionPoolEntry connection = cache.poll();
        if (connection == null) {
            long objects = getObjectCount() + getCreationInProgress();
            if (getActualMaxTotal() > objects) {
                return createConnection();
            }
            connection = all.poll();
            synchronized (connection.getReusedLock()) {
                connection.getReusedCount().incrementAndGet();
            }
            try {
                return connection;
            } finally {
                all.add(connection);
            }
        }
        idleCount.decrementAndGet();
        return connection;
    }

    @Override
    public void release(DefaultConnectionPoolEntry poolEntry) {
        if (poolEntry.getReusedCount().decrementAndGet() > 0) {
            return;
        }
        synchronized (poolEntry.getReusedLock()) {
            release0(poolEntry);
        }
    }


    private void release0(DefaultConnectionPoolEntry connection) {
        if (idleCount.get() >= getActualMaxIdle()) {
            destroy0(connection);
            return;
        }
        return0(connection);
    }

    @Override
    public CompletableFuture<Void> getCloseFuture() {
        return closeFuture;
    }


    private CompletableFuture<Void> clearAsync() {

        List<CompletableFuture<Void>> futures = new ArrayList<>(all.size());

        DefaultConnectionPoolEntry cached;
        while ((cached = cache.poll()) != null) {
            idleCount.decrementAndGet();
            objectCount.decrementAndGet();
            all.remove(cached);
            futures.add(CompletableFuture.runAsync(cached::close));
        }
        return allOf(futures);
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        if (!isPoolActive()) {
            return closeFuture;
        }
        state = State.TERMINATING;
        CompletableFuture<Void> clear = clearAsync();
        state = State.TERMINATED;

        clear.whenComplete((aVoid, throwable) -> {

            if (throwable != null) {
                closeFuture.completeExceptionally(throwable);
            } else {
                closeFuture.complete(aVoid);
            }
        });
        return closeFuture;
    }


    private void return0(DefaultConnectionPoolEntry connection) {
        int idleCount = this.idleCount.incrementAndGet();
        if (idleCount > getActualMaxIdle()) {
            this.idleCount.decrementAndGet();
            destroy0(connection);
            return;
        }
        cache.add(connection);
    }

    private void destroy0(DefaultConnectionPoolEntry entry) {
        destroy1(entry);
        entry.close();
    }

    private void destroy1(DefaultConnectionPoolEntry connection) {
        objectCount.decrementAndGet();
        all.remove(connection);
    }


    private DefaultConnectionPoolEntry createConnection() {
        try {
            long creations = objectsInCreationCount.incrementAndGet();
            DefaultConnectionPoolEntry connectionPoolEntry = new DefaultConnectionPoolEntry(url);
            connectionPoolEntry.getCloseFuture()
                .whenComplete((aVoid, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("Failed to create connection", throwable);
                    }
                    destroy1(connectionPoolEntry);
                });
            if (isPoolActive()) {
                createConnection0(connectionPoolEntry);
                objectCount.incrementAndGet();
                all.add(connectionPoolEntry);
                return connectionPoolEntry;
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to create connection", t);
            objectCount.decrementAndGet();
            all.remove();
        } finally {
            objectsInCreationCount.decrementAndGet();
        }
        throw new RuntimeException("No connection available");
    }

    private void createConnection0(DefaultConnectionPoolEntry connectionPoolEntry) {
        Connection connection = connectionPoolEntry.createConnection();
    }

    public int getMinIdle() {
        int maxIdleSave = getActualMaxIdle();
        return Math.min(this.minIdle, maxIdleSave);
    }

    private long getAvailableCapacity() {
        return getActualMaxTotal() - (getCreationInProgress() + getObjectCount());
    }


    public int getIdle() {
        return idleCount.get();
    }

    public int getObjectCount() {
        return objectCount.get();
    }

    public int getCreationInProgress() {
        return objectsInCreationCount.get();
    }

    private boolean isPoolActive() {
        return this.state == State.ACTIVE;
    }

    private int getActualMaxTotal() {
        return maxOrActual(maxTotal);
    }

    private int getActualMaxIdle() {
        return maxOrActual(maxIdle);
    }

    private static int maxOrActual(int count) {
        return count > -1 ? count : Integer.MAX_VALUE;
    }


    enum State {
        ACTIVE, TERMINATING, TERMINATED;
    }


    private CompletableFuture<Void> allOf(Collection<? extends CompletionStage<?>> stages) {
        CompletableFuture<?>[] futures = new CompletableFuture[stages.size()];
        int index = 0;
        for (CompletionStage<?> stage : stages) {
            futures[index++] = stage.toCompletableFuture();
        }
        return CompletableFuture.allOf(futures);
    }
}
