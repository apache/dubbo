package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.remoting.exchange.Request;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2SettingsFrame;

import java.io.IOException;

public class TripleClientHandler extends ChannelDuplexHandler {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Request) {
            writeRequest(ctx, (Request) msg, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof Http2SettingsFrame){
            // already handled
            System.out.println("Http2 Setting");
        }else if(msg instanceof Http2GoAwayFrame){
            System.out.println("Http2 goAway!");
        }
    }

    private void writeRequest(ChannelHandlerContext ctx, final Request req, ChannelPromise promise) throws IOException {
        ClientStream clientStream = new ClientStream(ctx, req);
        clientStream.write(req, promise);
    }
}
