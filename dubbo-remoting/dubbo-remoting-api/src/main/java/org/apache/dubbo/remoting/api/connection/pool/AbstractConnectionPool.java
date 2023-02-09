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

public abstract class AbstractConnectionPool implements ConnectionPool {

    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final URL url;

    private final ConnectionProvider connectionProvider;

    public AbstractConnectionPool(URL url,ConnectionProvider connectionProvider) {
        this.url = url;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Client getClient() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public void close(int seconds) {

    }

    protected URL getUrl() {
        return this.url;
    }

    protected ConnectionProvider getConnectionProvider() {
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
        client.close(timeout);
    }

}
