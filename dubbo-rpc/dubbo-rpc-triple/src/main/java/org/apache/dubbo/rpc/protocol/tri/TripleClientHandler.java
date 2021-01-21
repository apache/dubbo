package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.RpcInvocation;

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
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        super.close(ctx, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2SettingsFrame) {
            // already handled
        } else if (msg instanceof Http2GoAwayFrame) {
            final Connection connection = Connection.getConnectionFromChannel(ctx.channel());
            if (connection != null) {
                connection.onIdle();
            }
        }
    }

    private void writeRequest(ChannelHandlerContext ctx, final Request req, ChannelPromise promise) throws IOException {
        final RpcInvocation inv = (RpcInvocation) req.getData();
        final boolean needWrapper = TripleUtil.needWrapper(inv.getParameterTypes());
        final URL url = inv.getInvoker().getUrl();
        ClientStream clientStream = new ClientStream(url, ctx, needWrapper, req);
        clientStream.write(req, promise);
    }
}
