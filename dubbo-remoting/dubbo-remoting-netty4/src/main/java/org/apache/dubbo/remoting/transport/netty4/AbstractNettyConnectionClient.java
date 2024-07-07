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
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_CLIENT_CONNECT_TIMEOUT;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CONNECT_PROVIDER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_RECONNECT;

public abstract class AbstractNettyConnectionClient extends AbstractConnectionClient {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(AbstractNettyConnectionClient.class);

    private AtomicReference<Promise<Object>> connectingPromise;

    private Promise<Void> closePromise;

    private AtomicReference<io.netty.channel.Channel> channel;

    private AtomicBoolean isReconnecting;

    private ConnectionListener connectionListener;

    public static final AttributeKey<AbstractConnectionClient> CONNECTION = AttributeKey.valueOf("connection");

    public AbstractNettyConnectionClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected void doOpen() throws Throwable {
        initConnectionClient();
        initBootstrap();
    }

    @Override
    protected void initConnectionClient() {
        this.remote = getConnectAddress();
        this.connectingPromise = new AtomicReference<>();
        this.connectionListener = new ConnectionListener();
        this.channel = new AtomicReference<>();
        this.isReconnecting = new AtomicBoolean(false);
        this.closePromise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        this.init = new AtomicBoolean(false);
        this.increase();
    }

    protected abstract void initBootstrap() throws Exception;

    @Override
    protected void doClose() {
        // AbstractPeer close can set closed true.
        if (isClosed()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Connection:%s freed ", this));
            }
            performClose();
            closePromise.setSuccess(null);
        }
    }

    protected void performClose() {
        io.netty.channel.Channel current = getNettyChannel();
        if (current != null) {
            current.close();
        }
        clearNettyChannel();
    }

    @Override
    protected void doConnect() throws RemotingException {
        if (!isReconnecting.compareAndSet(false, true)) {
            return;
        }

        if (isClosed()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("%s aborted to reconnect cause connection closed. ", this));
            }
        }
        init.compareAndSet(false, true);
        long start = System.currentTimeMillis();

        createConnectingPromise();
        Future<Void> promise = performConnect();

        promise.addListener(connectionListener);

        boolean ret = connectingPromise.get().awaitUninterruptibly(getConnectTimeout(), TimeUnit.MILLISECONDS);
        // destroy connectingPromise after used
        synchronized (this) {
            connectingPromise.set(null);
        }
        if (promise.cause() != null) {
            Throwable cause = promise.cause();

            // 6-1 Failed to connect to provider server by other reason.
            RemotingException remotingException = new RemotingException(
                    this,
                    "client(url: " + getUrl() + ") failed to connect to server " + getConnectAddress()
                            + ", error message is:" + cause.getMessage(),
                    cause);

            LOGGER.error(
                    TRANSPORT_FAILED_CONNECT_PROVIDER,
                    "network disconnected",
                    "",
                    "Failed to connect to provider server by other reason.",
                    cause);

            throw remotingException;
        } else if (!ret || !promise.isSuccess()) {
            // 6-2 Client-side timeout
            RemotingException remotingException = new RemotingException(
                    this,
                    "client(url: " + getUrl() + ") failed to connect to server " + getConnectAddress()
                            + " client-side timeout " + getConnectTimeout() + "ms (elapsed: "
                            + (System.currentTimeMillis() - start) + "ms) from netty client " + NetUtils.getLocalHost()
                            + " using dubbo version "
                            + Version.getVersion());

            LOGGER.error(
                    TRANSPORT_CLIENT_CONNECT_TIMEOUT, "provider crash", "", "Client-side timeout.", remotingException);

            throw remotingException;
        }
    }

    protected abstract ChannelFuture performConnect();

    @Override
    protected void doDisConnect() {
        NettyChannel.removeChannelIfDisconnected(getNettyChannel());
    }

    @Override
    public void onConnected(Object channel) {
        if (!(channel instanceof io.netty.channel.Channel)) {
            return;
        }
        io.netty.channel.Channel nettyChannel = ((io.netty.channel.Channel) channel);
        if (isClosed()) {
            nettyChannel.close();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("%s is closed, ignoring connected event", this));
            }
            return;
        }

        // Close the existing channel before setting a new channel
        io.netty.channel.Channel current = getNettyChannel();
        if (current != null) {
            current.close();
        }

        this.channel.set(nettyChannel);
        // This indicates that the connection is available.
        if (connectingPromise.get() != null) {
            connectingPromise.get().trySuccess(CONNECTED_OBJECT);
        }
        nettyChannel.attr(CONNECTION).set(this);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("%s connected ", this));
        }
    }

    @Override
    public void onGoaway(Object channel) {
        if (!(channel instanceof io.netty.channel.Channel)) {
            return;
        }
        io.netty.channel.Channel nettyChannel = (io.netty.channel.Channel) channel;
        if (this.channel.compareAndSet(nettyChannel, null)) {
            // Ensure the channel is closed
            if (nettyChannel.isOpen()) {
                nettyChannel.close();
            }
            NettyChannel.removeChannelIfDisconnected(nettyChannel);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("%s goaway", this));
            }
        }
    }

    @Override
    protected Channel getChannel() {
        io.netty.channel.Channel c = getNettyChannel();
        if (c == null) {
            return null;
        }
        return NettyChannel.getOrAddChannel(c, getUrl(), this);
    }

    private io.netty.channel.Channel getNettyChannel() {
        return channel.get();
    }

    protected void clearNettyChannel() {
        channel.set(null);
    }

    @Override
    public Object getChannel(Boolean generalizable) {
        return Boolean.TRUE.equals(generalizable) ? getNettyChannel() : getChannel();
    }

    @Override
    public boolean isAvailable() {
        if (isClosed()) {
            return false;
        }
        io.netty.channel.Channel nettyChannel = getNettyChannel();
        if (nettyChannel != null && nettyChannel.isActive()) {
            return true;
        }

        if (init.compareAndSet(false, true)) {
            try {
                doConnect();
            } catch (RemotingException e) {
                LOGGER.error(TRANSPORT_FAILED_RECONNECT, "", "", "Failed to connect to server: " + getConnectAddress());
            }
        }

        createConnectingPromise();
        connectingPromise.get().awaitUninterruptibly(getConnectTimeout(), TimeUnit.MILLISECONDS);
        // destroy connectingPromise after used
        synchronized (this) {
            connectingPromise.set(null);
        }

        nettyChannel = getNettyChannel();
        return nettyChannel != null && nettyChannel.isActive();
    }

    @Override
    public void createConnectingPromise() {
        connectingPromise.compareAndSet(null, new DefaultPromise<>(GlobalEventExecutor.INSTANCE));
    }

    public Promise<Void> getClosePromise() {
        return closePromise;
    }

    public static AbstractConnectionClient getConnectionClientFromChannel(io.netty.channel.Channel channel) {
        return channel.attr(CONNECTION).get();
    }

    public ChannelFuture write(Object request) throws RemotingException {
        if (!isAvailable()) {
            throw new RemotingException(
                    null,
                    null,
                    "Failed to send request " + request + ", cause: The channel to " + remote + " is closed!");
        }
        return ((io.netty.channel.Channel) getChannel()).writeAndFlush(request);
    }

    @Override
    public void addCloseListener(Runnable func) {
        getClosePromise().addListener(future -> func.run());
    }

    @Override
    public void destroy() {
        close();
    }

    @Override
    public String toString() {
        return super.toString() + " (Ref=" + getCounter() + ",local="
                + Optional.ofNullable(getChannel())
                        .map(Channel::getLocalAddress)
                        .orElse(null) + ",remote=" + getRemoteAddress();
    }

    class ConnectionListener implements ChannelFutureListener {

        @Override
        public void operationComplete(ChannelFuture future) {
            if (!isReconnecting.compareAndSet(true, false)) {
                return;
            }
            if (future.isSuccess()) {
                return;
            }
            AbstractNettyConnectionClient connectionClient = AbstractNettyConnectionClient.this;
            if (connectionClient.isClosed() || connectionClient.getCounter() == 0) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format(
                            "%s aborted to reconnect. %s",
                            connectionClient, future.cause().getMessage()));
                }
                return;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format(
                        "%s is reconnecting, attempt=%d cause=%s",
                        connectionClient, 0, future.cause().getMessage()));
            }
            connectivityExecutor.schedule(
                    () -> {
                        try {
                            connectionClient.doConnect();
                        } catch (RemotingException e) {
                            LOGGER.error(
                                    TRANSPORT_FAILED_RECONNECT,
                                    "",
                                    "",
                                    "Failed to connect to server: " + getConnectAddress());
                        }
                    },
                    reconnectDuration,
                    TimeUnit.MILLISECONDS);
        }
    }
}
