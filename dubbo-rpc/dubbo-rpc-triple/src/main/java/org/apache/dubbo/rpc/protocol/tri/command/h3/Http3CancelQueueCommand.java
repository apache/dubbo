package org.apache.dubbo.rpc.protocol.tri.command.h3;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3ErrorCode;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;

public class Http3CancelQueueCommand extends Http3StreamQueueCommand {
    private final Http3ErrorCode errorCode;

    public static Http3CancelQueueCommand create(
            Future<QuicStreamChannel> streamChannelFuture, Http3ErrorCode errorCode) {
        return new Http3CancelQueueCommand(streamChannelFuture, errorCode);
    }

    private Http3CancelQueueCommand(Future<QuicStreamChannel> streamChannelFuture, Http3ErrorCode errorCode) {
        super(streamChannelFuture);
        this.errorCode = errorCode;
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        Http3HeadersFrame frame = new DefaultHttp3HeadersFrame();
        frame.headers().addInt("reset", errorCode.getCode());
        ctx.write(frame, promise);
    }
}
