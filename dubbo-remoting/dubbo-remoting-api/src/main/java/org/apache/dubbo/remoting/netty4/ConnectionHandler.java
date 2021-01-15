package org.apache.dubbo.remoting.netty4;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Timer;

import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class ConnectionHandler extends ChannelInboundHandlerAdapter {

    private static final int BACKOFF_CAP = 12;
    private static final int MIN_BACKOFF_GAP = 4000;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Timer timer;
    private final Bootstrap bootstrap;
    private long lastReconnect;

    public ConnectionHandler(Bootstrap bootstrap, Timer timer) {
        this.bootstrap = bootstrap;
        this.timer = timer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
        final Connection connection = Connection.getConnectionFromChannel(ctx.channel());
        if (connection != null) {
            connection.onConnected(ctx.channel());
        }
    }

    public boolean shouldFastReconnect() {
        final long period = System.currentTimeMillis() - lastReconnect;
        return period > MIN_BACKOFF_GAP;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Connection connection = Connection.getConnectionFromChannel(ctx.channel());
        if (connection != null) {
            if (!connection.isClosed() && connection.setStatus(Connection.ConnectionStatus.DISCONNECTED)) {
                if (shouldFastReconnect()) {
                    reconnect(connection, 1);
                } else {
                    reconnect(connection, BACKOFF_CAP);
                }
            }
        }
        ctx.fireChannelInactive();
    }

    private void reconnect(final Connection connection, final int attempts) {
        lastReconnect = System.currentTimeMillis();
        int timeout = 2 << attempts;
        if (bootstrap.config().group().isShuttingDown()) {
            return;
        }

        timer.newTimeout(timeout1 -> tryReconnect(connection, Math.min(BACKOFF_CAP, attempts + 1)), timeout, TimeUnit.MILLISECONDS);
    }

    private void tryReconnect(final Connection connection, final int nextAttempt) {
        if (connection.isClosed() || bootstrap.config().group().isShuttingDown()) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("reconnecting connection=%s to %s ", connection, connection.getRemote()));
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
                connection.onConnected(channel);
                if (!connection.isClosed()) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("%s connected to %s", connection, connection.getRemote()));
                    }
                } else {
                    channel.close();
                }
            }
            reconnect(connection, nextAttempt);
        });
    }

}