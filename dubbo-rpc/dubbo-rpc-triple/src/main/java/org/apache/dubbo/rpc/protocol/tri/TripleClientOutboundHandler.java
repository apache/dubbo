package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.Serialization2;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.RpcInvocation;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;

public class TripleClientOutboundHandler extends ChannelOutboundHandlerAdapter {
    private final Serialization2 serialization2;

    public TripleClientOutboundHandler() {
        this.serialization2 = ExtensionLoader.getExtensionLoader(Serialization2.class).getExtension("protobuf");
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Request) {
            final Request req = (Request) msg;
            ClientStream clientStream = new ClientStream(ctx, req);
            final RpcInvocation invocation = (RpcInvocation) req.getData();
            Http2Headers headers = new DefaultHttp2Headers()
                    .method(HttpMethod.POST.asciiName())
                    .path("/" + invocation.getServiceName() + "/" + invocation.getMethodName())
                    .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
                    .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
            DefaultHttp2HeadersFrame frame = new DefaultHttp2HeadersFrame(headers);
            final TripleHttp2ClientResponseHandler responseHandler = new TripleHttp2ClientResponseHandler();

            final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(ctx.channel());
            final Http2StreamChannel streamChannel = streamChannelBootstrap.open().syncUninterruptibly().getNow();
            TripleUtil.setClientStream(streamChannel, clientStream);
            streamChannel.pipeline().addLast(responseHandler)
                    .addLast(new GrpcDataDecoder(Integer.MAX_VALUE))
                    .addLast(new TripleClientInboundHandler());
            streamChannel.write(frame);
            if (invocation.getArguments().length == 1) {
                final ByteBuf buf = ctx.alloc().buffer();
                buf.writeByte(0);
                buf.writeInt(0);
                final ByteBufOutputStream bos = new ByteBufOutputStream(buf);
                final int size = serialization2.serialize(invocation.getArguments()[0], bos);
                buf.setInt(1, size);
                streamChannel.write(new DefaultHttp2DataFrame(bos.buffer(), true));
            } else {
                // TODO wrapper
            }
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
