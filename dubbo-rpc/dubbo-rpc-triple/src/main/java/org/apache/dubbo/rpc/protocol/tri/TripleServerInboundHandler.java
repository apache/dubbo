package org.apache.dubbo.rpc.protocol.tri;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TripleServerInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final ServerStream invoker = TripleUtil.getServerStream(ctx);
        if (invoker != null) {
            invoker.receiveData((ByteBuf) msg);
        }
    }
}
