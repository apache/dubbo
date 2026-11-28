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
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_CLIENT_CONNECT_TIMEOUT;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CONNECT_PROVIDER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_RECONNECT;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_UNEXPECTED_EXCEPTION;

public abstract class AbstractNettyConnectionClient extends AbstractConnectionClient {

    private AtomicReference<Promise<Object>> connectingPromiseRef;

    private AtomicReference<io.netty.channel.Channel> channelRef;

    private Promise<Void> connectedPromise;

    private Promise<Void> disconnectedPromise;

    private Promise<Void> closePromise;

    private AtomicBoolean isReconnecting;

    private ConnectionListener connectionListener;

    private StateTracker states;
    private AtomicInteger failedAttempts;
    private AtomicBoolean reconnectScheduled;
    private AtomicReference<ChannelFuture> connectAttempt;

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
        states = new StateTracker();
        failedAttempts = new AtomicInteger();
        reconnectScheduled = new AtomicBoolean();
        connectAttempt = new AtomicReference<>();
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
            closePromise.trySuccess(null);
        }
    }

    protected void performClose() {
        if (states != null) states.update(State.SHUTDOWN);
        ChannelFuture attempt = connectAttempt == null ? null : connectAttempt.getAndSet(null);
        if (attempt != null) {
            attempt.cancel(false);
            attempt.channel().close();
        }
        Promise<Object> connecting = connectingPromiseRef == null ? null : connectingPromiseRef.get();
        if (connecting != null) connecting.tryFailure(new IllegalStateException("Connection closed"));
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
            isReconnecting.set(false);
            if (logger.isDebugEnabled()) {
                logger.debug("Connection:{} aborted to reconnect cause connection closed", this);
            }
            return;
        }

        states.update(State.CONNECTING);

        if (logger.isDebugEnabled()) {
            logger.debug("Connection:{} attempting to reconnect to server {}", this, getConnectAddress());
        }

        init.compareAndSet(false, true);
        long start = System.currentTimeMillis();

        Promise<Object> connectingPromise = getOrCreateConnectingPromise();
        ChannelFuture connectPromise = performConnect();
        connectAttempt.set(connectPromise);
        if (isClosed()) {
            connectPromise.cancel(false);
            connectPromise.channel().close();
            connectingPromise.tryFailure(new IllegalStateException("Connection closed"));
        }
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
            states.update(State.TRANSIENT_FAILURE);
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

    protected void scheduleReconnect(long reconnectDuration, TimeUnit unit) {
        boolean exponential = Boolean.TRUE.equals(getUrl().getAttribute(ConnectionStateProvider.BACKOFF_ATTRIBUTE));
        if (isClosed() || (exponential && !reconnectScheduled.compareAndSet(false, true))) return;
        long delayMillis = exponential
                ? reconnectDelayMillis(
                        failedAttempts.get(), ThreadLocalRandom.current().nextDouble())
                : unit.toMillis(reconnectDuration);
        connectivityExecutor.schedule(
                () -> {
                    if (exponential) reconnectScheduled.set(false);
                    try {
                        doConnect();
                    } catch (RemotingException e) {
                        logger.error(
                                TRANSPORT_FAILED_RECONNECT,
                                "",
                                "",
                                "Failed to connect to server: " + getConnectAddress());
                    }
                },
                delayMillis,
                TimeUnit.MILLISECONDS);
    }

    // gRPC-style bounded exponential delay with +/-20% jitter; explicit native opt-in only.
    static long reconnectDelayMillis(int failures, double jitter) {
        double base = Math.min(120_000, 1_000 * Math.pow(1.6, Math.min(32, Math.max(0, failures))));
        return (long) (base * (0.8 + 0.4 * jitter));
    }

    protected final void connectionReady() {
        if (!isConnected()) {
            states.update(isClosed() ? State.SHUTDOWN : State.TRANSIENT_FAILURE);
            return;
        }
        failedAttempts.set(0);
        states.update(State.READY);
    }

    protected boolean readyOnConnected() {
        return true;
    }

    protected final void connectionFailed() {
        failedAttempts.updateAndGet(value -> Math.min(32, value + 1));
        states.update(State.TRANSIENT_FAILURE);
    }

    @Override
    public State getConnectivityState() {
        return states.state;
    }

    @Override
    public AutoCloseable addConnectivityListener(Consumer<State> listener) {
        return states.subscribe(listener);
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
        if (readyOnConnected()) connectionReady();

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
            states.update(State.TRANSIENT_FAILURE);
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
    public void onDisconnected(Object channel) {
        if (channel instanceof io.netty.channel.Channel
                && channelRef.compareAndSet((io.netty.channel.Channel) channel, null)) {
            states.update(State.TRANSIENT_FAILURE);
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
            connectionFailed();
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

            scheduleReconnect(reconnectDuration, TimeUnit.MILLISECONDS);
        }
    }

    private final class StateTracker {
        private volatile State state = State.IDLE;
        private final Set<Consumer<State>> listeners = new LinkedHashSet<>();

        synchronized AutoCloseable subscribe(Consumer<State> listener) {
            Objects.requireNonNull(listener, "listener");
            Consumer<State> registration = listener::accept;
            if (state != State.SHUTDOWN) listeners.add(registration);
            notifyListener(registration);
            return () -> {
                synchronized (StateTracker.this) {
                    listeners.remove(registration);
                }
            };
        }

        synchronized void update(State next) {
            if (state == State.SHUTDOWN || state == next) return;
            state = next;
            for (Consumer<State> listener : new ArrayList<>(listeners)) {
                if (listeners.contains(listener)) notifyListener(listener);
            }
            if (next == State.SHUTDOWN) listeners.clear();
        }

        private void notifyListener(Consumer<State> listener) {
            try {
                listener.accept(state);
            } catch (RuntimeException error) {
                logger.warn(TRANSPORT_UNEXPECTED_EXCEPTION, "", "", "Connection state listener failed", error);
            }
        }
    }
}
