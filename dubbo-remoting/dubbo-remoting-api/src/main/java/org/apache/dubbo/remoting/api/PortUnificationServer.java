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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.utils.UrlUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.Provider;
import java.security.Security;
import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SSL_ENABLED_KEY;

/**
 * PortUnificationServer.
 */
public class PortUnificationServer {

    private static final Logger logger = LoggerFactory.getLogger(PortUnificationServer.class);
    private final List<WireProtocol> protocols;
    private final URL url;
    /**
     * netty server bootstrap.
     */
    private ServerBootstrap bootstrap;
    /**
     * the boss channel that receive connections and dispatch these to worker channel.
     */
    private Channel channel;
    private DefaultChannelGroup channelGroup;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public PortUnificationServer(URL url) {
        // you can customize name and type of client thread pool by THREAD_NAME_KEY and THREADPOOL_KEY in CommonConstants.
        // the handler will be wrapped: MultiMessageHandler->HeartbeatHandler->handler
        this.url = ExecutorUtil.setThreadName(url, "DubboPUServerHandler");
        this.protocols = ExtensionLoader.getExtensionLoader(WireProtocol.class).getActivateExtension(url, new String[0]);
    }

    private static boolean checkJdkProvider() {
        Provider[] jdkProviders = Security.getProviders("SSLContext.TLS");
        return (jdkProviders != null && jdkProviders.length > 0);
    }

    private static SslProvider findSslProvider() {
        if (OpenSsl.isAvailable()) {
            logger.info("Using OPENSSL provider.");
            return SslProvider.OPENSSL;
        } else if (checkJdkProvider()) {
            logger.info("Using JDK provider.");
            return SslProvider.JDK;
        }
        throw new IllegalStateException(
                "Could not find any valid TLS provider, please check your dependency or deployment environment, " +
                        "usually netty-tcnative, Conscrypt, or Jetty NPN/ALPN is needed.");
    }

    public static SslContext buildServerSslContext(URL url) {
        ConfigManager globalConfigManager = ApplicationModel.getConfigManager();
        SslConfig sslConfig = globalConfigManager.getSsl().orElseThrow(() -> new IllegalStateException("Ssl enabled, but no ssl cert information provided!"));

        SslContextBuilder sslClientContextBuilder = null;
        try {
            String password = sslConfig.getServerKeyPassword();
            if (password != null) {
                sslClientContextBuilder = SslContextBuilder.forServer(sslConfig.getServerKeyCertChainPathStream(),
                        sslConfig.getServerPrivateKeyPathStream(), password);
            } else {
                sslClientContextBuilder = SslContextBuilder.forServer(sslConfig.getServerKeyCertChainPathStream(),
                        sslConfig.getServerPrivateKeyPathStream());
            }

            if (sslConfig.getServerTrustCertCollectionPathStream() != null) {
                sslClientContextBuilder.trustManager(sslConfig.getServerTrustCertCollectionPathStream());
                sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", e);
        }
        try {
            return sslClientContextBuilder.sslProvider(findSslProvider()).build();
        } catch (SSLException e) {
            throw new IllegalStateException("Build SslSession failed.", e);
        }
    }

    public URL getUrl() {
        return url;
    }

    public void bind() {
        if (channel == null) {
            doOpen();
        }
    }

    public void close() throws Throwable {
        if (channel != null) {
            doClose();
        }
    }

    /**
     * Init and start netty server
     */
    protected void doOpen() {
        bootstrap = new ServerBootstrap();

        bossGroup = NettyEventLoopFactory.eventLoopGroup(1, "NettyServerBoss");
        workerGroup = NettyEventLoopFactory.eventLoopGroup(
                getUrl().getPositiveParameter(IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                "NettyServerWorker");

        bootstrap.group(bossGroup, workerGroup)
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // FIXME: should we use getTimeout()?
                        int idleTimeout = UrlUtils.getIdleTimeout(getUrl());
                        final ChannelPipeline p = ch.pipeline();
//                        p.addLast(new LoggingHandler(LogLevel.DEBUG));
                        // TODO add SSL support
                        final boolean enableSSL = getUrl().getParameter(SSL_ENABLED_KEY, false);
                        final PortUnificationServerHandler puHandler;
                        if (enableSSL) {
                            final SslContext sslContext = buildServerSslContext(getUrl());
                            puHandler = new PortUnificationServerHandler(sslContext, protocols);
                        } else {
                            puHandler = new PortUnificationServerHandler(protocols);
                        }
                        p.addLast("server-idle-handler", new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS));
                        p.addLast("negotiation", puHandler);
                        channelGroup = puHandler.getChannels();
                    }
                });
        // bind

        String bindIp = getUrl().getParameter(Constants.BIND_IP_KEY, getUrl().getHost());
        int bindPort = getUrl().getParameter(Constants.BIND_PORT_KEY, getUrl().getPort());
        if (url.getParameter(ANYHOST_KEY, false) || NetUtils.isInvalidLocalHost(bindIp)) {
            bindIp = ANYHOST_VALUE;
        }
        InetSocketAddress bindAddress = new InetSocketAddress(bindIp, bindPort);
        ChannelFuture channelFuture = bootstrap.bind(bindAddress);
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();
    }

    protected void doClose() throws Throwable {
        final long st = System.currentTimeMillis();

        try {
            if (channel != null) {
                // unbind.
                channel.close();
                channel = null;
            }

            if (channelGroup != null) {
                ChannelGroupFuture closeFuture = channelGroup.close();
                closeFuture.await(15000);
            }
            final long cost = System.currentTimeMillis() - st;
            logger.info("Port unification server closed. cost:" + cost);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while shutting down", e);
        }

        for (WireProtocol protocol : protocols) {
            protocol.close();
        }

        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully().syncUninterruptibly();
                workerGroup.shutdownGracefully().syncUninterruptibly();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public boolean isBound() {
        return channel.isActive();
    }

    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) channel.localAddress();
    }

}
