package org.apache.dubbo.rpc.protocol.tri.command.h3;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;

import java.util.Map;

public class Http3HeaderQueueCommand extends Http3StreamQueueCommand {
    private Http2Headers h2Headers;

    public static Http3HeaderQueueCommand createHeaders(
            Future<QuicStreamChannel> streamChannelFuture, Http2Headers headers) {
        return new Http3HeaderQueueCommand(streamChannelFuture, headers);
    }

    protected Http3HeaderQueueCommand(Future<QuicStreamChannel> streamChannelFuture, Http2Headers headers) {
        super(streamChannelFuture);
        this.h2Headers = headers;
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        Http3HeadersFrame frame = new DefaultHttp3HeadersFrame();
        Http3Headers h3Headers = frame.headers();
        for (Map.Entry<CharSequence, CharSequence> entry: h2Headers) {
            h3Headers.add(entry.getKey(), entry.getValue());
        }
        ctx.write(frame, promise);
    }
}
