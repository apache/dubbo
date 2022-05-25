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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolEntry {

    private final URL url;

    private final AtomicInteger reusedCount = new AtomicInteger();

    private final Object reusedLock = new Object();

    private Connection connection;

    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    public PoolEntry(URL url) {
        this.url = url;
    }

    public Object getReusedLock() {
        return reusedLock;
    }

    public AtomicInteger getReusedCount() {
        return reusedCount;
    }

    public Connection createConnection() {
        connection = new Connection(url);
        connection.getClosePromise()
            .addListener(future -> {
                if (future.isSuccess()) {
                    closeFuture.complete(null);
                } else {
                    closeFuture.completeExceptionally(future.cause());
                }
            });
        return connection;
    }

    public void close() {
        if (connection != null) {
            connection.close();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public CompletableFuture<Void> getCloseFuture() {
        return closeFuture;
    }
}
