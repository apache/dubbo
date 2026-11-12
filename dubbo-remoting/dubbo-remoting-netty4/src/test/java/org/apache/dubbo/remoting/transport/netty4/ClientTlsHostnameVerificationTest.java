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
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.AbstractClient;
import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import javax.net.ssl.SSLException;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    private void assertHandshake(boolean connectionClient, String serverCertificate, boolean expectedSuccess)
            throws Exception {
        Path certificate = copyResource("client.pem");
        Path privateKey = copyResource("client.key");
        Path trust = copyResource("ca.pem");
        SslContext serverContext = SslContextBuilder.forServer(
                        copyResource(serverCertificate).toFile(),
                        copyResource("server.key").toFile())
                .trustManager(trust.toFile())
                .clientAuth(ClientAuth.REQUIRE)
                .sslProvider(SslProvider.JDK)
                .build();
        BlockingQueue<SslHandshakeCompletionEvent> handshakes = new LinkedBlockingQueue<>();
        EventLoopGroup group = new NioEventLoopGroup(1);
        ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        FrameworkModel frameworkModel = new FrameworkModel();
        AbstractClient client = null;
        try {
            Channel server = new ServerBootstrap()
                    .group(group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channels.add(channel);
                            channel.pipeline().addLast(serverContext.newHandler(channel.alloc()));
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
                        }
                    })
                    .bind("127.0.0.1", 0)
                    .sync()
                    .channel();
            channels.add(server);
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("tls-hostname-test"));
            SslConfig sslConfig = new SslConfig();
            sslConfig.setClientKeyCertChainPath(certificate.toString());
            sslConfig.setClientPrivateKeyPath(privateKey.toString());
            sslConfig.setClientTrustCertCollectionPath(trust.toString());
            applicationModel.getApplicationConfigManager().setSsl(sslConfig);
            // Use mapped loopback so AbstractClient does not replace it with the machine address.
            URL url = URL.valueOf("empty://[::ffff:127.0.0.1]:" + ((InetSocketAddress) server.localAddress()).getPort())
                    .addParameter("codec", "telnet")
                    .setScopeModel(applicationModel.getDefaultModule());
            try {
                client = connectionClient
                        ? new NettyConnectionClient(url, new ChannelHandlerAdapter())
                        : new NettyClient(url, new ChannelHandlerAdapter());
            } catch (RemotingException exception) {
                if (expectedSuccess) {
                    throw exception;
                }
            }
            SslHandshakeCompletionEvent handshake = handshakes.poll(10, TimeUnit.SECONDS);
            assertNotNull(handshake, "The TLS handshake did not complete");
            assertEquals(expectedSuccess, handshake.isSuccess(), () -> "TLS handshake: " + handshake.cause());
            if (!expectedSuccess) {
                assertInstanceOf(SSLException.class, handshake.cause());
            }
        } finally {
            if (client != null) {
                client.close();
            }
            channels.close().awaitUninterruptibly();
            frameworkModel.destroy();
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
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
