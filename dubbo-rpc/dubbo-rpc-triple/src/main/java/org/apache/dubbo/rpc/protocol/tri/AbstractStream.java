package org.apache.dubbo.rpc.protocol.tri;


import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.InputStream;

import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public abstract class AbstractStream implements Stream {
    private static final GrpcStatus TOO_MANY_DATA = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
            .withDescription("Too many data");
    private final boolean needWrap;
    private final ChannelHandlerContext ctx;
    private Http2Headers headers;
    private Http2Headers te;
    private InputStream data;

    protected AbstractStream(ChannelHandlerContext ctx, boolean needWrap) {
        this.ctx = ctx;
        this.needWrap = needWrap;
    }

    public boolean isNeedWrap() {
        return needWrap;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public Http2Headers getHeaders() {
        return headers;
    }

    public Http2Headers getTe() {
        return te;
    }

    public InputStream getData() {
        return data;
    }

    @Override
    public void onData(InputStream in) {
        if (data != null) {
            responseErr(ctx, TOO_MANY_DATA);
            return;
        }

        this.data = in;
    }

    public void onHeaders(Http2Headers headers) {
        if (this.headers == null) {
            this.headers = headers;
        } else if (te == null) {
            this.te = headers;
        }
    }
}
