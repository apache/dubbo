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
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.utils.UrlUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.AttributeKey;
import io.netty.util.HashedWheelTimer;
import io.netty.util.ReferenceCounted;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CLIENT_THREADPOOL;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.remoting.api.NettyEventLoopFactory.socketChannelClass;

public class Connection extends AbstractReferenceCounted implements ReferenceCounted {

    public static final Timer TIMER = new HashedWheelTimer(
            new NamedThreadFactory("dubbo-network-timer", true), 30, TimeUnit.MILLISECONDS);
    public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");
    private static final Logger logger = LoggerFactory.getLogger(Connection.class);
    private final URL url;
    private final Bootstrap bootstrap;
    private final int connectTimeout;
    private final WireProtocol protocol;
    private final Promise<Void> closeFuture;
    private final InetSocketAddress remote;
    private final AtomicReference<ConnectionStatus> status;
    private volatile Channel channel;
    private volatile Future<Channel> connectFuture;
    private int retryAttempts = 1;
    private long lastReconnectTime;

    public Connection(URL url) {
        url = ExecutorUtil.setThreadName(url, "DubboClientHandler");
        url = url.addParameterIfAbsent(THREADPOOL_KEY, DEFAULT_CLIENT_THREADPOOL);
        this.url = url;
        this.status = new AtomicReference<>(ConnectionStatus.DISCONNECTED);
        this.protocol = ExtensionLoader.getExtensionLoader(WireProtocol.class).getExtension(url.getProtocol());
        this.connectTimeout = url.getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
        this.closeFuture = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        this.remote = getConnectAddress();
        this.bootstrap = open();
    }

    public static Connection getConnectionFromChannel(Channel channel) {
        return channel.attr(CONNECTION).get();
    }

    public Promise<Void> getCloseFuture() {
        return closeFuture;
    }


    public void init() throws RemotingException {
        concurrentConnect();
    }

    public Bootstrap open() {
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NettyEventLoopFactory.NIO_EVENT_LOOP_GROUP)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(socketChannelClass());

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(3000, connectTimeout));
        final ConnectionHandler connectionHandler = new ConnectionHandler(bootstrap, TIMER);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) {
                ch.attr(CONNECTION).set(Connection.this);
                Connection.this.channel = ch;
                int heartbeatInterval = UrlUtils.getHeartbeat(getUrl());
                // TODO support SSL
                final ChannelPipeline p = ch.pipeline();//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                p.addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS));
                p.addLast(connectionHandler);
                // TODO support ssl
                protocol.configClientPipeline(p, null);
                // TODO support Socks5
            }
        });
        return bootstrap;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return getRemote() + ",channel=" + channel + ", status=" + status;
    }

    public void onIdle() {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Connection:%s disconnected cause idle", this));
        }
        setStatus(ConnectionStatus.DISCONNECTED);
        this.channel = null;
    }

    public boolean onDisConnected() {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Connection:%s disconnected ", this));
        }
        return setStatus(ConnectionStatus.DISCONNECTED);
    }

    public void onConnected(Channel channel) {
        setStatus(ConnectionStatus.CONNECTED);
        this.channel = channel;
        channel.attr(CONNECTION).set(this);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Connection:%s connected ", this));
        }
    }

    public boolean isAvailable() {
        return ConnectionStatus.CONNECTED == getStatus() && channel != null && channel.isActive();
    }

    public ConnectionStatus getStatus() {
        return status.get();
    }

    public boolean setStatus(ConnectionStatus status) {
        return this.status.getAndSet(status) != status;
    }

    public boolean isClosed() {
        return getStatus() == ConnectionStatus.CLOSED;
    }

    public void close() {
        setStatus(ConnectionStatus.CLOSED);
        if (channel != null) {
            channel.close();
        }
    }

    public ChannelFuture write(Object request) throws RemotingException {
        if (channel == null || !channel.isActive()) {
            throw new RemotingException(null, null, "Failed to send request " + request + ", cause: The channel to " + remote + " is closed!");
        }

        return getChannel().writeAndFlush(request);
    }

    public InetSocketAddress getRemote() {
        return remote;
    }

    @Override
    protected void deallocate() {
        setStatus(ConnectionStatus.CLOSED);
        if (channel != null) {
            channel.close();
        }
        closeFuture.setSuccess(null);
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

    private InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    protected Future<Channel> connectAsync() {
        final Promise<Channel> promise = ImmediateEventExecutor.INSTANCE.newPromise();
        ChannelFuture future = bootstrap.connect(getRemote());
        final ChannelFutureListener listener = new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (bootstrap.config().group().isShuttingDown()) {
                    promise.tryFailure(new IllegalStateException("Client is shutdown"));
                    return;
                }
                if (future.isSuccess()) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Succeed connect to server " + future.channel().remoteAddress() + " from " + getClass().getSimpleName() + " "
                                + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                                + ", channel is " + future.channel());
                    }
                    bootstrap.config().group().execute(() -> promise.setSuccess(future.channel()));
                } else {
                    bootstrap.config().group().execute(() -> {
                        final RemotingException cause = new RemotingException(null, getRemote(), "client(url: " + getUrl() + ") failed to connect to server "
                                + future.channel().remoteAddress() + ", error message is:" + future.cause().getMessage(), future.cause());
                        promise.tryFailure(cause);
                    });
                }
                Connection.this.connectFuture = null;
            }
        };
        future.addListener(listener);
        return promise;
    }


    private void concurrentConnect() throws RemotingException {
        if (channel != null && channel.isActive()) {
            return;
        }
        synchronized (this) {
            if (channel != null && channel.isActive()) {
                return;
            }
            if (connectFuture != null) {
                connectFuture.awaitUninterruptibly(getConnectTimeout());
            } else {
                int backoff = 2 << retryAttempts;
                if (System.currentTimeMillis() - lastReconnectTime < backoff) {
                    return;
                }
                connectWithGuard();
            }
        }
    }

    protected synchronized void connectWithGuard() throws RemotingException {
        this.lastReconnectTime = System.currentTimeMillis();
        this.retryAttempts = Math.min(12, retryAttempts + 1);
        long start = System.currentTimeMillis();
        final Future<Channel> connectFuture = connectAsync();
        this.connectFuture = connectFuture;
        connectFuture.awaitUninterruptibly(getConnectTimeout());
        if (!connectFuture.isSuccess()) {
            if (connectFuture.isDone()) {
                throw new RemotingException(null, getRemote(), "client(url: " + getUrl() + ") failed to connect to server .error message is:" + connectFuture.cause().getMessage(),
                        connectFuture.cause());
            } else {
                throw new RemotingException(null, getRemote(), "client(url: " + getUrl() + ") failed to connect to server. client-side timeout "
                        + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client "
                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion());
            }
        } else {
            this.retryAttempts = 0;
        }
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

    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        READ_ONLY,
        CLOSED
    }

}

