package org.apache.dubbo.rpc.protocol.tri.command.h3;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;

import java.util.Arrays;

public class Http3DataQueueCommand extends Http3StreamQueueCommand {
    private final byte[] data;
    private final int compressFlag;

    public static Http3DataQueueCommand create(Future<QuicStreamChannel> streamChannelFuture,
                                               byte[] data, int compressFlag) {
        return new Http3DataQueueCommand(streamChannelFuture, data, compressFlag);
    }

    protected Http3DataQueueCommand(Future<QuicStreamChannel> streamChannelFuture,
                                    byte[] data, int compressFlag) {
        super(streamChannelFuture);
        this.data = data;
        this.compressFlag = compressFlag;
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        if (null == data) {
            ctx.write(new DefaultHttp3DataFrame(Unpooled.EMPTY_BUFFER), promise);
        }
        else {
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeByte(compressFlag);
            buf.writeInt(data.length);
            buf.writeBytes(data);
            ctx.write(new DefaultHttp3DataFrame(buf), promise);
        }
    }
}
