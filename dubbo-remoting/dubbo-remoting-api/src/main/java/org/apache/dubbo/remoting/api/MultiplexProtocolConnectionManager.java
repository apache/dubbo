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
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class MultiplexProtocolConnectionManager implements ConnectionManager {
    public static final String NAME = "multiple";

    private final ConcurrentMap<String, ConnectionManager> protocols = new ConcurrentHashMap<>();

    private FrameworkModel frameworkModel;

    public MultiplexProtocolConnectionManager(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public Connection connect(URL url) throws RemotingException {
        final ConnectionManager manager = protocols.computeIfAbsent(url.getProtocol(), this::createSingleProtocolConnectionManager);
        return manager.connect(url);
    }

    @Override
    public void forEachConnection(Consumer<Connection> connectionConsumer) {
        protocols.values().forEach(p -> p.forEachConnection(connectionConsumer));
    }

    private ConnectionManager createSingleProtocolConnectionManager(String protocol) {
        return frameworkModel.getExtensionLoader(ConnectionManager.class).getExtension(SingleProtocolConnectionManager.NAME);
    }
}
