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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class SingleProtocolConnectionManager implements ConnectionManager {
    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(SingleProtocolConnectionManager.class);

    public static final String NAME = "single";

    private final ConcurrentMap<String, AbstractConnectionClient> connections = new ConcurrentHashMap<>(16);

    private FrameworkModel frameworkModel;

    public SingleProtocolConnectionManager(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public AbstractConnectionClient connect(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        return connections.compute(url.getAddress(), (address, conn) -> {
            String transport = url.getParameter(Constants.TRANSPORTER_KEY, "netty4");
            if (conn == null) {
                return createAbstractConnectionClient(url, handler, address, transport);
            } else {
                boolean shouldReuse = conn.retain();
                if (!shouldReuse) {
                    logger.info("Trying to create a new connection for {}.", address);
                    return createAbstractConnectionClient(url, handler, address, transport);
                }
                return conn;
            }
        });
    }

    private AbstractConnectionClient createAbstractConnectionClient(
            URL url, ChannelHandler handler, String address, String transport) {
        ConnectionManager manager =
                frameworkModel.getExtensionLoader(ConnectionManager.class).getExtension(transport);
        final AbstractConnectionClient connectionClient = manager.connect(url, handler);
        connectionClient.addCloseListener(() -> {
            logger.info(
                    "Remove closed connection (with reference count==0) for address {}, a new one will be created for upcoming RPC requests routing to this address.",
                    address);
            connections.remove(address, connectionClient);
        });
        return connectionClient;
    }

    @Override
    public void forEachConnection(Consumer<AbstractConnectionClient> connectionConsumer) {
        connections.values().forEach(connectionConsumer);
    }
}
