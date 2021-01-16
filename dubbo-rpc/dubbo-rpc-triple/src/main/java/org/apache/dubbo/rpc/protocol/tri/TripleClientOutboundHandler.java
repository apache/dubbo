package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.remoting.exchange.Request;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.io.IOException;

public class TripleClientOutboundHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Request) {
            writeRequest(ctx, (Request) msg, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    private void writeRequest(ChannelHandlerContext ctx, final Request req, ChannelPromise promise) throws IOException {
        ClientStream clientStream = new ClientStream(ctx, req);
        clientStream.write(req, promise);
    }
}
