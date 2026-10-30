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
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.remoting.api.pu.DefaultPuHandler;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import io.netty.channel.socket.SocketChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_DEFAULT;
import static org.apache.dubbo.common.constants.CommonConstants.EXT_PROTOCOL;
import static org.awaitility.Awaitility.await;

class PortUnificationServerTest {

    @Test
    void testBind() throws Throwable {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("empty://127.0.0.1:" + port + "?foo=bar&" + EXT_PROTOCOL + "=tri");
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
        // abstract endpoint need to get codec of url(which is in triple package)
        final NettyPortUnificationServer server = new NettyPortUnificationServer(url, new DefaultPuHandler());
        server.bind();
        Assertions.assertTrue(server.isBound());
        Assertions.assertEquals(2, server.getProtocols().size());
        server.close();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {true, false})
    void testKeepAliveOnAcceptedChannels(Boolean keepAlive) throws Throwable {
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("provider-app"));
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setKeepAlive(keepAlive);
            Map<String, String> parameters = new HashMap<>();
            AbstractConfig.appendParameters(parameters, protocolConfig);
            URL url = URL.valueOf("empty://127.0.0.1:0")
                    .addParameters(parameters)
                    .setScopeModel(applicationModel.getDefaultModule());
            NettyPortUnificationServer server = new NettyPortUnificationServer(url, new DefaultPuHandler());
            try {
                server.bind();
                try (Socket client = new Socket()) {
                    client.connect(new InetSocketAddress(
                            "127.0.0.1", server.getLocalAddress().getPort()));
                    await().untilAsserted(() ->
                            Assertions.assertEquals(1, server.getChannels().size()));
                    NettyChannel channel =
                            (NettyChannel) server.getChannels().iterator().next();
                    SocketChannel socketChannel = (SocketChannel) channel.getNioChannel();
                    Assertions.assertEquals(
                            Boolean.TRUE.equals(keepAlive),
                            socketChannel.config().isKeepAlive());
                }
            } finally {
                server.close();
            }
        } finally {
            frameworkModel.destroy();
        }
    }
}
