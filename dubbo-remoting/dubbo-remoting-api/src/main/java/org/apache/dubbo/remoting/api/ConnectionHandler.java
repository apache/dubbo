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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.Timer;

import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class ConnectionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

    private static final int MIN_FAST_RECONNECT_INTERVAL = 4000;
    private static final int BACKOFF_CAP = 15;
    private static final AttributeKey<Boolean> GO_AWAY_KEY = AttributeKey.valueOf("dubbo_channel_goaway");
    private final Timer timer;
    private final Bootstrap bootstrap;
    private volatile long lastReconnect;

    public ConnectionHandler(Bootstrap bootstrap, Timer timer) {
        this.bootstrap = bootstrap;
        this.timer = timer;
    }

    public void onGoAway(Channel channel) {
        channel.attr(GO_AWAY_KEY).set(true);
        final Connection connection = Connection.getConnectionFromChannel(channel);
        if (connection != null) {
            connection.onGoaway(channel);
        }
        tryReconnect(connection);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
        final Connection connection = Connection.getConnectionFromChannel(ctx.channel());
        if (connection != null) {
            connection.onConnected(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn(String.format("Channel error:%s", ctx.channel()), cause);
        ctx.close();
    }

    public boolean shouldFastReconnect() {
        final long period = System.currentTimeMillis() - lastReconnect;
        return period > MIN_FAST_RECONNECT_INTERVAL;
    }


    private boolean isGoAway(Channel channel) {
        return Boolean.TRUE.equals(channel.attr(GO_AWAY_KEY).get());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Reconnect event will be triggered by Connection.init();
        if (isGoAway(ctx.channel())) {
            ctx.fireChannelInactive();
            return;
        }
        Connection connection = Connection.getConnectionFromChannel(ctx.channel());
        tryReconnect(connection);
        ctx.fireChannelInactive();
    }

    private void tryReconnect(Connection connection) {
        if (connection != null && !connection.isClosed()) {
            if (shouldFastReconnect()) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Connection %s inactive, schedule fast reconnect", connection));
                }
                reconnect(connection, 1);
            } else {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Connection %s inactive, schedule normal reconnect", connection));
                }
                reconnect(connection, BACKOFF_CAP);
            }
        }
    }

    private void reconnect(final Connection connection, final int attempts) {
        this.lastReconnect = System.currentTimeMillis();

        int timeout = 2 << attempts;
        if (bootstrap.config().group().isShuttingDown()) {
            return;
        }

        int nextAttempt = Math.min(BACKOFF_CAP, attempts + 1);
        timer.newTimeout(timeout1 -> tryReconnect(connection, nextAttempt), timeout, TimeUnit.MILLISECONDS);
    }


    private void tryReconnect(final Connection connection, final int nextAttempt) {

        if (connection.isClosed() || bootstrap.config().group().isShuttingDown()) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info(String.format("Connection %s is reconnecting, attempt=%d", connection, nextAttempt));
        }

        bootstrap.connect(connection.getRemote()).addListener((ChannelFutureListener) future -> {
            if (connection.isClosed() || bootstrap.config().group().isShuttingDown()) {
                if (future.isSuccess()) {
                    Channel ch = future.channel();
                    Connection con = Connection.getConnectionFromChannel(ch);
                    if (con != null) {
                        con.close();
                    }
                }
                return;
            }

            if (future.isSuccess()) {
                final Channel channel = future.channel();
                if (!connection.isClosed()) {
                    connection.onConnected(channel);
                } else {
                    channel.close();
                }
            } else {
                reconnect(connection, nextAttempt);
            }
        });
    }

}