package org.apache.dubbo.remoting.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Settings;

public class DubboHttp2ConnectionHandler extends Http2ConnectionHandler {
    protected DubboHttp2ConnectionHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) {
        super(decoder, encoder, initialSettings);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof StreamHeader) {
            encoder().writeHeaders(ctx, ((StreamHeader) msg).id(), ((StreamHeader) msg).headers(), 0,
                    ((StreamHeader) msg).endOfStream(), promise);
        } else if (msg instanceof StreamData) {
            encoder().writeData(ctx, ((StreamData) msg).id(), ((StreamData) msg).data(), 0,
                    ((StreamData) msg).endOfStream(), promise);
        } else {
            ctx.write(msg);
        }
    }
}
