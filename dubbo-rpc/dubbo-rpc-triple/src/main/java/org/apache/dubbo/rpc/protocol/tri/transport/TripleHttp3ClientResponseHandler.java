package org.apache.dubbo.rpc.protocol.tri.transport;

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3ErrorCode;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamFrame;

import io.netty.incubator.codec.quic.QuicStreamChannel;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http3.h3.Http3InputMessageFrame;
import org.apache.dubbo.rpc.TriRpcStatus;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_SERIALIZE_TRIPLE;

public final class TripleHttp3ClientResponseHandler extends SimpleChannelInboundHandler<Http3RequestStreamFrame> {
    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(TripleHttp3ClientResponseHandler.class);

    private final H3TransportListener transportListener;

    public TripleHttp3ClientResponseHandler(H3TransportListener transportListener) {
        this.transportListener = transportListener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3RequestStreamFrame msg) throws Exception {
        if (msg instanceof Http3HeadersFrame) {
            final Http3HeadersFrame headers = (Http3HeadersFrame) msg;
            if (headers.headers().contains("reset")) { // RESET frame
                onResetRead(ctx, headers);
            }
            else { // HEADERS frame
                transportListener.onHeader(headers.headers());
            }
        } else if (msg instanceof Http3DataFrame) {
            final Http3DataFrame data = (Http3DataFrame) msg;
            data.content().retain(); // because TriDecoder.deframe() will release it
            Http3InputMessageFrame inputFrame = new Http3InputMessageFrame(
                    new ByteBufInputStream(data.content()), ((QuicStreamChannel)ctx.channel()).streamId());
            transportListener.onData(data.content(), inputFrame.isEndStream());
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        final TriRpcStatus status = TriRpcStatus.INTERNAL.withCause(cause);
        LOGGER.warn(
                PROTOCOL_FAILED_SERIALIZE_TRIPLE,
                "",
                "",
                "Meet Exception on ClientResponseHandler, status code is: " + status.code,
                cause);
        transportListener.cancelByRemote(Http3ErrorCode.H3_INTERNAL_ERROR.getCode());
        ctx.close();
    }

    private void onResetRead(ChannelHandlerContext ctx, Http3HeadersFrame resetFrame) {
        long errorCode = Long.parseLong(resetFrame.headers().get("reset").toString());
        LOGGER.warn(
                PROTOCOL_FAILED_SERIALIZE_TRIPLE,
                "",
                "",
                "Triple Client received remote reset errorCode=" + errorCode);
        transportListener.cancelByRemote(errorCode);
        ctx.close();
    }
}
