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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.connection.ConnectionManager;
import org.apache.dubbo.remoting.api.connection.MultiplexProtocolConnectionManager;
import org.apache.dubbo.remoting.api.pu.DefaultPuHandler;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_DEFAULT;
import static org.awaitility.Awaitility.await;

public class TripleGoAwayTest {

    private static URL url;

    private static NettyPortUnificationServer server;

    private static ConnectionManager connectionManager;

    @BeforeAll
    public static void init() throws Throwable {
        int port = NetUtils.getAvailablePort();
        url = URL.valueOf("tri://127.0.0.1:" + port + "?foo=bar");
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
                .getExtensionLoader(ConnectionManager.class)
                .getExtension(MultiplexProtocolConnectionManager.NAME);
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
    void testGoawayCausesNoChannelFailure() {
        final AbstractConnectionClient connectionClient = connectionManager.connect(url, new DefaultPuHandler());
        Assertions.assertTrue(
                connectionClient.isAvailable(), "Precondition: connection should be available before GOAWAY");
        io.netty.channel.Channel channelBeforeGoaway = connectionClient.getChannel(true);
        NettyConnectionHandler handler = new NettyConnectionHandler((AbstractNettyConnectionClient) connectionClient);
        handler.onGoAway(channelBeforeGoaway);
        io.netty.channel.Channel channelAfterGoaway = connectionClient.getChannel(true);
        Assertions.assertNotNull(
                channelAfterGoaway,
                "Channel should NOT be null immediately after GOAWAY "
                        + "— old channel should stay alive until new one is ready");
        await().atMost(Duration.ofSeconds(10)).until(() -> connectionClient.isAvailable());
        Assertions.assertTrue(
                connectionClient.isAvailable(), "Connection should be available after GOAWAY migration completes");
        connectionClient.close();
    }
}
