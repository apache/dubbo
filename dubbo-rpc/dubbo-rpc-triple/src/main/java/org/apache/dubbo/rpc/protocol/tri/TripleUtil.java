package org.apache.dubbo.rpc.protocol.tri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AttributeKey;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class TripleUtil {

    public static final AttributeKey<UnaryInvoker> INVOKER_KEY=AttributeKey.newInstance("tri_invoker");

    public static UnaryInvoker getInvoker(ChannelHandlerContext ctx){
        return ctx.channel().attr(TripleUtil.INVOKER_KEY).get();
    }
    /**
     * must starts from application/grpc
     */
    public static boolean supportContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith(TripleConstant.APPLICATION_GRPC);
    }

    public static void responseErr(ChannelHandlerContext ctx, GrpcStatus status, String message) {
        Http2Headers trailers = new DefaultHttp2Headers()
                .status(OK.codeAsText())
                .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
                .setInt(TripleConstant.STATUS_KEY, status.code)
                .set(TripleConstant.MESSAGE_KEY, message);
        ctx.write(new DefaultHttp2HeadersFrame(trailers, true));
    }

    public static void responsePlainTextError(ChannelHandlerContext ctx, int code, int statusCode, String
            msg) {
        Http2Headers headers = new DefaultHttp2Headers(true)
                .status("" + code)
                .setInt(TripleConstant.STATUS_KEY, statusCode)
                .set(TripleConstant.MESSAGE_KEY, msg)
                .set(TripleConstant.CONTENT_TYPE_KEY, "text/plain; encoding=utf-8");
        ctx.write(new DefaultHttp2HeadersFrame(headers));
        ByteBuf buf = ByteBufUtil.writeUtf8(ctx.alloc(), msg);
        ctx.write(new DefaultHttp2DataFrame(buf, true));
    }
}
