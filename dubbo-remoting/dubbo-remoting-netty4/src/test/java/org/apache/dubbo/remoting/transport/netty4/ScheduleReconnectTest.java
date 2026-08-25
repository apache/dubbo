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
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.net.ConnectException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.DefaultEventLoop;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_DEFAULT;
import static org.apache.dubbo.remoting.Constants.LEAST_RECONNECT_DURATION_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the blocking reconnect attempt is delegated to the client executor instead of running on the shared
 * framework connectivity scheduler. See https://github.com/apache/dubbo/issues/13853
 */
class ScheduleReconnectTest {

    // Static because performConnect is invoked from the super constructor before instance fields exist.
    private static final AtomicBoolean FIRST_CONNECT = new AtomicBoolean(true);

    private static final CountDownLatch CONNECT_ATTEMPT = new CountDownLatch(1);

    private static final AtomicReference<String> CONNECT_THREAD = new AtomicReference<>();

    private static final io.netty.channel.Channel MOCK_CHANNEL = mock(io.netty.channel.Channel.class);

    private static final DefaultEventLoop MOCK_EVENT_LOOP = new DefaultEventLoop();

    static {
        when(MOCK_CHANNEL.eventLoop()).thenReturn(MOCK_EVENT_LOOP);
    }

    private FrameworkModel frameworkModel;

    @BeforeEach
    void setUp() {
        FIRST_CONNECT.set(true);
        frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ApplicationConfig applicationConfig = new ApplicationConfig("reconnect-app");
        applicationConfig.setExecutorManagementMode(EXECUTOR_MANAGEMENT_MODE_DEFAULT);
        ConfigManager configManager = new ConfigManager(applicationModel);
        configManager.setApplication(applicationConfig);
        configManager.getApplication();
        applicationModel.setConfigManager(configManager);
    }

    @AfterEach
    void tearDown() {
        frameworkModel.destroy();
    }

    @Test
    void testReconnectRunsOnClientExecutor() throws Exception {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("empty://127.0.0.1:" + port + "/client.schedule.reconnect.test?check=false&"
                + Constants.CONNECT_TIMEOUT_KEY + "=1000&" + LEAST_RECONNECT_DURATION_KEY + "=60000");
        ApplicationModel applicationModel =
                frameworkModel.getApplicationModels().get(0);
        url = url.putAttribute(CommonConstants.SCOPE_MODEL, applicationModel);

        BlockingConnectClient client = new BlockingConnectClient(url, new HandlerAdapter());

        // Trigger a reconnect manually. The scheduled task must hand the blocking connect off to the client
        // executor instead of running it on the shared connectivity scheduler thread.
        client.scheduleReconnect(0, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(CONNECT_ATTEMPT.await(5, TimeUnit.SECONDS), "reconnect was not attempted in time");
        Assertions.assertTrue(
                CONNECT_THREAD.get().startsWith("DubboClientHandler"),
                "reconnect should run on the client executor, but ran on " + CONNECT_THREAD.get());

        client.close(2000);
    }

    private static class HandlerAdapter extends ChannelHandlerAdapter {
        @Override
        public void connected(Channel channel) {}

        @Override
        public void disconnected(Channel channel) {}

        @Override
        public void sent(Channel channel, Object message) {}

        @Override
        public void received(Channel channel, Object message) {}

        @Override
        public void caught(Channel channel, Throwable exception) {}
    }

    private static class BlockingConnectClient extends AbstractNettyConnectionClient {

        BlockingConnectClient(URL url, ChannelHandler handler) throws RemotingException {
            super(url, handler);
        }

        @Override
        protected void initBootstrap() {
            // No real bootstrap is needed because performConnect is stubbed.
        }

        @Override
        protected ChannelFuture performConnect() {
            if (FIRST_CONNECT.compareAndSet(true, false)) {
                // Fail the initial connect performed by the constructor so the client can be
                // created against an unreachable address.
                return new DefaultChannelPromise(MOCK_CHANNEL, MOCK_EVENT_LOOP)
                        .setFailure(new ConnectException("initial connect fails"));
            }
            CONNECT_THREAD.set(Thread.currentThread().getName());
            CONNECT_ATTEMPT.countDown();
            // Never completes: doConnect() blocks on this promise until the connect timeout,
            // exercising the blocking path on the executing thread.
            return new DefaultChannelPromise(MOCK_CHANNEL, MOCK_EVENT_LOOP);
        }
    }
}
