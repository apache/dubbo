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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.remoting.RemotingException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;

public class SingleProtocolConnectionManagerTest {

    private ConnectionManager connectionManager = ExtensionLoader.getExtensionLoader(ConnectionManager.class).getExtension(SingleProtocolConnectionManager.NAME);

    @Test
    public void testConnect() throws Exception {
        URL url = URL.valueOf("empty://127.0.0.1:8080?foo=bar");
        Connection connection = connectionManager.connect(url);
        Assertions.assertNotNull(connection);
        Field protocolsField = connectionManager.getClass().getDeclaredField("connections");
        protocolsField.setAccessible(true);
        Map protocolMap = (Map) protocolsField.get(connectionManager);
        Assertions.assertNotNull(protocolMap.get(url.getAddress()));
        connection.close();

        // Test whether closePromise's listener removes entry
        connection.getClosePromise().await();
        while (protocolMap.containsKey(url.getAddress())) {
        }
        Assertions.assertNull(protocolMap.get(url.getAddress()));
    }

    @Test
    public void testForEachConnection() throws RemotingException {
        URL url = URL.valueOf("empty://127.0.0.1:8080?foo=bar");
        Connection connection = connectionManager.connect(url);

        {
            Consumer<Connection> consumer1 = new Consumer<Connection>() {
                @Override
                public void accept(Connection connection) {
                    Assertions.assertEquals("empty", connection.getUrl().getProtocol());
                }
            };

            connectionManager.forEachConnection(consumer1);
        }

        {
            Consumer<Connection> consumer2 = new Consumer<Connection>() {
                @Override
                public void accept(Connection connection) {
                    Assertions.assertNotEquals("not-empty", connection.getUrl().getProtocol());
                }
            };

            connectionManager.forEachConnection(consumer2);
        }

    }
}
