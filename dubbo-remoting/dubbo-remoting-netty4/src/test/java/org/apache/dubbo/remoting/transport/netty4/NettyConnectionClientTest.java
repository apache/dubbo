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
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.net.InetSocketAddress;
import java.util.Map;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.proxy.Socks5ProxyHandler;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_DEFAULT;
import static org.apache.dubbo.remoting.transport.netty4.NettyClient.SOCKS_PROXY_HOST;
import static org.apache.dubbo.remoting.transport.netty4.NettyClient.SOCKS_PROXY_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class NettyConnectionClientTest {

    @Test
    void shouldInsertSocks5ProxyHandlerWhenSocksProxyConfigured() throws Exception {
        URL url = createUrl("192.168.1.10", 20880, "proxy.example.com", "2081");
        EmbeddedChannel channel = new EmbeddedChannel();

        NettyClient.configureSocks5Proxy(url, channel.pipeline());

        Socks5ProxyHandler handler = channel.pipeline().get(Socks5ProxyHandler.class);
        assertInstanceOf(Socks5ProxyHandler.class, handler);
        assertEquals("proxy.example.com", ((InetSocketAddress) handler.proxyAddress()).getHostString());
        assertEquals(2081, ((InetSocketAddress) handler.proxyAddress()).getPort());
    }

    @Test
    void shouldNotInsertSocks5ProxyHandlerWhenSocksProxyHostBlank() throws Exception {
        URL url = createUrl("192.168.1.10", 20880, "   ", "2081");
        EmbeddedChannel channel = new EmbeddedChannel();

        NettyClient.configureSocks5Proxy(url, channel.pipeline());

        assertNull(channel.pipeline().get(Socks5ProxyHandler.class));
    }

    @Test
    void shouldNotInsertSocks5ProxyHandlerWhenTargetAddressIsLocal() throws Exception {
        URL url = createUrl("127.0.0.1", 20880, "proxy.example.com", "2081");
        EmbeddedChannel channel = new EmbeddedChannel();

        NettyClient.configureSocks5Proxy(url, channel.pipeline());

        assertNull(channel.pipeline().get(Socks5ProxyHandler.class));
    }

    private URL createUrl(String host, int port, String socksProxyHost, String socksProxyPort) {
        URL url = new ServiceConfigURL("tri", host, port);

        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig = new ApplicationConfig("provider-app");
        applicationConfig.setExecutorManagementMode(EXECUTOR_MANAGEMENT_MODE_DEFAULT);
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        ConfigManager configManager = new ConfigManager(applicationModel);
        configManager.setApplication(applicationConfig);
        configManager.getApplication();
        applicationModel.setConfigManager(configManager);

        Map<String, String> properties =
                applicationModel.modelEnvironment().getSystemConfiguration().getProperties();
        if (socksProxyHost != null) {
            properties.put(SOCKS_PROXY_HOST, socksProxyHost);
        }
        if (socksProxyPort != null) {
            properties.put(SOCKS_PROXY_PORT, socksProxyPort);
        }

        url = url.setScopeModel(applicationModel);
        ModuleModel moduleModel = applicationModel.getDefaultModule();
        return url.putAttribute(CommonConstants.SCOPE_MODEL, moduleModel);
    }
}
