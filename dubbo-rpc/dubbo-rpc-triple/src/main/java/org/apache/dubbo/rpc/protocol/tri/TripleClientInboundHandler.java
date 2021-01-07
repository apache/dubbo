package org.apache.dubbo.rpc.protocol.tri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TripleClientInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final ClientStream invoker = TripleUtil.getClientStream(ctx);
        if (invoker != null) {
            invoker.onData(new ByteBufInputStream((ByteBuf) msg));
        }
    }
}
