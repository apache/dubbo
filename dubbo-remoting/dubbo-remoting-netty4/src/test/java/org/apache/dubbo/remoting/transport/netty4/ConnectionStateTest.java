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
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider.State;
import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.AttributeKey;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConnectionStateTest {
    @Test
    void asyncInitialConnectReturnsAndCloseReleasesAnUnfinishedAttemptWithoutWaitingForTimeout() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        Channel channel = channel();
        CountDownLatch started = new CountDownLatch(1);
        DefaultChannelPromise pending = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        try {
            URL url = url(framework)
                    .addParameter("connect.timeout", 6000)
                    .putAttribute(ConnectionStateProvider.ASYNC_CONNECT_ATTRIBUTE, true)
                    .putAttribute("test.pending-connect", pending)
                    .putAttribute("test.connect-started", started);
            long start = System.nanoTime();
            TestClient client = new TestClient(url);
            try {
                assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1000);
                assertTrue(started.await(1, TimeUnit.SECONDS));
                assertEquals(State.CONNECTING, client.getConnectivityState());
                start = System.nanoTime();
                client.close();
                assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 500);
                assertEquals(State.SHUTDOWN, client.getConnectivityState());
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
                while (!pending.isCancelled() && System.nanoTime() < deadline) Thread.sleep(5);
                assertTrue(pending.isCancelled());
                assertFalse(channel.isActive());
                client.onConnected(channel());
                assertEquals(State.SHUTDOWN, client.getConnectivityState());
                assertNull(client.getChannel(true));
            } finally {
                client.close();
            }
        } finally {
            pending.cancel(false);
            framework.destroy();
        }
    }

    @Test
    void repeatedSubscriptionsUnsubscribeIndependentlyAndStaleLossCannotClearAReplacement() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        try {
            TestClient client = new TestClient(url(framework));
            try {
                assertEquals(State.READY, client.getConnectivityState());
                List<State> events = new ArrayList<>();
                Consumer<State> listener = events::add;
                AutoCloseable first = client.addConnectivityListener(listener);
                AutoCloseable second = client.addConnectivityListener(listener);
                assertEquals(Arrays.asList(State.READY, State.READY), events);
                first.close();
                first.close();
                Channel old = client.getChannel(true);
                client.onDisconnected(old);
                assertEquals(State.TRANSIENT_FAILURE, client.getConnectivityState());
                client.reconnect();
                Channel replacement = client.getChannel(true);
                assertNotSame(old, replacement);
                assertEquals(
                        Arrays.asList(State.READY, State.READY, State.TRANSIENT_FAILURE, State.CONNECTING, State.READY),
                        events);
                client.onDisconnected(old);
                client.onGoaway(old);
                assertSame(replacement, client.getChannel(true));
                assertEquals(State.READY, client.getConnectivityState());
                client.close();
                assertEquals(State.SHUTDOWN, client.getConnectivityState());
                assertEquals(State.SHUTDOWN, events.get(events.size() - 1));
                second.close();
                List<State> closed = new ArrayList<>();
                client.addConnectivityListener(closed::add).close();
                client.onConnected(channel());
                assertEquals(Arrays.asList(State.SHUTDOWN), closed);
            } finally {
                client.close();
            }
        } finally {
            framework.destroy();
        }
    }

    @Test
    void throwingListenerCannotStopOtherListenersOrConnectionRecovery() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        try {
            TestClient client = new TestClient(url(framework));
            try (AutoCloseable broken = client.addConnectivityListener(state -> {
                throw new IllegalStateException("test callback");
            })) {
                List<State> events = new ArrayList<>();
                try (AutoCloseable healthy = client.addConnectivityListener(events::add)) {
                    client.onDisconnected(client.getChannel(true));
                    client.reconnect();
                    assertEquals(
                            Arrays.asList(State.READY, State.TRANSIENT_FAILURE, State.CONNECTING, State.READY), events);
                }
                client.close();
                assertEquals(
                        Arrays.asList(State.READY, State.TRANSIENT_FAILURE, State.CONNECTING, State.READY), events);
            } finally {
                client.close();
            }
        } finally {
            framework.destroy();
        }
    }

    @Test
    void nativeBackoffUsesBoundedGrowthAndTwentyPercentJitter() {
        assertEquals(800, AbstractNettyConnectionClient.reconnectDelayMillis(0, 0));
        assertEquals(1200, AbstractNettyConnectionClient.reconnectDelayMillis(0, 1));
        assertEquals(1600, AbstractNettyConnectionClient.reconnectDelayMillis(1, 0.5));
        assertEquals(2560, AbstractNettyConnectionClient.reconnectDelayMillis(2, 0.5));
        assertEquals(120000, AbstractNettyConnectionClient.reconnectDelayMillis(Integer.MAX_VALUE, 0.5));
        assertEquals(96000, AbstractNettyConnectionClient.reconnectDelayMillis(Integer.MAX_VALUE, 0));
        assertEquals(144000, AbstractNettyConnectionClient.reconnectDelayMillis(Integer.MAX_VALUE, 1));
    }

    private static URL url(FrameworkModel framework) {
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("connection-state-test"));
        return URL.valueOf("empty://127.0.0.1:17070").setScopeModel(application);
    }

    private static Channel channel() {
        Channel channel = mock(Channel.class);
        DefaultAttributeMap attributes = new DefaultAttributeMap();
        when(channel.attr(any(AttributeKey.class))).thenAnswer(call -> attributes.attr(call.getArgument(0)));
        when(channel.isActive()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.localAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 17071));
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 17070));
        when(channel.close()).thenAnswer(call -> {
            when(channel.isActive()).thenReturn(false);
            return new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE).setSuccess();
        });
        return channel;
    }

    private static final class TestClient extends AbstractNettyConnectionClient {
        TestClient(URL url) throws RemotingException {
            super(url, new ChannelHandlerAdapter());
        }

        @Override
        protected void initBootstrap() {}

        @Override
        protected ChannelFuture performConnect() {
            ChannelFuture pending = (ChannelFuture) getUrl().getAttribute("test.pending-connect");
            if (pending != null) {
                ((CountDownLatch) getUrl().getAttribute("test.connect-started")).countDown();
                return pending;
            }
            Channel channel = channel();
            onConnected(channel);
            return new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE).setSuccess();
        }
    }
}
