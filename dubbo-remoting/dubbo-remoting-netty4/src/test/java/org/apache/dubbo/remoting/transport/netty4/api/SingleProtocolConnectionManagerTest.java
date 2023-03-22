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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.connection.ConnectionManager;
import org.apache.dubbo.remoting.api.connection.SingleProtocolConnectionManager;
import org.apache.dubbo.remoting.api.pu.DefaultPuHandler;
import org.apache.dubbo.remoting.transport.netty4.NettyConnectionClient;
import org.apache.dubbo.remoting.transport.netty4.NettyPortUnificationServer;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_DEFAULT;

public class SingleProtocolConnectionManagerTest {

    private static URL url;

    private static NettyPortUnificationServer server;

    private static ConnectionManager connectionManager;

    @BeforeAll
    public static void init() throws RemotingException {
        int port = NetUtils.getAvailablePort();
        url = URL.valueOf("empty://127.0.0.1:" + port + "?foo=bar");
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig = new ApplicationConfig("provider-app");
        applicationConfig.setExecutorManagementMode(EXECUTOR_MANAGEMENT_MODE_DEFAULT);
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        ConfigManager configManager = new ConfigManager(applicationModel);
        configManager.setApplication(applicationConfig);
        configManager.getApplication();
        applicationModel.setConfigManager(configManager);
        url = url.setScopeModel(applicationModel);
        ModuleModel moduleModel = applicationModel.getDefaultModule();
        url = url.putAttribute(CommonConstants.SCOPE_MODEL, moduleModel);
        server = new NettyPortUnificationServer(url, new DefaultPuHandler());
        server.bind();
        connectionManager = url.getOrDefaultFrameworkModel()
                .getExtensionLoader(ConnectionManager.class).getExtension(SingleProtocolConnectionManager.NAME);
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
        final NettyConnectionClient connectionClient = (NettyConnectionClient) connectionManager.connect(url, new DefaultPuHandler());
        Assertions.assertNotNull(connectionClient);
        Field protocolsField = connectionManager.getClass().getDeclaredField("connections");
        protocolsField.setAccessible(true);
        Map protocolMap = (Map) protocolsField.get(connectionManager);
        Assertions.assertNotNull(protocolMap.get(url.getAddress()));
        connectionClient.close();

        // Test whether closePromise's listener removes entry
        connectionClient.getClosePromise().await();
        while (protocolMap.containsKey(url.getAddress())) {
        }
        Assertions.assertNull(protocolMap.get(url.getAddress()));
    }

    @Test
    public void testForEachConnection() throws RemotingException {
        AbstractConnectionClient connectionClient = connectionManager.connect(url, new DefaultPuHandler());

        {
            Consumer<AbstractConnectionClient> consumer1 = connection -> Assertions.assertEquals("empty", connection.getUrl().getProtocol());

            connectionManager.forEachConnection(consumer1);
        }

        {
            Consumer<AbstractConnectionClient> consumer2 = connection -> Assertions.assertNotEquals("not-empty", connection.getUrl().getProtocol());

            connectionManager.forEachConnection(consumer2);
        }
        connectionClient.close();

    }
}
