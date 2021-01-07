package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;

public final class TripleHttp2ClientResponseHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleHttp2ClientResponseHandler.class);

    public TripleHttp2ClientResponseHandler() {
        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            onHeadersRead(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame msg) {
        TripleUtil.getClientStream(ctx).onHeaders(msg.headers());
        if (msg.isEndStream()) {
            final ClientStream clientStream = TripleUtil.getClientStream(ctx);
            clientStream.halfClose();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final ClientStream clientStream = TripleUtil.getClientStream(ctx);
        final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withCause(cause);
        clientStream.onError(status);
        ctx.close();
    }

    public void onDataRead(ChannelHandlerContext ctx, Http2DataFrame msg) throws Exception {
        super.channelRead(ctx, msg.content());
        if (msg.isEndStream()) {
            final ClientStream clientStream = TripleUtil.getClientStream(ctx);
            // stream already closed;
            if (clientStream != null) {
                clientStream.halfClose();
            }
        }
    }
}
