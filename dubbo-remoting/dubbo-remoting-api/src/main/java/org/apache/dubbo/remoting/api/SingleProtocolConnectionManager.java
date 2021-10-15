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
import org.apache.dubbo.remoting.RemotingException;

import io.netty.util.internal.PlatformDependent;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class SingleProtocolConnectionManager implements ConnectionManager {
    public static final String NAME = "single";

    private final ConcurrentMap<String, Connection> connections = PlatformDependent.newConcurrentHashMap();

    @Override
    public Connection connect(URL url) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        return connections.compute(url.getAddress(), (address, conn) -> {
            if (conn == null) {
                final Connection created = new Connection(url);
                created.getClosePromise().addListener(future -> connections.remove(address, created));
                return created;
            } else {
                conn.retain();
                return conn;
            }
        });
    }

    @Override
    public void forEachConnection(Consumer<Connection> connectionConsumer) {
        connections.values().forEach(connectionConsumer);
    }
}
