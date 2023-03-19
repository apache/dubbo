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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.rpc.protocol.rest.PathAndInvokerMapper;
import org.apache.dubbo.rpc.protocol.rest.handler.NettyHttpHandler;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class NettyServer {

    protected ServerBootstrap bootstrap = new ServerBootstrap();
    protected String hostname = null;
    protected int configuredPort = 8080;
    protected int runtimePort = -1;

    protected String root = "";
    private EventLoopGroup eventLoopGroup;
    private int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
    private SSLContext sslContext;
    private int maxRequestSize = 1024 * 1024 * 10;
    private int maxInitialLineLength = 4096;
    private int maxHeaderSize = 8192;
    private int maxChunkSize = 8192;
    private int backlog = 128;
    // default no idle timeout.
    private int idleTimeout = 60;
    private List<ChannelHandler> channelHandlers = Collections.emptyList();
    private Map<ChannelOption, Object> channelOptions = Collections.emptyMap();
    private Map<ChannelOption, Object> childChannelOptions = Collections.emptyMap();
    private List<ChannelHandler> httpChannelHandlers = Collections.emptyList();


    private final RestHttpRequestDecoder restHttpRequestDecoder;


    public NettyServer(PathAndInvokerMapper pathAndInvokerMapper) {
        this.restHttpRequestDecoder = new RestHttpRequestDecoder(new NettyHttpHandler(pathAndInvokerMapper));
    }

    public void setSSLContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }


    /**
     * Specify the worker count to use. For more information about this please see the javadocs of {@link EventLoopGroup}
     *
     * @param ioWorkerCount worker count
     */
    public void setIoWorkerCount(int ioWorkerCount) {
        this.ioWorkerCount = ioWorkerCount;
    }

    /**
     * Set the number of threads to use for the EventExecutor. For more information please see the javadocs of {@link EventExecutor}.
     * If you want to disable the use of the {@link EventExecutor} specify a value {@literal <=} 0.  This should only be done if you are 100% sure that you don't have any blocking
     * code in there.
     *
     * @param executorThreadCount thread count
     */

    /**
     * Set the max. request size in bytes. If this size is exceed we will send a "413 Request Entity Too Large" to the client.
     *
     * @param maxRequestSize the max request size. This is 10mb by default.
     */
    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public void setMaxInitialLineLength(int maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
    }

    public void setMaxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
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

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * Set the idle timeout.
     * Set this value to turn on idle connection cleanup.
     * If there is no traffic within idleTimeoutSeconds, it'll close connection.
     *
     * @param idleTimeoutSeconds - How many seconds to cleanup client connection. default value -1 meaning no idle timeout.
     */
    public void setIdleTimeout(int idleTimeoutSeconds) {
        this.idleTimeout = idleTimeoutSeconds;
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
     * Add additional {@link io.netty.channel.ChannelHandler}s to the {@link io.netty.bootstrap.ServerBootstrap}.
     * <p>The additional channel handlers are being added <em>after</em> the HTTP handling.</p>
     *
     * @param httpChannelHandlers the additional {@link io.netty.channel.ChannelHandler}s.
     */
    public void setHttpChannelHandlers(final List<ChannelHandler> httpChannelHandlers) {
        this.httpChannelHandlers = httpChannelHandlers == null ? Collections.<ChannelHandler>emptyList() : httpChannelHandlers;
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


    public void start(URL url) {
        eventLoopGroup = new NioEventLoopGroup(ioWorkerCount,url.getOrDefaultFrameworkModel().getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url));

        // Configure the server.
        bootstrap.group(eventLoopGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(createChannelInitializer())
            .option(ChannelOption.SO_BACKLOG, backlog)
            .childOption(ChannelOption.SO_KEEPALIVE, true);

        for (Map.Entry<ChannelOption, Object> entry : channelOptions.entrySet()) {
            bootstrap.option(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<ChannelOption, Object> entry : childChannelOptions.entrySet()) {
            bootstrap.childOption(entry.getKey(), entry.getValue());
        }

        final InetSocketAddress socketAddress;
        if (null == hostname || hostname.isEmpty()) {
            socketAddress = new InetSocketAddress(configuredPort);
        } else {
            socketAddress = new InetSocketAddress(hostname, configuredPort);
        }

        Channel channel = bootstrap.bind(socketAddress).syncUninterruptibly().channel();
        runtimePort = ((InetSocketAddress) channel.localAddress()).getPort();
    }

    private ChannelInitializer<SocketChannel> createChannelInitializer() {
        if (sslContext == null) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    setupHandlers(ch, "http");
                }
            };
        } else {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
//                    ch.pipeline().addFirst(new SniHandler());
                    setupHandlers(ch, "https");
                }
            };
        }
    }

    protected void setupHandlers(SocketChannel ch, String protocol) {
        ChannelPipeline channelPipeline = ch.pipeline();
        channelPipeline.addLast(channelHandlers.toArray(new ChannelHandler[channelHandlers.size()]));
        if (idleTimeout > 0) {
            channelPipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, idleTimeout));
        }
        channelPipeline.addLast(new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize));
        channelPipeline.addLast(new HttpObjectAggregator(maxRequestSize));
        channelPipeline.addLast(new HttpResponseEncoder());
        channelPipeline.addLast(httpChannelHandlers.toArray(new ChannelHandler[httpChannelHandlers.size()]));
        restHttpRequestDecoder.setProto(protocol);
        channelPipeline.addLast(restHttpRequestDecoder);

    }


    public void stop() {
        runtimePort = -1;
        eventLoopGroup.shutdownGracefully();
    }
}
