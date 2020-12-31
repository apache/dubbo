package org.apache.dubbo.remoting.netty4;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2ConnectionHandler;


public class Http2SessionHandler extends ChannelDuplexHandler {
    private Http2ConnectionHandler handler;
    private ChannelHandlerContext ctx;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent
                || evt instanceof CleartextHttp2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent) {
            this.handler = ctx.pipeline().get(Http2ConnectionHandler.class);
            this.ctx = ctx;
        }
        ctx.fireUserEventTriggered(evt);
    }

    public void close() {
        if (ctx.channel().isOpen()) {
            try {
                handler.close(ctx, ctx.voidPromise());
            } catch (Exception e) {
                throw new RuntimeException("Closing error:", e);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }
}
