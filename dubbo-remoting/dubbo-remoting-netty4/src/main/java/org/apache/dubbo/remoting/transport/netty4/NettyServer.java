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
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.registry.event.NettyEvent;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.AbstractServer;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelHandlers;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslServerTlsHandler;
import org.apache.dubbo.remoting.utils.UrlUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.KEEP_ALIVE_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE;
import static org.apache.dubbo.remoting.Constants.EVENT_LOOP_BOSS_POOL_NAME;
import static org.apache.dubbo.remoting.Constants.EVENT_LOOP_WORKER_POOL_NAME;

/**
 * NettyServer.
 */
public class NettyServer extends AbstractServer {

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

    public NettyServer(URL url, ChannelHandler handler) throws RemotingException {
        // you can customize name and type of client thread pool by THREAD_NAME_KEY and THREAD_POOL_KEY in
        // CommonConstants.
        // the handler will be wrapped: MultiMessageHandler->HeartbeatHandler->handler
        super(url, ChannelHandlers.wrap(handler, url));

        // read config before destroy
        serverShutdownTimeoutMills = ConfigurationUtils.getServerShutdownTimeout(getUrl().getOrDefaultModuleModel());
    }

    /**
     * Init and start netty server
     *
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {
        bootstrap = new ServerBootstrap();

        bossGroup = createBossGroup();
        workerGroup = createWorkerGroup();

        final NettyServerHandler nettyServerHandler = createNettyServerHandler();
        channels = nettyServerHandler.getChannels();

        initServerBootstrap(nettyServerHandler);

        // bind
        try {
            ChannelFuture channelFuture = bootstrap.bind(getBindAddress());
            channelFuture.syncUninterruptibly();
            channel = channelFuture.channel();
        } catch (Throwable t) {
            closeBootstrap();
            throw t;
        }

        // metrics
        if (isSupportMetrics()) {
            ApplicationModel applicationModel = ApplicationModel.defaultModel();
            MetricsEventBus.post(NettyEvent.toNettyEvent(applicationModel), () -> {
                Map<String, Long> dataMap = new HashMap<>();
                dataMap.put(
                        MetricsKey.NETTY_ALLOCATOR_HEAP_MEMORY_USED.getName(),
                        PooledByteBufAllocator.DEFAULT.metric().usedHeapMemory());
                dataMap.put(
                        MetricsKey.NETTY_ALLOCATOR_DIRECT_MEMORY_USED.getName(),
                        PooledByteBufAllocator.DEFAULT.metric().usedDirectMemory());
                dataMap.put(MetricsKey.NETTY_ALLOCATOR_HEAP_ARENAS_NUM.getName(), (long)
                        PooledByteBufAllocator.DEFAULT.numHeapArenas());
                dataMap.put(MetricsKey.NETTY_ALLOCATOR_DIRECT_ARENAS_NUM.getName(), (long)
                        PooledByteBufAllocator.DEFAULT.numDirectArenas());
                dataMap.put(MetricsKey.NETTY_ALLOCATOR_NORMAL_CACHE_SIZE.getName(), (long)
                        PooledByteBufAllocator.DEFAULT.normalCacheSize());
                dataMap.put(MetricsKey.NETTY_ALLOCATOR_SMALL_CACHE_SIZE.getName(), (long)
                        PooledByteBufAllocator.DEFAULT.smallCacheSize());
                dataMap.put(MetricsKey.NETTY_ALLOCATOR_THREAD_LOCAL_CACHES_NUM.getName(), (long)
                        PooledByteBufAllocator.DEFAULT.numThreadLocalCaches());
                dataMap.put(MetricsKey.NETTY_ALLOCATOR_CHUNK_SIZE.getName(), (long)
                        PooledByteBufAllocator.DEFAULT.chunkSize());
                return dataMap;
            });
        }
    }

    private boolean isSupportMetrics() {
        return ClassUtils.isPresent("io.netty.buffer.PooledByteBufAllocatorMetric", NettyServer.class.getClassLoader());
    }

    protected EventLoopGroup createBossGroup() {
        return NettyEventLoopFactory.eventLoopGroup(1, EVENT_LOOP_BOSS_POOL_NAME);
    }

    protected EventLoopGroup createWorkerGroup() {
        return NettyEventLoopFactory.eventLoopGroup(
                getUrl().getPositiveParameter(IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                EVENT_LOOP_WORKER_POOL_NAME);
    }

    protected NettyServerHandler createNettyServerHandler() {
        return new NettyServerHandler(getUrl(), this);
    }

    protected void initServerBootstrap(NettyServerHandler nettyServerHandler) {
        boolean keepalive = getUrl().getParameter(KEEP_ALIVE_KEY, Boolean.FALSE);
        bootstrap
                .group(bossGroup, workerGroup)
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_KEEPALIVE, keepalive)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        int closeTimeout = UrlUtils.getCloseTimeout(getUrl());
                        NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                        ch.pipeline().addLast("negotiation", new SslServerTlsHandler(getUrl()));
                        ch.pipeline()
                                .addLast("decoder", adapter.getDecoder())
                                .addLast("encoder", adapter.getEncoder())
                                .addLast("server-idle-handler", new IdleStateHandler(0, 0, closeTimeout, MILLISECONDS))
                                .addLast("handler", nettyServerHandler);
                    }
                });
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
                Future<?> workerGroupShutdownFuture =
                        workerGroup.shutdownGracefully(quietPeriod, timeout, MILLISECONDS);
                bossGroupShutdownFuture.syncUninterruptibly();
                workerGroupShutdownFuture.syncUninterruptibly();
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

    protected EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    protected EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    protected ServerBootstrap getServerBootstrap() {
        return bootstrap;
    }

    protected io.netty.channel.Channel getBossChannel() {
        return channel;
    }

    protected Map<String, Channel> getServerChannels() {
        return channels;
    }
}
