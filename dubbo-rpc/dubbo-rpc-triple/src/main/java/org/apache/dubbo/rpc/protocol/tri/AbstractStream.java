package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.Serialization2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.InputStream;

import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public abstract class AbstractStream implements Stream {
    private static final GrpcStatus TOO_MANY_DATA = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
            .withDescription("Too many data");
    private final Serialization2 serialization2 = ExtensionLoader.getExtensionLoader(Serialization2.class).getExtension("protobuf");
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

    public Serialization2 getSerialization2() {
        return serialization2;
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
