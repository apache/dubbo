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

    private AtomicReference<Promise<Object>> connectingPromiseRef;

    private AtomicReference<io.netty.channel.Channel> channelRef;

    private Promise<Void> connectedPromise;

    private Promise<Void> disconnectedPromise;

    private Promise<Void> closePromise;

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
        remote = getConnectAddress();
        init = new AtomicBoolean(false);
        connectingPromiseRef = new AtomicReference<>();
        channelRef = new AtomicReference<>();
        connectedPromise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        disconnectedPromise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        closePromise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        isReconnecting = new AtomicBoolean(false);
        connectionListener = new ConnectionListener();
        increase();
    }

    protected abstract void initBootstrap() throws Exception;

    @Override
    protected void doClose() {
        // AbstractPeer close can set closed true.
        if (isClosed()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Connection:{} freed", this);
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
            if (logger.isDebugEnabled()) {
                logger.debug("Connection:{} aborted to reconnect cause connection closed", this);
            }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Connection:{} attempting to reconnect to server {}", this, getConnectAddress());
        }

        init.compareAndSet(false, true);
        long start = System.currentTimeMillis();

        Promise<Object> connectingPromise = getOrCreateConnectingPromise();
        Future<Void> connectPromise = performConnect();
        connectPromise.addListener(connectionListener);

        boolean ret = connectingPromise.awaitUninterruptibly(getConnectTimeout(), TimeUnit.MILLISECONDS);
        // destroy connectingPromise after used
        synchronized (this) {
            connectingPromiseRef.set(null);
        }

        if (connectPromise.cause() != null) {
            Throwable cause = connectPromise.cause();

            // 6-1 Failed to connect to provider server by other reason.
            RemotingException remotingException = new RemotingException(
                    this,
                    "client(url: " + getUrl() + ") failed to connect to server " + getConnectAddress()
                            + ", error message is:" + cause.getMessage(),
                    cause);

            logger.error(
                    TRANSPORT_FAILED_CONNECT_PROVIDER,
                    "network disconnected",
                    "",
                    "Failed to connect to provider server by other reason",
                    cause);

            throw remotingException;
        } else if (!ret || !connectPromise.isSuccess()) {
            // 6-2 Client-side timeout
            RemotingException remotingException = new RemotingException(
                    this,
                    "client(url: " + getUrl() + ") failed to connect to server " + getConnectAddress()
                            + " client-side timeout " + getConnectTimeout() + "ms (elapsed: "
                            + (System.currentTimeMillis() - start) + "ms) from netty client " + NetUtils.getLocalHost()
                            + " using dubbo version "
                            + Version.getVersion());

            logger.error(
                    TRANSPORT_CLIENT_CONNECT_TIMEOUT, "provider crash", "", "Client-side timeout", remotingException);

            throw remotingException;
        }
    }

    protected abstract ChannelFuture performConnect();

    @Override
    protected void doDisConnect() {
        NettyChannel.removeChannelIfDisconnected(getNettyChannel());
    }

    protected void doReconnect() {
        connectivityExecutor.execute(() -> {
            try {
                doConnect();
            } catch (RemotingException e) {
                logger.error(
                        TRANSPORT_FAILED_RECONNECT, "", "", "Failed to reconnect to server: " + getConnectAddress());
            }
        });
    }

    @Override
    public void onConnected(Object channel) {
        if (!(channel instanceof io.netty.channel.Channel)) {
            return;
        }

        io.netty.channel.Channel nettyChannel = ((io.netty.channel.Channel) channel);
        if (isClosed()) {
            nettyChannel.close();
            if (logger.isDebugEnabled()) {
                logger.debug("Connection:{} is closed, ignoring connected event", this);
            }
            return;
        }

        // Close the existing channel before setting a new channel
        io.netty.channel.Channel current = getNettyChannel();
        if (current != null) {
            current.close();
        }

        channelRef.set(nettyChannel);

        // This indicates that the connection is available.
        Promise<Object> connectingPromise = connectingPromiseRef.get();
        if (connectingPromise != null) {
            connectingPromise.trySuccess(CONNECTED_OBJECT);
        }

        nettyChannel.attr(CONNECTION).set(this);

        // Notify the connection is available.
        connectedPromise.trySuccess(null);

        if (logger.isDebugEnabled()) {
            logger.debug("Connection:{} connected", this);
        }
    }

    @Override
    public void onGoaway(Object channel) {
        if (!(channel instanceof io.netty.channel.Channel)) {
            return;
        }

        io.netty.channel.Channel nettyChannel = (io.netty.channel.Channel) channel;
        if (channelRef.compareAndSet(nettyChannel, null)) {
            // Ensure the channel is closed
            if (nettyChannel.isOpen()) {
                nettyChannel.close();
            }
            NettyChannel.removeChannelIfDisconnected(nettyChannel);
            if (logger.isDebugEnabled()) {
                logger.debug("Connection:{} goaway", this);
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
        return channelRef.get();
    }

    protected void clearNettyChannel() {
        channelRef.set(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getChannel(Boolean generalizable) {
        return Boolean.TRUE.equals(generalizable) ? (T) getNettyChannel() : (T) getChannel();
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
                logger.error(TRANSPORT_FAILED_RECONNECT, "", "", "Failed to connect to server: " + getConnectAddress());
            }
        }

        getOrCreateConnectingPromise().awaitUninterruptibly(getConnectTimeout(), TimeUnit.MILLISECONDS);
        // destroy connectingPromise after used
        synchronized (this) {
            connectingPromiseRef.set(null);
        }

        nettyChannel = getNettyChannel();
        return nettyChannel != null && nettyChannel.isActive();
    }

    private Promise<Object> getOrCreateConnectingPromise() {
        connectingPromiseRef.compareAndSet(null, new DefaultPromise<>(GlobalEventExecutor.INSTANCE));
        return connectingPromiseRef.get();
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
        return getNettyChannel().writeAndFlush(request);
    }

    @Override
    public void addConnectedListener(Runnable func) {
        connectedPromise.addListener(future -> func.run());
    }

    @Override
    public void addDisconnectedListener(Runnable func) {
        disconnectedPromise.addListener(future -> func.run());
    }

    @Override
    public void addCloseListener(Runnable func) {
        closePromise.addListener(future -> func.run());
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
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Connection:{} aborted to reconnect. {}",
                            connectionClient,
                            future.cause().getMessage());
                }
                return;
            }
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Connection:{} is reconnecting, attempt=0 cause={}",
                        connectionClient,
                        future.cause().getMessage());
            }

            // Notify the connection is unavailable.
            disconnectedPromise.trySuccess(null);

            doReconnect();
        }
    }
}
