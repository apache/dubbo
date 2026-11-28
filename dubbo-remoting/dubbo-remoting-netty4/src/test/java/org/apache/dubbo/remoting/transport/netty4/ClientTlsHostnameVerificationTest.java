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
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.remoting.transport.AbstractClient;
import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslClientTlsHandler;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import javax.net.ssl.SSLException;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies hostname validation through both TCP client TLS pipelines.
 *
 * @date 2026-11-12
 */
class ClientTlsHostnameVerificationTest {

    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void acceptsMatchingSubjectAlternativeName(boolean connectionClient) throws Exception {
        assertHandshake(connectionClient, "server.pem", true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsMismatchedSubjectAlternativeName(boolean connectionClient) throws Exception {
        assertHandshake(connectionClient, "wrong-host.pem", false);
    }

    @Test
    void usesUrlAddressForLegacyConstructor() throws Exception {
        FrameworkModel frameworkModel = new FrameworkModel();
        EmbeddedChannel channel = null;
        try {
            ApplicationModel applicationModel = configureClient(frameworkModel, copyResource("ca.pem"));
            URL url = URL.valueOf("empty://localhost:20880").setScopeModel(applicationModel.getDefaultModule());
            channel = new EmbeddedChannel(new SslClientTlsHandler(url));
            SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
            assertNotNull(sslHandler);
            assertEquals("localhost", sslHandler.engine().getPeerHost());
            assertEquals(20880, sslHandler.engine().getPeerPort());
        } finally {
            try {
                if (channel != null) {
                    channel.finishAndReleaseAll();
                }
            } finally {
                frameworkModel.destroy();
            }
        }
    }

    @Test
    void preservesContextOnlyConstructor() throws Exception {
        SslContext context =
                SslContextBuilder.forClient().sslProvider(SslProvider.JDK).build();
        EmbeddedChannel channel = new EmbeddedChannel(new SslClientTlsHandler(context));
        try {
            SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
            assertNotNull(sslHandler);
            assertNull(sslHandler.engine().getPeerHost());
            assertEquals(-1, sslHandler.engine().getPeerPort());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "orders.shop.svc.cluster.local"})
    void explicitServerNameRetainsPeerPortAndDefaultsToSocketHost(String serverName) throws Exception {
        SslContext context =
                SslContextBuilder.forClient().sslProvider(SslProvider.JDK).build();
        InetSocketAddress peer = InetSocketAddress.createUnresolved("127.0.0.1", 20880);
        EmbeddedChannel channel = new EmbeddedChannel(new SslClientTlsHandler(context, peer, serverName));
        try {
            SslHandler handler = channel.pipeline().get(SslHandler.class);
            assertNotNull(handler);
            assertEquals(
                    serverName == null || serverName.isEmpty() ? peer.getHostString() : serverName,
                    handler.engine().getPeerHost());
            assertEquals(peer.getPort(), handler.engine().getPeerPort());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    private void assertHandshake(boolean connectionClient, String serverCertificate, boolean expectedSuccess)
            throws Exception {
        Path trust = copyResource("ca.pem");
        SslContext serverContext = SslContextBuilder.forServer(
                        copyResource(serverCertificate).toFile(),
                        copyResource("server.key").toFile())
                .trustManager(trust.toFile())
                .clientAuth(ClientAuth.REQUIRE)
                .sslProvider(SslProvider.JDK)
                .build();
        BlockingQueue<SslHandshakeCompletionEvent> handshakes = new LinkedBlockingQueue<>();
        BlockingQueue<Channel> acceptedChannels = new LinkedBlockingQueue<>();
        EventLoopGroup group = new NioEventLoopGroup(1);
        ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        FrameworkModel frameworkModel = new FrameworkModel();
        AbstractClient client = null;
        try {
            Channel server = new ServerBootstrap()
                    .group(group)
                    .channel(NioServerSocketChannel.class)
                    // Delay server TLS until the client's SslHandler has been captured.
                    .childOption(ChannelOption.AUTO_READ, false)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channels.add(channel);
                            channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void userEventTriggered(ChannelHandlerContext ctx, Object event)
                                        throws Exception {
                                    if (event instanceof SslHandshakeCompletionEvent) {
                                        handshakes.add((SslHandshakeCompletionEvent) event);
                                    }
                                    super.userEventTriggered(ctx, event);
                                }
                            });
                            acceptedChannels.add(channel);
                        }
                    })
                    .bind("127.0.0.1", 0)
                    .sync()
                    .channel();
            channels.add(server);
            ApplicationModel applicationModel = configureClient(frameworkModel, trust);
            // Use mapped loopback so AbstractClient does not replace it with the machine address.
            URL url = URL.valueOf("empty://[::ffff:127.0.0.1]:" + ((InetSocketAddress) server.localAddress()).getPort())
                    .addParameter("codec", "telnet")
                    .setScopeModel(applicationModel.getDefaultModule());
            client = connectionClient
                    ? new NettyConnectionClient(url, new ChannelHandlerAdapter())
                    : new NettyClient(url, new ChannelHandlerAdapter());
            Channel nettyChannel = connectionClient
                    ? ((NettyConnectionClient) client).<Channel>getChannel(Boolean.TRUE)
                    : ((NettyClient) client).getNettyChannel();
            assertNotNull(nettyChannel);
            SslHandler clientTls = nettyChannel.pipeline().get(SslHandler.class);
            assertNotNull(clientTls);
            assertEquals(
                    client.getConnectAddress().getHostString(),
                    clientTls.engine().getPeerHost());
            assertEquals(
                    client.getConnectAddress().getPort(), clientTls.engine().getPeerPort());
            Channel accepted = acceptedChannels.poll(5, TimeUnit.SECONDS);
            assertNotNull(accepted, "The server did not accept the TCP connection");
            accepted.eventLoop()
                    .submit(() -> {
                        accepted.pipeline().addFirst(serverContext.newHandler(accepted.alloc()));
                        accepted.config().setAutoRead(true);
                    })
                    .sync();
            assertTrue(
                    clientTls.handshakeFuture().await(10, TimeUnit.SECONDS),
                    "The client TLS handshake did not complete");
            assertEquals(
                    expectedSuccess,
                    clientTls.handshakeFuture().isSuccess(),
                    () -> "Client TLS handshake: " + clientTls.handshakeFuture().cause());
            if (!expectedSuccess) {
                assertSubjectAlternativeNameMismatch(clientTls.handshakeFuture().cause());
            }
            SslHandshakeCompletionEvent handshake = handshakes.poll(10, TimeUnit.SECONDS);
            assertNotNull(handshake, "The server TLS handshake did not complete");
            assertEquals(expectedSuccess, handshake.isSuccess(), () -> "Server TLS handshake: " + handshake.cause());
        } finally {
            if (client != null) {
                client.close();
            }
            channels.close().awaitUninterruptibly();
            frameworkModel.destroy();
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }

    private void assertSubjectAlternativeNameMismatch(Throwable cause) {
        assertInstanceOf(SSLException.class, cause);
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof CertificateException
                    && current.getMessage() != null
                    && current.getMessage().contains("subject alternative")
                    && current.getMessage().contains("127.0.0.1")) {
                return;
            }
        }
        fail("Expected a certificate SAN mismatch for 127.0.0.1", cause);
    }

    private ApplicationModel configureClient(FrameworkModel frameworkModel, Path trust) throws Exception {
        ApplicationModel applicationModel = frameworkModel.newApplication();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("tls-hostname-test"));
        SslConfig sslConfig = new SslConfig();
        sslConfig.setClientKeyCertChainPath(copyResource("client.pem").toString());
        sslConfig.setClientPrivateKeyPath(copyResource("client.key").toString());
        sslConfig.setClientTrustCertCollectionPath(trust.toString());
        applicationModel.getApplicationConfigManager().setSsl(sslConfig);
        return applicationModel;
    }

    private Path copyResource(String name) throws Exception {
        Path destination = directory.resolve(name);
        try (InputStream stream =
                ClientTlsHostnameVerificationTest.class.getResourceAsStream("/tls-hostname/" + name)) {
            Files.copy(stream, destination);
        }
        return destination;
    }
}
