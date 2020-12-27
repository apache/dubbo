package org.apache.dubbo.remoting.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Settings;

import static io.netty.handler.codec.http2.Http2Error.NO_ERROR;
import static io.netty.util.CharsetUtil.UTF_8;

public class DubboHttp2ConnectionHandler extends Http2ConnectionHandler {

    static final long GRACEFUL_SHUTDOWN_PING = 0x97ACEF001L;
    private final Http2ConnectionEncoder encoder;

    protected DubboHttp2ConnectionHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) {
        super(decoder, encoder, initialSettings);
        this.encoder = encoder;
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        goAway(
            ctx,
            Integer.MAX_VALUE,
            0x0,
            ByteBufUtil.writeAscii(ctx.alloc(), "app_requested"),
            ctx.newPromise());

        encoder.writePing(ctx, false /* isAck */, GRACEFUL_SHUTDOWN_PING, ctx.newPromise());

        super.close(ctx, promise);
    }

    public ChannelFuture goAway(final ChannelHandlerContext ctx, final int lastStreamId, final long errorCode,
        final ByteBuf debugData, ChannelPromise promise) {
        final DubboHttp2ConnectionHandler connectionHandler = ctx.pipeline().get(DubboHttp2ConnectionHandler.class);
        final Http2ConnectionEncoder encoder = connectionHandler.encoder();
        Http2Connection connection = connectionHandler.encoder().connection();
        promise = promise.unvoid();
        try {
            connection.goAwaySent(lastStreamId, errorCode, debugData);
        } catch (Throwable cause) {
            debugData.release();
            promise.tryFailure(cause);
            return promise;
        }

        debugData.retain();
        ChannelFuture future = encoder.frameWriter().writeGoAway(ctx, lastStreamId, errorCode, debugData, promise);

        if (future.isDone()) {
            processGoAwayWriteResult(ctx, lastStreamId, errorCode, debugData, future);
        } else {
            future.addListener((ChannelFutureListener) future1 -> processGoAwayWriteResult(ctx, lastStreamId, errorCode, debugData, future1));
        }

        return future;
    }

    private static void processGoAwayWriteResult(final ChannelHandlerContext ctx, final int lastStreamId,
        final long errorCode, final ByteBuf debugData, ChannelFuture future) {
        try {
            if (future.isSuccess()) {
                if (errorCode != NO_ERROR.code()) {
                    System.out.println("{} Sending GOAWAY failed: lastStreamId '{" + lastStreamId
                        + "}', errorCode '{" + errorCode
                        + "}', " +
                        "debugData '{" + debugData.toString(UTF_8) + "}'. Forcing shutdown of the connection.");
                    ctx.close();
                }
            } else {
                System.out.println("{} Sending GOAWAY failed: lastStreamId '{" + lastStreamId
                    + "}', errorCode '{" + errorCode
                    + "}', " +
                    "debugData '{" + debugData.toString(UTF_8) + "}'. Forcing shutdown of the connection.");

                ctx.close();
            }
        } finally {
            // We're done with the debug data now.
            debugData.release();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof StreamHeader) {
            encoder().writeHeaders(ctx, ((StreamHeader) msg).id(), ((StreamHeader) msg).headers(), 0,
                    ((StreamHeader) msg).endOfStream(), promise);
        } else if (msg instanceof StreamData) {
            encoder().writeData(ctx, ((StreamData) msg).id(), ((StreamData) msg).data(), 0,
                    ((StreamData) msg).endOfStream(), promise);
        } else {
            ctx.write(msg);
        }
    }
}
