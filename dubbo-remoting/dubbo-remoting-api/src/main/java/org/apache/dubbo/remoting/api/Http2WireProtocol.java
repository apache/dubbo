package org.apache.dubbo.remoting.api;

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
    public void close() {
    }
}
