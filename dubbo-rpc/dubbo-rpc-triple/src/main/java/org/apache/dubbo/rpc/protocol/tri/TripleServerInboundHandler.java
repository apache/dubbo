package org.apache.dubbo.rpc.protocol.tri;

import com.google.protobuf.RpcCallback;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TripleServerInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final ServerStream serverStream = TripleUtil.getServerStream(ctx);
        if (serverStream != null) {
            serverStream.onData(new ByteBufInputStream((ByteBuf) msg));
        }
    }
}
