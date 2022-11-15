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

package org.apache.dubbo.remoting.transport.netty4.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.connection.ConnectionManager;
import org.apache.dubbo.remoting.api.connection.MultiplexProtocolConnectionManager;
import org.apache.dubbo.remoting.api.pu.DefaultPuHandler;
import org.apache.dubbo.remoting.transport.netty4.NettyPortUnificationServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;

public class MultiplexProtocolConnectionManagerTest {
    private static URL url1;

    private static URL url2;

    private static NettyPortUnificationServer server;


    private static ConnectionManager connectionManager;

    @BeforeAll
    public static void init() throws RemotingException {
        url1 = URL.valueOf("empty://127.0.0.1:8080?foo=bar");
        url2 = URL.valueOf("tri://127.0.0.1:8081?foo=bar");
        server = new NettyPortUnificationServer(url1, new DefaultPuHandler());
        server.bind();
        connectionManager = url1.getOrDefaultFrameworkModel()
                .getExtensionLoader(ConnectionManager.class).getExtension(MultiplexProtocolConnectionManager.NAME);
    }

    @AfterAll
    public static void close() {
        try {
            server.close();
        } catch (Throwable e) {
            // ignored
        }
    }

    @Test
    public void testConnect() throws Exception {
        final AbstractConnectionClient connectionClient = connectionManager.connect(url1, new DefaultPuHandler());
        Assertions.assertNotNull(connectionClient);
        Field protocolsField = connectionManager.getClass().getDeclaredField("protocols");
        protocolsField.setAccessible(true);
        Map protocolMap = (Map) protocolsField.get(connectionManager);
        Assertions.assertNotNull(protocolMap.get(url1.getProtocol()));
        connectionClient.close();
    }

    @Test
    public void testForEachConnection() throws RemotingException {
        DefaultPuHandler handler = new DefaultPuHandler();

        NettyPortUnificationServer server2 = new NettyPortUnificationServer(url2, handler);
        server2.bind();

        final AbstractConnectionClient connect1 = connectionManager.connect(url1, handler);
        final AbstractConnectionClient connect2 = connectionManager.connect(url2, handler);

        Consumer<AbstractConnectionClient> consumer = connection -> {
            try {
                Assertions.assertEquals("empty", connection.getUrl().getProtocol());
            } catch (AssertionFailedError e) {
                // AssertionFailedError is an Error that could not be catched as Exception.
                Assertions.assertEquals("tri", connection.getUrl().getProtocol());
            }

        };

        connectionManager.forEachConnection(consumer);

        // close connections to avoid impacts on other test cases.
        connect1.close();
        connect2.close();

        try {
            server2.close();
        } catch (Throwable e) {
            // ignored
        }
    }

}

