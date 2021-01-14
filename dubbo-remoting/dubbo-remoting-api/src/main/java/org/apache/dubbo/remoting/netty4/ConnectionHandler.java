package org.apache.dubbo.remoting.netty4;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelStatus;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timer;

import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class ConnectionHandler extends ChannelInboundHandlerAdapter {

    private static final int BACKOFF_CAP = 12;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Timer timer;
    private final Bootstrap bootstrap;
    private final ChannelGroup channels;

    public ConnectionHandler(Bootstrap bootstrap, ChannelGroup channels, Timer timer) {
        this.bootstrap = bootstrap;
        this.channels = channels;
        this.timer = timer;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        ctx.channel().attr(Connection.CONNECTION).set(new Connection());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
        ctx.fireChannelActive();
        Connection.getConnectionFromChannel(ctx.channel()).onConnected(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Connection connection = Connection.getConnectionFromChannel(ctx.channel());
        if (connection != null) {
            connection.setStatus(ChannelStatus.UNCONNECTED);
            if (!connection.isClosed()) {
                reconnect(connection, 1);
            }
        }
        ctx.fireChannelInactive();
    }

    private void reconnect(final Connection connection, final int attempts) {
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