package org.apache.dubbo.rpc.protocol.tri;


import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2ChannelDuplexHandler;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2PingFrame;

import static org.apache.dubbo.rpc.protocol.tri.GracefulShutdown.GRACEFUL_SHUTDOWN_PING;

public class TripleServerConnectionHandler extends Http2ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(TripleServerConnectionHandler.class);
    private GracefulShutdown gracefulShutdown;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2PingFrame) {
            if (((Http2PingFrame) msg).content() == GRACEFUL_SHUTDOWN_PING) {
                if (gracefulShutdown == null) {
                    // this should never happen
                    logger.warn("Received GRACEFUL_SHUTDOWN_PING Ack but gracefulShutdown is null");
                } else {
                    gracefulShutdown.secondGoAwayAndClose(ctx);
                }
            }
        } else if (msg instanceof Http2GoAwayFrame) {
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn(String.format("Channel:%s Error", ctx.channel()), cause);
        ctx.close();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (gracefulShutdown == null) {
            gracefulShutdown = new GracefulShutdown(ctx, "app_requested", null, promise);
        }
        gracefulShutdown.gracefulShutdown();
    }
}
