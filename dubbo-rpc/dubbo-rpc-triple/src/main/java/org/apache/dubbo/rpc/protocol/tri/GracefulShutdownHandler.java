package org.apache.dubbo.rpc.protocol.tri;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2ChannelDuplexHandler;


public class GracefulShutdownHandler extends Http2ChannelDuplexHandler {

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        new GracefulShutdown(ctx,"app_requested", null).gracefulShutdown();
    }
}
