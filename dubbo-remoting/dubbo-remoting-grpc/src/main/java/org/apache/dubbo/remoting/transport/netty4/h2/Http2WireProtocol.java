package org.apache.dubbo.remoting.transport.netty4.h2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.util.AsciiString;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.transport.netty4.DetectionResult;
import org.apache.dubbo.remoting.transport.netty4.Status;
import org.apache.dubbo.remoting.transport.netty4.WireProtocol;
import org.apache.dubbo.remoting.transport.netty4.grpc.GrpcElf;
import org.apache.dubbo.remoting.transport.netty4.invocation.DataBody;
import org.apache.dubbo.remoting.transport.netty4.invocation.DataHeader;

import static io.netty.handler.logging.LogLevel.DEBUG;

@SPI
// todo extends AbstractProxyProtocol
public abstract class Http2WireProtocol implements WireProtocol {
    // todo
    public static final Http2FrameLogger CLIENT_LOGGER = new Http2FrameLogger(DEBUG, Http2WireProtocol.class);
    public static final Http2FrameLogger SERVER_LOGGER = new Http2FrameLogger(DEBUG, Http2WireProtocol.class);

    private static final ByteBuf PREFACE = Http2CodecUtil.connectionPrefaceBuf();
    private final HttpServerUpgradeHandler.UpgradeCodecFactory upgradeCodecFactory = protocol -> {
        final Http2ConnectionHandler connectionHandler = new Http2ServerConnectionHandlerBuilder()
            .frameLogger(CLIENT_LOGGER)
            .frameListener(new Http2ServerFrameListener())
            .build();
        if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
            return new Http2ServerUpgradeCodec(connectionHandler);
        } else {
            return null;
        }
    };

    @Override
    public int id() {
        return 0;
    }

    @Override
    public DetectionResult accept(ByteBuf in) {
        if (ByteBufUtil.equals(in, 0,
            PREFACE, 0,
            PREFACE.readableBytes())) {
            return DetectionResult.ACCEPTED;
        }
        if (ByteBufUtil.equals(in, 0, PREFACE, 0, in.readableBytes())) {
            return DetectionResult.NO_ENOUGH_DATA;
        }
        return DetectionResult.UNRECOGNIZED;
    }

    protected void respondWithHttpError(CharSequence code, Status status, String msg, int streamId, ChannelHandlerContext ctx) {
        Http2Headers http2Headers = new DefaultHttp2Headers();
        http2Headers.setInt(GrpcElf.GRPC_STATUS, status.getCode().value())
            .set(GrpcElf.GRPC_MESSAGE, msg)
            .status(code)
            .set(HttpHeaderNames.CONTENT_TYPE, "text/plain; encoding=utf-8");
        ctx.write(new DataHeader(http2Headers, streamId, false));
        ByteBuf msgBuf = ByteBufUtil.writeUtf8(ctx.alloc(), msg);
        ctx.write(new DataBody(msgBuf, streamId, true));
        ctx.close();

    }

    @Override
    public void initServerChannel(ChannelHandlerContext ctx) {
        final ChannelPipeline p = ctx.pipeline();
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory);
        final Http2ConnectionHandler connectionHandler = new Http2ServerConnectionHandlerBuilder()
            .frameLogger(SERVER_LOGGER)
            .frameListener(null)
            .build();
        final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler =
            new CleartextHttp2ServerUpgradeHandler(sourceCodec, upgradeHandler, connectionHandler);

        p.addLast(cleartextHttp2ServerUpgradeHandler);
        p.addLast(new Http2ServerSessionHandler());
        p.addLast(new UserEventLogger());
    }

    /**
     * Class that logs any User Events triggered on this channel.
     */
    private static class UserEventLogger extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            //            System.out.println("User Event Triggered: " + evt);
            ctx.fireUserEventTriggered(evt);
        }
    }
}
