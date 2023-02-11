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
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.api.connection.ConnectionProvider;

import static org.apache.dubbo.common.constants.CommonConstants.LAZY_CONNECT_KEY;

public class SingleConnectionPool<C extends Client> extends AbstractConnectionPool<C> {

    private volatile C client;

    private final Object lock = new Object();

    public SingleConnectionPool(URL url, ConnectionProvider<C> connectionProvider) {
        super(url, connectionProvider);
        if (!url.getParameter(LAZY_CONNECT_KEY, false)) {
            initClientIfAbsent();
        }
    }

    private void initClientIfAbsent() {
        if (client != null) {
            return;
        }
        synchronized (lock) {
            client = getConnectionProvider().initConnection(getUrl());
        }
    }

    @Override
    public C doGetClient() {
        initClientIfAbsent();

        return client;
    }

    @Override
    public boolean doIsAvailable() {
        initClientIfAbsent();

        return isConnectionAvailable(client);
    }

    @Override
    public void doClose() {
        if (client != null) {
            closeConnection(client);
        }
    }

    @Override
    public void doClose(int timeout) {
        if (client != null) {
            closeConnection(client, timeout);
        }
    }

}
