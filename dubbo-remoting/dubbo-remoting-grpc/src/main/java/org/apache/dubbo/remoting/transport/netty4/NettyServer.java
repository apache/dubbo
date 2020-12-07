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

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.transport.AbstractServer;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelHandlers;
import org.apache.dubbo.remoting.transport.netty4.h2.Http2OrHttpHandler;
import org.apache.dubbo.remoting.utils.UrlUtils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.net.ssl.SSLException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SSL_ENABLED_KEY;

/**
 * NettyServer.
 */
public class NettyServer extends AbstractServer implements RemotingServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    /**
     * the cache for alive worker channel.
     * <ip:port, dubbo channel>
     */
    private Map<String, Channel> channels;
    /**
     * netty server bootstrap.
     */
    private ServerBootstrap bootstrap;
    /**
     * the boss channel that receive connections and dispatch these to worker channel.
     */
	private io.netty.channel.Channel channel;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(URL url, ChannelHandler handler) throws RemotingException {
        // you can customize name and type of client thread pool by THREAD_NAME_KEY and THREADPOOL_KEY in CommonConstants.
        // the handler will be wrapped: MultiMessageHandler->HeartbeatHandler->handler
        super(ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME), ChannelHandlers.wrap(handler, url));
    }

    /**
     * Init and start netty server
     *
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {

        bossGroup = NettyEventLoopFactory.eventLoopGroup(1, "NettyServerBoss");
        workerGroup = NettyEventLoopFactory.eventLoopGroup(
            getUrl().getPositiveParameter(IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
            "NettyServerWorker");

        bossGroup = new NioEventLoopGroup(); // (1)
        //final MethodContainer container = ServiceProviders.load(MethodContainer.class);
        //for (DiscoverableService<?> service : services) {
        //    for (Method declaredMethod : service.getInterfaceClass().getDeclaredMethods()) {
        //        ServerMethodModel model = new ShinyServerMethodModel(service.getEventLoopGroup(), service.getImpl(), declaredMethod.getName(), service.getName(), declaredMethod);
        //        for (String path : model.getPaths()) {
        //            container.add(path, model);
        //        }
        //    }
        //}
        final boolean ssl = getUrl().getParameter(SSL_ENABLED_KEY, false);
        final SslContext sslCtx;
        if (ssl) {
            SelfSignedCertificate ssc = null;
            try {
                ssc = new SelfSignedCertificate();
            } catch (CertificateException e) {
                // todo add unified exception handler
                throw new RuntimeException(e);
            }
            try {
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                    .sslProvider(SslProvider.OPENSSL)
                    /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
                     * Please refer to the HTTP/2 specification for cipher requirements. */
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                    .build();
            } catch (SSLException e) {
                // todo add unified exception handler
                throw new RuntimeException(e);
            }
        } else {
            sslCtx = null;
        }
        ServerBootstrap b = new ServerBootstrap(); // (2)
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class) // (3)
            .option(ChannelOption.SO_BACKLOG, 128)          // (5)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    // FIXME: should we use getTimeout()?
                    int idleTimeout = UrlUtils.getIdleTimeout(getUrl());
                    NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                    if (ssl && sslCtx != null) {
                        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new Http2OrHttpHandler());
                    }
                    ch.pipeline()
                        .addLast(ExceptionHandler.INSTANCE)
                        .addLast("protocol_detector", new ServerProtocolDetector());
                }
            }); // (6)

        // Bind and start to accept incoming connections.
        ChannelFuture f = b.bind(getBindAddress()).syncUninterruptibly();

        channel = f.channel();
        //LifeCycleManager.addApp(this);

    }

    @Override
    protected void doClose() throws Throwable {
        try {
            if (channel != null) {
                // unbind.
                channel.close();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            Collection<org.apache.dubbo.remoting.Channel> channels = getChannels();
            if (channels != null && channels.size() > 0) {
                for (org.apache.dubbo.remoting.Channel channel : channels) {
                    try {
                        channel.close();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully().syncUninterruptibly();
                workerGroup.shutdownGracefully().syncUninterruptibly();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (channels != null) {
                channels.clear();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public Collection<Channel> getChannels() {
        Collection<Channel> chs = new HashSet<Channel>();
        for (Channel channel : this.channels.values()) {
            if (channel.isConnected()) {
                chs.add(channel);
            } else {
                channels.remove(NetUtils.toAddressString(channel.getRemoteAddress()));
            }
        }
        return chs;
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return channels.get(NetUtils.toAddressString(remoteAddress));
    }

    @Override
    public boolean canHandleIdle() {
        return true;
    }

    @Override
    public boolean isBound() {
        return channel.isActive();
    }

}
