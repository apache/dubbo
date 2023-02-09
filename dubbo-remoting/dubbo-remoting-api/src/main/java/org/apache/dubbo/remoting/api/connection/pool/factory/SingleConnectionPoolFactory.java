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
package org.apache.dubbo.remoting.api.connection.pool.factory;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.remoting.api.connection.ConnectionProvider;
import org.apache.dubbo.remoting.api.connection.pool.AbstractConnectionPool;
import org.apache.dubbo.remoting.api.connection.pool.ConnectionPool;
import org.apache.dubbo.remoting.api.connection.pool.SingleConnectionPool;

import java.util.concurrent.ConcurrentHashMap;

public class SingleConnectionPoolFactory implements ConnectionPoolFactory {

    public static final String NAME = "single";

    private final ConcurrentHashMap<String, ConnectionPool> connectionPoolMap = new ConcurrentHashMap<>();

    @Override
    public ConnectionPool getConnectionPool(URL url, ConnectionProvider connectionProvider) {
        String address = url.getAddress();
        ConcurrentHashMapUtils.computeIfAbsent(connectionPoolMap, address, s -> new SingleConnectionPool(url, connectionProvider));
        ConnectionPool connectionPool = connectionPoolMap.get(address);

        ((AbstractConnectionPool)connectionPool).reference();

        return connectionPool;
    }
}
