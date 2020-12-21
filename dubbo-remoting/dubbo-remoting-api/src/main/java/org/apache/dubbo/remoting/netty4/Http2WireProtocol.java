package org.apache.dubbo.remoting.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameLogger;

import static io.netty.handler.logging.LogLevel.DEBUG;

public abstract class Http2WireProtocol implements WireProtocol {
    public static final Http2FrameLogger CLIENT_LOGGER = new Http2FrameLogger(DEBUG, "H2_CLIENT");
    public static final Http2FrameLogger SERVER_LOGGER = new Http2FrameLogger(DEBUG, "H2_SERVER");
    private final ProtocolDetector detector = new Http2ProtocolDetector();

    @Override
    public ProtocolDetector detector() {
        return detector;
    }

    @Override
    public void configServerPipeline(ChannelHandlerContext ctx) {
        final ChannelPipeline p = ctx.pipeline();
        final Http2Connection connection = new DefaultHttp2Connection(true);
        final Http2ConnectionHandler handler = new DubboConnectionHandlerBuilder()
                .connection(connection)
                .frameLogger(SERVER_LOGGER)
                .frameListener(frameListener())
                .build();
        p.addLast(handler);
    }

    abstract Http2FrameListener frameListener();

    @Override
    public void configClientPipeline(ChannelHandlerContext ctx) {

    }
}
