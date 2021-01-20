package org.apache.dubbo.remoting.api;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Timer;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class ConnectionHandler extends ChannelInboundHandlerAdapter {

    private static final int MIN_FAST_RECONNECT_INTERVAL = 4000;
    private static final int BACKOFF_CAP = 12;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Timer timer;
    private final Bootstrap bootstrap;
    private final Semaphore permit = new Semaphore(1);
    private volatile long lastReconnect;

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
        return period > MIN_FAST_RECONNECT_INTERVAL;
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            final Connection connection = Connection.getConnectionFromChannel(ctx.channel());
            if (connection != null) {
                connection.onIdle();
                ctx.close();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Connection connection = Connection.getConnectionFromChannel(ctx.channel());
        if (connection != null) {
            if (!connection.isClosed() && connection.onDisConnected()) {
                if (shouldFastReconnect()) {
                    if(log.isDebugEnabled()) {
                        log.debug(String.format( "Connection %s inactive, schedule fast reconnect",connection ));
                    }
                    reconnect(connection, 1);
                } else {
                    if(log.isDebugEnabled()) {
                        log.debug(String.format( "Connection %s inactive, schedule normal reconnect",connection ));
                    }
                    reconnect(connection, BACKOFF_CAP);
                }
            }
        }
        ctx.fireChannelInactive();
    }

    private void reconnect(final Connection connection, final int attempts) {
        this.lastReconnect = System.currentTimeMillis();

        int timeout = 2 << attempts;
        if (bootstrap.config().group().isShuttingDown()) {
            return;
        }

        if (permit.tryAcquire()) {
            timer.newTimeout(timeout1 -> tryReconnect(connection, Math.min(BACKOFF_CAP, attempts + 1)), timeout, TimeUnit.MILLISECONDS);
        }
    }


    private void tryReconnect(final Connection connection, final int nextAttempt) {
        permit.release();

        if (connection.isClosed() || bootstrap.config().group().isShuttingDown()) {
            return;
        }
        if(log.isDebugEnabled()) {
            log.debug(String.format( "Connection %s is reconnecting, attempt=%d",connection,nextAttempt ));
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