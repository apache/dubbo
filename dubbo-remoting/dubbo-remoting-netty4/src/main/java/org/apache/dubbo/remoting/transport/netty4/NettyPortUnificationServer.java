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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.NettyEventLoopFactory;
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.remoting.api.pu.AbstractPortUnificationServer;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelHandlers;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.KEEP_ALIVE_KEY;
import static org.apache.dubbo.remoting.Constants.EVENT_LOOP_BOSS_POOL_NAME;
import static org.apache.dubbo.remoting.Constants.EVENT_LOOP_WORKER_POOL_NAME;

// server is a handler to manage remote channel
public class NettyPortUnificationServer extends AbstractPortUnificationServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyPortUnificationServer.class);


    private final DefaultChannelGroup channelGroup = new DefaultChannelGroup(
        GlobalEventExecutor.INSTANCE);
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
    private final int serverShutdownTimeoutMills;

    public NettyPortUnificationServer(URL url, ChannelHandler handler) throws RemotingException {
        // new add url also need to be wrapped
        super(ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME), ChannelHandlers.wrap(handler, url));

        // read config before destroy
        serverShutdownTimeoutMills = ConfigurationUtils.getServerShutdownTimeout(getUrl().getOrDefaultModuleModel());
    }

    protected void initServerBootstrap() {
        boolean keepalive = getUrl().getParameter(KEEP_ALIVE_KEY, Boolean.FALSE);
        logger.debug("start create port unification netty server");
        bootstrap.group(bossGroup, workerGroup)
            .channel(NettyEventLoopFactory.serverSocketChannelClass())
            .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
            .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
            .childOption(ChannelOption.SO_KEEPALIVE, keepalive)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {

                    final NettyServerHandler serverHandler =
                        new NettyServerHandler(NettyPortUnificationServer.this.getUrl(),NettyPortUnificationServer.this);
                    ch.pipeline()
                        .addLast("negotiation-handler", serverHandler);
                }
            });
    }


    @Override
    protected void doOpen() throws Throwable {
        bootstrap = new ServerBootstrap();

        bossGroup = NettyEventLoopFactory.eventLoopGroup(1, EVENT_LOOP_BOSS_POOL_NAME);;
        workerGroup =  NettyEventLoopFactory.eventLoopGroup(
            getUrl().getPositiveParameter(IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
            EVENT_LOOP_WORKER_POOL_NAME);

        channels = new HashMap<>();
        initServerBootstrap();

        // bind
        ChannelFuture channelFuture = bootstrap.bind(getBindAddress());
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();
    }

    @Override
    protected void doClose() throws Throwable {
        try {
            if (channel != null) {
                // unbind.
                channel.close();
            }
            channelGroup.close().await(serverShutdownTimeoutMills);
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        for (WireProtocol protocol: getProtocols()) {
            protocol.close();
        }
        try {
            Collection<Channel> channels = getChannels();
            if (CollectionUtils.isNotEmpty(channels)) {
                for (Channel channel : channels) {
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
                long timeout = serverShutdownTimeoutMills;
                long quietPeriod = Math.min(2000L, timeout);
                Future<?> bossGroupShutdownFuture = bossGroup.shutdownGracefully(quietPeriod, timeout, MILLISECONDS);
                Future<?> workerGroupShutdownFuture = workerGroup.shutdownGracefully(quietPeriod, timeout, MILLISECONDS);
                bossGroupShutdownFuture.syncUninterruptibly();
                workerGroupShutdownFuture.syncUninterruptibly();
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
    public boolean isBound() {
        return channel.isActive();
    }

    @Override
    public Collection<Channel> getChannels() {
        Collection<Channel> chs = new ArrayList<>(this.channels.size());
        // pick channels from NettyServerHandler ( needless to check connectivity )
        chs.addAll(this.channels.values());
        return chs;
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return channels.get(NetUtils.toAddressString(remoteAddress));
    }
}
