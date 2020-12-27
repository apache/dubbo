package org.apache.dubbo.remoting.netty4;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.util.AsciiString;
import org.apache.dubbo.common.utils.ConcurrentHashSet;

import static io.netty.handler.logging.LogLevel.DEBUG;

public abstract class Http2WireProtocol implements WireProtocol {
    public static final Http2FrameLogger CLIENT_LOGGER = new Http2FrameLogger(DEBUG, "H2_CLIENT");
    public static final Http2FrameLogger SERVER_LOGGER = new Http2FrameLogger(DEBUG, "H2_SERVER");
    private final ProtocolDetector detector = new Http2ProtocolDetector();
    private static final Set<Http2SessionHandler> handlers = ConcurrentHashMap.newKeySet();
    @Override
    public ProtocolDetector detector() {
        return detector;
    }
    private final HttpServerUpgradeHandler.UpgradeCodecFactory upgradeCodecFactory = protocol -> {
        final Http2ConnectionHandler connectionHandler = new DubboConnectionHandlerBuilder()
            .frameLogger(CLIENT_LOGGER)
            .frameListener(frameListener())
            .build();
        if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
            return new Http2ServerUpgradeCodec(connectionHandler);
        } else {
            return null;
        }
    };
    @Override
    public void configServerPipeline(ChannelHandlerContext ctx) {
        final ChannelPipeline p = ctx.pipeline();
        final Http2Connection connection = new DefaultHttp2Connection(true);
        final Http2SessionHandler sessionHandler = new Http2SessionHandler();
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory);
        final Http2ConnectionHandler handler = new DubboConnectionHandlerBuilder()
            .connection(connection)
            .frameLogger(SERVER_LOGGER)
            .frameListener(frameListener())
            .build();
        final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler =
            new CleartextHttp2ServerUpgradeHandler(sourceCodec, upgradeHandler, handler);

        p.addLast(cleartextHttp2ServerUpgradeHandler);
        p.addLast(sessionHandler);
        handlers.add(sessionHandler);
    }

    protected abstract Http2FrameListener frameListener();

    @Override
    public void configClientPipeline(ChannelHandlerContext ctx) {

    }

    @Override
    public void close() {
        for (Http2SessionHandler handler : handlers) {
            handler.close();
        }
    }
}
