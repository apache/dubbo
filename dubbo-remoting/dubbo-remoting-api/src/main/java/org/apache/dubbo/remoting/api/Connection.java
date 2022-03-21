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
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CLIENT_THREADPOOL;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.remoting.api.NettyEventLoopFactory.socketChannelClass;

public class Connection extends AbstractReferenceCounted implements ReferenceCounted {

    public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");
    private static final Logger logger = LoggerFactory.getLogger(Connection.class);
    private final URL url;
    private final int connectTimeout;
    private final WireProtocol protocol;
    private final Promise<Void> closeFuture;
    private final InetSocketAddress remote;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<Channel> channel = new AtomicReference<>();
    private final ChannelFuture initPromise;

    public Connection(URL url) {
        url = ExecutorUtil.setThreadName(url, "DubboClientHandler");
        url = url.addParameterIfAbsent(THREADPOOL_KEY, DEFAULT_CLIENT_THREADPOOL);
        this.url = url;
        this.protocol = ExtensionLoader.getExtensionLoader(WireProtocol.class).getExtension(url.getProtocol());
        this.connectTimeout = Math.max(3000, url.getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT));
        this.closeFuture = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        this.remote = getConnectAddress();
        this.initPromise = connect();
    }

    public static Connection getConnectionFromChannel(Channel channel) {
        return channel.attr(CONNECTION).get();
    }

    public Promise<Void> getCloseFuture() {
        return closeFuture;
    }


    public ChannelFuture connect() {
        if (isClosed()) {
            return null;
        }
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NettyEventLoopFactory.NIO_EVENT_LOOP_GROUP)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .remoteAddress(getConnectAddress())
                .channel(socketChannelClass());

        final ConnectionHandler connectionHandler = new ConnectionHandler(this);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) {
                ch.attr(CONNECTION).set(Connection.this);
                // TODO support SSL
                final ChannelPipeline p = ch.pipeline();//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                // TODO support IDLE
//                int heartbeatInterval = UrlUtils.getHeartbeat(getUrl());
//                p.addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS));
                p.addLast(connectionHandler);
                // TODO support ssl
                protocol.configClientPipeline(p, null);
                // TODO support Socks5
            }
        });
        final ChannelFuture promise = bootstrap.connect();
        promise.addListener(new ConnectionListener(this));
        return promise;
    }

    public Channel getChannel() {
        return channel.get();
    }

    @Override
    public String toString() {
        return "(Ref=" + ReferenceCountUtil.refCnt(this) + ",local=" + (getChannel() == null ? null : getChannel().localAddress()) + ",remote=" + getRemote();
    }

    public void onGoaway(Channel channel) {
        if (this.channel.compareAndSet(channel, null)) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Connection:%s  goaway", this));
            }
        }
    }

    public void onConnected(Channel channel) {
        this.channel.set(channel);
        channel.attr(CONNECTION).set(this);
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Connection:%s connected ", this));
        }
    }

    public void connectSync() {
        this.initPromise.awaitUninterruptibly(this.connectTimeout);
    }

    public boolean isAvailable() {
        final Channel channel = getChannel();
        return channel != null && channel.isActive();
    }

    public boolean isClosed() {
        return closed.get();
    }

    public ChannelFuture write(Object request) throws RemotingException {
        if (!isAvailable()) {
            throw new RemotingException(null, null, "Failed to send request " + request + ", cause: The channel to " + remote + " is closed!");
        }
        return getChannel().writeAndFlush(request);
    }

    public InetSocketAddress getRemote() {
        return remote;
    }

    @Override
    protected void deallocate() {
        if (closed.compareAndSet(false, true)) {
            close();
        }
        closeFuture.setSuccess(null);
    }

    public void close() {
        final Channel current = this.channel.get();
        if (current != null) {
            current.close();
        }
        this.channel.set(null);
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

    private InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    /**
     * get url.
     *
     * @return url
     */
    public URL getUrl() {
        return url;
    }

    private int getConnectTimeout() {
        return connectTimeout;
    }
}

