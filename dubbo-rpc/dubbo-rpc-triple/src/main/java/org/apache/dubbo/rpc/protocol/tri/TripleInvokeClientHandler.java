package org.apache.dubbo.rpc.protocol.tri;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class TripleInvokeClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        final ClientStream invoker = TripleUtil.getClientStream(ctx);
        if (invoker != null) {
            invoker.receiveData(msg.retainedDuplicate());
        }
    }
}
