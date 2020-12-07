package org.apache.dubbo.remoting.transport.netty4.h2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import org.apache.dubbo.remoting.transport.netty4.WireProtocol;
import org.apache.dubbo.remoting.transport.netty4.invocation.DataHeader;
import org.apache.dubbo.remoting.transport.netty4.invocation.StreamInboundListener;

public class Http2ServerFrameListener extends Http2FrameAdapter {

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
        short weight, boolean exclusive, int padding, boolean endStream) throws Http2Exception {
        onHeadersRead(ctx, streamId, headers, padding, endStream);
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding,
        boolean endStream) throws Http2Exception {
        final Http2ServerSessionHandler sessionHandler = ctx.pipeline().get(Http2ServerSessionHandler.class);
        final Http2Connection connection = sessionHandler.getConnection();
        final Http2Stream stream = connection.stream(streamId);
        final CharSequence mimeType = HttpUtil.getMimeType(headers.get(HttpHeaderNames.CONTENT_TYPE));
        final WireProtocol protocol = Http2WireProtocol.of(mimeType);
        final Http2ConnectionEncoder encoder = sessionHandler.getHandler().encoder();
        if (protocol == null) {
            //if (log.isWarnEnabled()) {
            //    log.warn("Error={} meta={}", "No protocol found", headers);
            //}
            Http2Headers outHeaders = new DefaultHttp2Headers()
                .status(HttpResponseStatus.NOT_FOUND.codeAsText())
                .set(HttpHeaderNames.CONTENT_TYPE, "text/plain; encoding=utf-8");
            String msg = "No protocol found for contentType" + mimeType;
            encoder.writeHeaders(ctx, streamId, outHeaders, 0, false, ctx.voidPromise());
            ByteBuf msgBuf = ByteBufUtil.writeUtf8(ctx.alloc(), msg);
            encoder.writeData(ctx, streamId, msgBuf, 0, true, ctx.voidPromise());
        } else {
            final StreamInboundListener listener = protocol.createServerListener(new DataHeader(headers, streamId, endStream), ctx);
            stream.setProperty(sessionHandler.getInboundKey(), listener);
        }
    }

}
