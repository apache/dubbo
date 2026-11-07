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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.api.connection.ConnectionHandler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_RECONNECT;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_UNEXPECTED_EXCEPTION;

/**
 * Connection lifecycle handler for Netty-based Dubbo connections.
 *
 * <p>Implements graceful HTTP/2 GOAWAY migration: when a GOAWAY frame is received
 * (e.g., from Envoy/Higress max_requests_per_connection), the old channel is kept
 * alive for in-flight requests while a new connection is established in the background.
 * Once the new connection is ready, {@link AbstractNettyConnectionClient#onConnected} atomically
 * swaps the channel reference and closes the old one - eliminating any unavailability window.
 *
 * <p>Fallback: if the new connection fails (e.g., mTLS handshake timeout), the handler
 * calls {@link AbstractNettyConnectionClient#onGoaway} to null the channel and let the
 * connectivity-scheduler handle recovery (original behavior).
 */
@Sharable
public class NettyConnectionHandler extends ChannelInboundHandlerAdapter implements ConnectionHandler {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(NettyConnectionHandler.class);

    private static final AttributeKey<Boolean> GO_AWAY_KEY = AttributeKey.valueOf("dubbo_channel_goaway");

    /**
     * Delay before initiating new connection after GOAWAY (milliseconds).
     * Gives the gateway (Envoy/Higress) time to complete drain setup
     * while keeping the migration window short.
     */
    private static final long GRACEFUL_RECONNECT_DELAY_MS = 200;

    private final AbstractNettyConnectionClient connectionClient;

    public NettyConnectionHandler(AbstractNettyConnectionClient connectionClient) {
        this.connectionClient = connectionClient;
    }

    @Override
    public void onGoAway(Object channel) {
        if (!(channel instanceof Channel)) {
            return;
        }
        Channel nettyChannel = ((Channel) channel);
        Attribute<Boolean> attr = nettyChannel.attr(GO_AWAY_KEY);
        if (Boolean.TRUE.equals(attr.get())) {
            return;
        }
        attr.set(true);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format(
                    "Received GOAWAY on %s -> %s, initiating graceful migration (delay=%dms)",
                    nettyChannel.localAddress(), nettyChannel.remoteAddress(), GRACEFUL_RECONNECT_DELAY_MS));
        }

        if (connectionClient.isClosed()) {
            LOGGER.info("The client has been closed and will not reconnect.");
            return;
        }

        // Use connectivityExecutor (a dedicated scheduler) instead of the Netty I/O
        // EventLoop. doConnect() internally awaits the connect promise via
        // awaitUninterruptibly(getConnectTimeout()), which would block the I/O thread
        // and stall in-flight requests on this very channel.
        final ScheduledExecutorService scheduler = connectionClient.getConnectivityExecutor();
        scheduler.schedule(
                () -> attemptGracefulMigration(nettyChannel, scheduler, true),
                GRACEFUL_RECONNECT_DELAY_MS,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Try to establish a new connection on behalf of {@link #onGoAway}.
     *
     * <p>On success, {@link AbstractNettyConnectionClient#onConnected} swaps the channel
     * reference atomically (closeFuture CAS in initBootstrap protects against
     * the old channel's close handler wiping the new reference).
     *
     * <p>On failure, the first attempt schedules one extra retry through the
     * same connectivity executor. The second failure falls back to
     * {@link AbstractNettyConnectionClient#onGoaway}, which nulls the channel and lets
     * the connectivity scheduler take over recovery.
     */
    private void attemptGracefulMigration(
            final Channel nettyChannel, final ScheduledExecutorService scheduler, final boolean canRetry) {
        if (connectionClient.isClosed()) {
            return;
        }
        try {
            connectionClient.doConnect();
            // Success: onConnected() will close old channel and set new one.
            // The closeFuture CAS in initBootstrap() ensures no race condition.
        } catch (Throwable e) {
            if (canRetry) {
                long retryDelay = Math.max(connectionClient.getReconnectDuration(), GRACEFUL_RECONNECT_DELAY_MS);
                LOGGER.warn(
                        TRANSPORT_FAILED_RECONNECT,
                        "",
                        "",
                        String.format(
                                "Graceful migration attempt failed for %s, scheduling one retry in %dms",
                                nettyChannel.remoteAddress(), retryDelay),
                        e);
                scheduler.schedule(
                        () -> attemptGracefulMigration(nettyChannel, scheduler, false),
                        retryDelay,
                        TimeUnit.MILLISECONDS);
                return;
            }
            // Graceful migration failed twice (e.g., persistent mTLS timeout).
            // Fallback: null the channel to let connectivity-scheduler take over.
            LOGGER.error(
                    TRANSPORT_FAILED_RECONNECT,
                    "",
                    "",
                    String.format(
                            "Graceful migration failed for %s, falling back to scheduler recovery",
                            nettyChannel.remoteAddress()),
                    e);
            connectionClient.onGoaway(nettyChannel);
        }
    }

    /**
     * Reconnect with longer delay - used for unexpected disconnects (non-GOAWAY).
     */
    @Override
    public void reconnect(Object channel) {
        if (!(channel instanceof Channel)) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Connection:{} is reconnecting, attempt={}", connectionClient, 1);
        }
        if (connectionClient.isClosed()) {
            LOGGER.info("The client has been closed and will not reconnect.");
            return;
        }
        connectionClient.scheduleReconnect(1, TimeUnit.SECONDS);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();
        Channel ch = ctx.channel();
        NettyChannel channel = NettyChannel.getOrAddChannel(ch, connectionClient.getUrl(), connectionClient);
        if (!connectionClient.isClosed()) {
            connectionClient.onConnected(ch);

            if (LOGGER.isInfoEnabled() && channel != null) {
                LOGGER.info(
                        "The connection {} of {} -> {} is established.",
                        ch,
                        channel.getLocalAddressKey(),
                        channel.getRemoteAddressKey());
            }
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel ch = ctx.channel();
        // Look up the cached NettyChannel without allocating a new one.
        // The channel is already inactive at this point, so getOrAddChannel would
        // construct a transient unregistered NettyChannel only to log it.
        NettyChannel channel = NettyChannel.getChannelIfPresent(ch);
        try {
            Attribute<Boolean> goawayAttr = ch.attr(GO_AWAY_KEY);
            if (!Boolean.TRUE.equals(goawayAttr.get())) {
                // Only reconnect for unexpected disconnects (not GOAWAY).
                // GOAWAY disconnects are already handled by the graceful migration above.
                reconnect(ch);
            }
            if (LOGGER.isInfoEnabled() && channel != null) {
                LOGGER.info(
                        "The connection {} of {} -> {} is disconnected.",
                        ch,
                        channel.getLocalAddressKey(),
                        channel.getRemoteAddressKey());
            }
        } finally {
            NettyChannel.removeChannel(ch);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warn(TRANSPORT_UNEXPECTED_EXCEPTION, "", "", String.format("Channel error:%s", ctx.channel()), cause);
        ctx.close();
    }
}
