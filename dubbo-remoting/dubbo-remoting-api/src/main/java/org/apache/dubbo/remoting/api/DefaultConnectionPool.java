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

package org.apache.dubbo.remoting.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultConnectionPool implements ConnectionPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConnectionPool.class);

    private volatile State state = State.ACTIVE;

    private final int maxTotal = 8;

    private final int maxIdle = 8;

    private final int minIdle = 0;

    private final URL url;


    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    private final Queue<Connection> cache = new ConcurrentLinkedQueue<>();

    private final Queue<Connection> all = new ConcurrentLinkedQueue<>();

    private final AtomicInteger objectCount = new AtomicInteger();

    private final AtomicInteger objectsInCreationCount = new AtomicInteger();

    private final AtomicInteger idleCount = new AtomicInteger();

    public DefaultConnectionPool(URL url) {
        this.url = url;
    }

    @Override
    public Connection acquire() {
        Connection connection = cache.poll();
        if (connection == null) {
            long objects = getObjectCount() + getCreationInProgress();
            if (getActualMaxTotal() > objects) {
                return createConnection();
            }
            throw new RuntimeException("No connection available");
        }
        return connection;
    }

    @Override
    public void release(Connection connection) {
        if (!all.contains(connection)) {
            return;
        }
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

    @Override
    public CompletableFuture<Void> closeAsync() {
        if (!isPoolActive()) {
            return closeFuture;
        }
        state = State.TERMINATING;
        Connection cached;
        while ((cached = cache.poll()) != null) {
            idleCount.decrementAndGet();
            objectCount.decrementAndGet();
            all.remove(cached);
            cached.close();
        }
        state = State.TERMINATED;
        return closeFuture;
    }


    private void return0(Connection connection) {
        int idleCount = this.idleCount.incrementAndGet();
        if (idleCount > getActualMaxIdle()) {
            this.idleCount.decrementAndGet();
            destroy0(connection);
            return;
        }
        cache.add(connection);
    }

    private void destroy0(Connection connection) {
        destroy1(connection);
        connection.close();
    }

    private void destroy1(Connection connection) {
        objectCount.decrementAndGet();
        all.remove(connection);
    }


    private Connection createConnection() {
        try {
            long creations = objectsInCreationCount.incrementAndGet();
            Connection connection = new Connection(url);
            connection.getClosePromise().addListener(future -> {
                destroy1(connection);
            });
            if (isPoolActive()) {
                objectCount.incrementAndGet();
                all.add(connection);
                return connection;
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

    public void createIdle() {
        int potentialIdle = getMinIdle() - getIdle();
        if (potentialIdle <= 0 || !isPoolActive()) {
            return;
        }
        long totalLimit = getAvailableCapacity();
        int toCreate = Math.toIntExact(Math.min(Math.max(0, totalLimit), potentialIdle));
        for (int i = 0; i < toCreate; i++) {
            Connection connection = createConnection();
            if (isPoolActive()) {
                idleCount.decrementAndGet();
                cache.add(connection);
            } else {
                destroy0(connection);
            }
        }
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
}
