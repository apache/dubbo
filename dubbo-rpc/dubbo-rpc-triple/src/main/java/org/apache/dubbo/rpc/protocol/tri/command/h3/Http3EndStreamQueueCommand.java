package org.apache.dubbo.rpc.protocol.tri.command.h3;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;

public class Http3EndStreamQueueCommand extends Http3StreamQueueCommand {
    public static Http3EndStreamQueueCommand create(Future<QuicStreamChannel> streamChannelFuture) {
        return new Http3EndStreamQueueCommand(streamChannelFuture);
    }

    protected Http3EndStreamQueueCommand(Future<QuicStreamChannel> streamChannelFuture) {
        super(streamChannelFuture);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        // Do nothing
    }
}
