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
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.http3.Http3SslContexts;
import org.apache.dubbo.remoting.transport.AbstractServer;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelHandlers;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE;
import static org.apache.dubbo.remoting.Constants.EVENT_LOOP_BOSS_POOL_NAME;
import static org.apache.dubbo.remoting.http3.netty4.Constants.PIPELINE_CONFIGURATOR_KEY;

public class NettyHttp3Server extends AbstractServer {

    private Map<String, Channel> channels;
    private Bootstrap bootstrap;
    private EventLoopGroup bossGroup;
    private io.netty.channel.Channel channel;

    private final Consumer<ChannelPipeline> pipelineConfigurator;
    private final int serverShutdownTimeoutMills;

    @SuppressWarnings("unchecked")
    public NettyHttp3Server(URL url, ChannelHandler handler) throws RemotingException {
        super(url, ChannelHandlers.wrap(handler, url));
        pipelineConfigurator = (Consumer<ChannelPipeline>) getUrl().getAttribute(PIPELINE_CONFIGURATOR_KEY);
        Objects.requireNonNull(pipelineConfigurator, "pipelineConfigurator should be set");
        serverShutdownTimeoutMills = ConfigurationUtils.getServerShutdownTimeout(getUrl().getOrDefaultModuleModel());
    }

    @Override
    protected void doOpen() throws Throwable {
        bootstrap = new Bootstrap();
        bossGroup = NettyEventLoopFactory.eventLoopGroup(1, EVENT_LOOP_BOSS_POOL_NAME);
        NettyServerHandler nettyServerHandler = new NettyServerHandler(getUrl(), this);
        channels = nettyServerHandler.getChannels();
        try {
            ChannelFuture channelFuture = bootstrap
                    .group(bossGroup)
                    .channel(NioDatagramChannel.class)
                    .handler(Http3Helper.configCodec(Http3.newQuicServerCodecBuilder(), getUrl())
                            .sslContext(Http3SslContexts.buildServerSslContext(getUrl()))
                            .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                            .handler(new ChannelInitializer<QuicChannel>() {
                                @Override
                                protected void initChannel(QuicChannel ch) {
                                    ChannelPipeline pipeline = ch.pipeline();
                                    pipelineConfigurator.accept(pipeline);
                                    pipeline.addLast(nettyServerHandler);
                                }
                            })
                            .build())
                    .bind(getBindAddress());
            channelFuture.syncUninterruptibly();
            channel = channelFuture.channel();
        } catch (Throwable t) {
            closeBootstrap();
            throw t;
        }
    }

    @Override
    protected void doClose() {
        try {
            if (channel != null) {
                // unbind.
                channel.close();
            }
        } catch (Throwable e) {
            logger.warn(TRANSPORT_FAILED_CLOSE, "", "", e.getMessage(), e);
        }
        try {
            Collection<Channel> channels = getChannels();
            if (CollectionUtils.isNotEmpty(channels)) {
                for (Channel channel : channels) {
                    try {
                        channel.close();
                    } catch (Throwable e) {
                        logger.warn(TRANSPORT_FAILED_CLOSE, "", "", e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn(TRANSPORT_FAILED_CLOSE, "", "", e.getMessage(), e);
        }
        closeBootstrap();
        try {
            if (channels != null) {
                channels.clear();
            }
        } catch (Throwable e) {
            logger.warn(TRANSPORT_FAILED_CLOSE, "", "", e.getMessage(), e);
        }
    }

    private void closeBootstrap() {
        try {
            if (bootstrap != null) {
                long timeout = ConfigurationUtils.reCalShutdownTime(serverShutdownTimeoutMills);
                long quietPeriod = Math.min(2000L, timeout);
                Future<?> bossGroupShutdownFuture = bossGroup.shutdownGracefully(quietPeriod, timeout, MILLISECONDS);
                bossGroupShutdownFuture.syncUninterruptibly();
            }
        } catch (Throwable e) {
            logger.warn(TRANSPORT_FAILED_CLOSE, "", "", e.getMessage(), e);
        }
    }

    @Override
    protected int getChannelsSize() {
        return channels.size();
    }

    @Override
    public Collection<Channel> getChannels() {
        return new ArrayList<>(channels.values());
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
