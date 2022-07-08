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
package org.apache.dubbo.remoting.transport.netty4.portunification;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.NettyEventLoopFactory;
import org.apache.dubbo.remoting.api.newportunification.AbstractPortUnificationServer;
import org.apache.dubbo.remoting.api.newportunification.PortUnificationServerHandlerDelegate;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelHandlers;
import org.apache.dubbo.remoting.transport.netty4.NettyBackedChannelBuffer;
import org.apache.dubbo.remoting.utils.UrlUtils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.KEEP_ALIVE_KEY;
import static org.apache.dubbo.remoting.Constants.EVENT_LOOP_BOSS_POOL_NAME;
import static org.apache.dubbo.remoting.Constants.EVENT_LOOP_WORKER_POOL_NAME;

// server is a handler to manage remote channel
public class NettyPortUnificationServer extends AbstractPortUnificationServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyPortUnificationServer.class);

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

    private class InternalEncoder extends MessageToByteEncoder {

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
            ChannelBuffer buffer = new NettyBackedChannelBuffer(out);
            // channel should have been created here,
            // so passing null pointer to handler has no effect
            NettyChannelWithHandler channel = NettyChannelWithHandler.getOrAddChannel(ctx.channel(), null);
            if(channel.getUrl() != null) {
                getChannelCodec(channel.getUrl()).encode(channel, buffer, msg);
            }
        }
    }

    private class InternalDecoder extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf input, List<Object> out) throws Exception {

            ChannelBuffer message = new NettyBackedChannelBuffer(input);
            // channel should have been created here,
            // so passing null pointer to handler has no effect
            NettyChannelWithHandler channel = NettyChannelWithHandler.getOrAddChannel(ctx.channel(), null);
            if(channel.getUrl() == null) {
                // 这里调用的应该是检测协议的方法
                // here channel call portUnificationServerHandlerDelegate
                channel.received(channel, message);
                if(channel.getUrl() == null) {
                    return;
                }
                // if url is not null, protocol recognition process is finished
                // time to decode,
            }
            Codec2 codec = getChannelCodec(channel.getUrl());
            // decode object.
            do {
                int saveReaderIndex = message.readerIndex();
                Object msg = codec.decode(channel, message);
                if (msg == Codec2.DecodeResult.NEED_MORE_INPUT) {
                    message.readerIndex(saveReaderIndex);
                    break;
                } else {
                    //is it possible to go here ?
                    if (saveReaderIndex == message.readerIndex()) {
                        throw new IOException("Decode without read data.");
                    }
                    if (msg != null) {
                        out.add(msg);
                    }
                }
            } while (message.readable());
        }
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
                    // FIXME: should we use getTimeout()?
                    int idleTimeout = UrlUtils.getIdleTimeout(getUrl());
                    // 通过port统一的包装server自身的handler
                    PortUnificationServerHandlerDelegate handler = new PortUnificationServerHandlerDelegate(
                        NettyPortUnificationServer.this.getProtocols(),
                        NettyPortUnificationServer.this.getWireProtocolURLConcurrentMap(),
                        NettyPortUnificationServer.this
                    );

                    final NettyPortUnificationServerHandler serverHandler =
                        new NettyPortUnificationServerHandler(handler);
                    // message in pu-handler decoder
                    // message out encoder pu-handler
                    ch.pipeline()
                        .addLast("encoder", new NettyPortUnificationServer.InternalEncoder())
                        .addLast("decoder", new NettyPortUnificationServer.InternalDecoder())
                        .addLast("server-idle-handler", new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS))
                        .addLast("pu-handler", serverHandler);
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
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
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
