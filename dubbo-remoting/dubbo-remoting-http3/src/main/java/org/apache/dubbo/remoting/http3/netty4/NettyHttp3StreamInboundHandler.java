package org.apache.dubbo.remoting.http3.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownEvent;

public abstract class NettyHttp3StreamInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == ChannelInputShutdownEvent.INSTANCE) {
            channelEndStream(ctx);
        }
        ctx.fireUserEventTriggered(evt);
    }

    protected abstract void channelEndStream(ChannelHandlerContext ctx) throws Exception;
}
