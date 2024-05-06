package org.apache.dubbo.rpc.protocol.tri.command.h3;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;

import org.apache.dubbo.remoting.http3.h3.Http3InputMessageFrame;

public class Http3EndStreamQueueCommand extends Http3StreamQueueCommand {
    public static Http3EndStreamQueueCommand create(Future<QuicStreamChannel> streamChannelFuture) {
        return new Http3EndStreamQueueCommand(streamChannelFuture);
    }

    protected Http3EndStreamQueueCommand(Future<QuicStreamChannel> streamChannelFuture) {
        super(streamChannelFuture);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeLong(Http3InputMessageFrame.END_STREAM_DATA);
        ctx.write(new DefaultHttp3DataFrame(buf), promise);
    }
}
