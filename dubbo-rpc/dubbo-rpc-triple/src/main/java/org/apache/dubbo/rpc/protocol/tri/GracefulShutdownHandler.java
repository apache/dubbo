package org.apache.dubbo.rpc.protocol.tri;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2ChannelDuplexHandler;
import io.netty.handler.codec.http2.Http2PingFrame;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import static org.apache.dubbo.rpc.protocol.tri.GracefulShutdown.GRACEFUL_SHUTDOWN_PING;

public class GracefulShutdownHandler extends Http2ChannelDuplexHandler {
    private GracefulShutdown gracefulShutdown;
    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdownHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);

        if (msg instanceof Http2PingFrame) {
            if (((Http2PingFrame)msg).content() == GRACEFUL_SHUTDOWN_PING) {
                if (gracefulShutdown == null) {
                    // this should never happen
                    logger.warn("Received GRACEFUL_SHUTDOWN_PING Ack but gracefulShutdown is null");
                } else {
                    gracefulShutdown.secondGoAwayAndClose(ctx);
                }
            }
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (gracefulShutdown == null) {
            gracefulShutdown = new GracefulShutdown(ctx,"app_requested", null);
            gracefulShutdown.gracefulShutdown();
        }
    }
}
