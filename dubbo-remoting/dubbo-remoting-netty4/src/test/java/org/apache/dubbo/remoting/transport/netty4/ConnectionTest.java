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
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.connection.ConnectionManager;
import org.apache.dubbo.remoting.api.connection.MultiplexProtocolConnectionManager;
import org.apache.dubbo.remoting.api.pu.DefaultPuHandler;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_DEFAULT;


public class ConnectionTest {

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
        connectionManager = url.getOrDefaultFrameworkModel().getExtensionLoader(ConnectionManager.class).getExtension(MultiplexProtocolConnectionManager.NAME);
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
    void testGetChannel() {
        final AbstractConnectionClient connectionClient = connectionManager.connect(url, new DefaultPuHandler());
        Assertions.assertNotNull(connectionClient);
        connectionClient.close();
    }

    @Test
    void testRefCnt0() throws InterruptedException {
        final AbstractConnectionClient connectionClient = connectionManager.connect(url, new DefaultPuHandler());
        CountDownLatch latch = new CountDownLatch(1);
        Assertions.assertNotNull(connectionClient);
        connectionClient.addCloseListener(latch::countDown);
        connectionClient.release();
        latch.await();
        Assertions.assertEquals(0, latch.getCount());
    }

    @Test
    void testRefCnt1() {
        DefaultPuHandler handler = new DefaultPuHandler();
        final AbstractConnectionClient connectionClient = connectionManager.connect(url, handler);
        CountDownLatch latch = new CountDownLatch(1);
        Assertions.assertNotNull(connectionClient);

        connectionManager.connect(url, handler);
        connectionClient.addCloseListener(latch::countDown);
        connectionClient.release();
        Assertions.assertEquals(1, latch.getCount());
        connectionClient.close();
    }

    @Test
    void testRefCnt2() throws InterruptedException {
        final AbstractConnectionClient connectionClient = connectionManager.connect(url, new DefaultPuHandler());
        CountDownLatch latch = new CountDownLatch(1);
        connectionClient.retain();
        connectionClient.addCloseListener(latch::countDown);
        connectionClient.release();
        connectionClient.release();
        latch.await();
        Assertions.assertEquals(0, latch.getCount());
    }

    @Test
    void connectSyncTest() throws RemotingException {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("empty://127.0.0.1:" + port + "?foo=bar");
        NettyPortUnificationServer nettyPortUnificationServer = new NettyPortUnificationServer(url, new DefaultPuHandler());
        nettyPortUnificationServer.bind();
        final AbstractConnectionClient connectionClient = connectionManager.connect(url, new DefaultPuHandler());
        Assertions.assertTrue(connectionClient.isAvailable());

        nettyPortUnificationServer.close();
        Assertions.assertFalse(connectionClient.isAvailable());

        nettyPortUnificationServer.bind();
        // auto reconnect
        Assertions.assertTrue(connectionClient.isAvailable());

        connectionClient.close();
        Assertions.assertFalse(connectionClient.isAvailable());
        nettyPortUnificationServer.close();

    }

    @Test
    void testMultiConnect() throws Throwable {
        ExecutorService service = Executors.newFixedThreadPool(10);
        final CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger failedCount = new AtomicInteger(0);
        final AbstractConnectionClient connectionClient = connectionManager.connect(url, new DefaultPuHandler());
        Runnable runnable = () -> {
            try {
                Assertions.assertTrue(connectionClient.isAvailable());
            } catch (Exception e) {
                // ignore
                e.printStackTrace();
                failedCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        };
        for (int i = 0; i < 10; i++) {
            service.execute(runnable);
        }
        latch.await();
        Assertions.assertEquals(0, failedCount.get());
        service.shutdown();
        connectionClient.destroy();
    }
}
