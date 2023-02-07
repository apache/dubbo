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
package org.apache.dubbo.remoting.api.connection.pool;

import org.apache.dubbo.common.URL;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class SingleConnectionPool<Client> extends AbstractConnectionPool<Client> {

    public static final String NAME = "single";

    private Client client;

    private final Lock lock = new ReentrantLock();

    public SingleConnectionPool(URL url) {
        super(url);
    }

    @Override
    public Client getClient(URL url) {
        if (client != null) {
            return client;
        }
        try {
            lock.lock();
            if (client != null) {
                return client;
            }
            client = initConnection(url);
        } finally {
            lock.unlock();
        }
        return client;
    }

    @Override
    public boolean isAvailable() {
        if (client == null) {
            return false;
        }
        return isConnectionAvailable(client);
    }

    @Override
    public void close() {
        if (client != null) {
            closeConnection(client);
        }
    }

    @Override
    public void close(int seconds) {
        if (client != null) {
            closeConnection(client, seconds);
        }
    }
}
