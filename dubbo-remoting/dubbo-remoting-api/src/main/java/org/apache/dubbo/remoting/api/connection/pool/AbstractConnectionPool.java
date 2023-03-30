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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.api.connection.ConnectionProvider;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractConnectionPool<C extends Client> implements ConnectionPool<C> {

    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final URL url;

    private final ConnectionProvider<C> connectionProvider;

    protected final AtomicInteger referenceCount;

    public AbstractConnectionPool(URL url, ConnectionProvider<C> connectionProvider) {
        this.url = url;
        this.connectionProvider = connectionProvider;
        referenceCount = new AtomicInteger(0);
    }

    @Override
    public C getClient() {
        if (referenceCount.get() <= 0) {
            throw new IllegalStateException("client is already closed!");
        }
        return doGetClient();
    }

    protected abstract C doGetClient();

    @Override
    public boolean isAvailable() {
        if (referenceCount.get() <= 0) {
            return false;
        }
        return doIsAvailable();
    }

    protected abstract boolean doIsAvailable();

    @Override
    public void close() {
        if (referenceCount.decrementAndGet() <= 0) {
            doClose();
        }
    }

    protected abstract void doClose();

    @Override
    public void close(int timeout) {
        if (referenceCount.decrementAndGet() <= 0) {
            if (timeout <= 0) {
                doClose();
            } else {
                doClose(timeout);
            }
        }
    }

    protected abstract void doClose(int timeout);

    public void reference() {
        referenceCount.incrementAndGet();
    }

    protected URL getUrl() {
        return this.url;
    }

    protected ConnectionProvider<C> getConnectionProvider() {
        return this.connectionProvider;
    }

    protected boolean isConnectionAvailable(Client client) {
        return client != null && client.isConnected()
                && !client.hasAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY);
    }

    protected void closeConnection(Client client) {
        client.close();
    }

    protected void closeConnection(Client client, int timeout) {
        if (timeout <= 0) {
            client.close();
        } else {
            client.close(timeout);
        }
    }

}
