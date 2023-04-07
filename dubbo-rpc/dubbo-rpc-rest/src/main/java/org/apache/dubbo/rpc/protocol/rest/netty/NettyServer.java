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

package org.apache.dubbo.rpc.protocol.rest.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.remoting.Constants.EVENT_LOOP_BOSS_POOL_NAME;
import static org.apache.dubbo.remoting.Constants.EVENT_LOOP_WORKER_POOL_NAME;


public class NettyServer {

    protected ServerBootstrap bootstrap = new ServerBootstrap();
    protected String hostname = null;
    protected int configuredPort = 8080;
    protected int runtimePort = -1;

    private EventLoopGroup eventLoopGroup;
    private EventLoopGroup workerLoopGroup;
    private int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;

    private List<ChannelHandler> channelHandlers = Collections.emptyList();
    private Map<ChannelOption, Object> channelOptions = Collections.emptyMap();
    private Map<ChannelOption, Object> childChannelOptions = Collections.emptyMap();
    private UnSharedHandlerCreator unSharedHandlerCallBack;

    public NettyServer() {
    }


    /**
     * Specify the worker count to use. For more information about this please see the javadocs of {@link EventLoopGroup}
     *
     * @param ioWorkerCount worker count
     */
    public void setIoWorkerCount(int ioWorkerCount) {
        this.ioWorkerCount = ioWorkerCount;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return runtimePort > 0 ? runtimePort : configuredPort;
    }

    public void setPort(int port) {
        this.configuredPort = port;
    }


    /**
     * Add additional {@link io.netty.channel.ChannelHandler}s to the {@link io.netty.bootstrap.ServerBootstrap}.
     * <p>The additional channel handlers are being added <em>before</em> the HTTP handling.</p>
     *
     * @param channelHandlers the additional {@link io.netty.channel.ChannelHandler}s.
     */
    public void setChannelHandlers(final List<ChannelHandler> channelHandlers) {
        this.channelHandlers = channelHandlers == null ? Collections.<ChannelHandler>emptyList() : channelHandlers;
    }

    /**
     * Add Netty {@link io.netty.channel.ChannelOption}s to the {@link io.netty.bootstrap.ServerBootstrap}.
     *
     * @param channelOptions the additional {@link io.netty.channel.ChannelOption}s.
     * @see io.netty.bootstrap.ServerBootstrap#option(io.netty.channel.ChannelOption, Object)
     */
    public void setChannelOptions(final Map<ChannelOption, Object> channelOptions) {
        this.channelOptions = channelOptions == null ? Collections.<ChannelOption, Object>emptyMap() : channelOptions;
    }

    /**
     * Add child options to the {@link io.netty.bootstrap.ServerBootstrap}.
     *
     * @param channelOptions the additional child {@link io.netty.channel.ChannelOption}s.
     * @see io.netty.bootstrap.ServerBootstrap#childOption(io.netty.channel.ChannelOption, Object)
     */
    public void setChildChannelOptions(final Map<ChannelOption, Object> channelOptions) {
        this.childChannelOptions = channelOptions == null ? Collections.<ChannelOption, Object>emptyMap() : channelOptions;
    }

    public void setUnSharedHandlerCallBack(UnSharedHandlerCreator unSharedHandlerCallBack) {
        this.unSharedHandlerCallBack = unSharedHandlerCallBack;
    }

    public void start(URL url) {
        eventLoopGroup = new NioEventLoopGroup(1, new NamedThreadFactory(EVENT_LOOP_BOSS_POOL_NAME));
        workerLoopGroup = new NioEventLoopGroup(ioWorkerCount, new NamedThreadFactory(EVENT_LOOP_WORKER_POOL_NAME));

        // Configure the server.
        bootstrap.group(eventLoopGroup, workerLoopGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(setupHandlers(url));


        for (Map.Entry<ChannelOption, Object> entry : channelOptions.entrySet()) {
            bootstrap.option(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<ChannelOption, Object> entry : childChannelOptions.entrySet()) {
            bootstrap.childOption(entry.getKey(), entry.getValue());
        }

        final InetSocketAddress socketAddress;
        if (null == getHostname() || getHostname().isEmpty()) {
            socketAddress = new InetSocketAddress(configuredPort);
        } else {
            socketAddress = new InetSocketAddress(hostname, configuredPort);
        }

        Channel channel = bootstrap.bind(socketAddress).syncUninterruptibly().channel();
        runtimePort = ((InetSocketAddress) channel.localAddress()).getPort();
    }


    protected ChannelHandler setupHandlers(URL url) {

        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline channelPipeline = ch.pipeline();

                int idleTimeout = url.getParameter(RestConstant.IDLE_TIMEOUT_PARAM, RestConstant.IDLE_TIMEOUT);
                if (idleTimeout > 0) {
                    channelPipeline.addLast(new IdleStateHandler(0, 0, idleTimeout));
                }

                channelPipeline.addLast(channelHandlers.toArray(new ChannelHandler[channelHandlers.size()]));

                List<ChannelHandler> unSharedHandlers = unSharedHandlerCallBack.getUnSharedHandlers(url);

                for (ChannelHandler unSharedHandler : unSharedHandlers) {
                    channelPipeline.addLast(unSharedHandler);
                }

            }
        };

    }


    public void stop() {
        runtimePort = -1;
        eventLoopGroup.shutdownGracefully();
    }
}
