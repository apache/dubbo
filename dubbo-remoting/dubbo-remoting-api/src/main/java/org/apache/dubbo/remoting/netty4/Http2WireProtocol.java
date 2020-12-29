package org.apache.dubbo.remoting.netty4;

import io.netty.handler.codec.http2.Http2FrameLogger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.netty.handler.logging.LogLevel.DEBUG;
import static io.netty.handler.logging.LogLevel.INFO;

public abstract class Http2WireProtocol implements WireProtocol {
    public static final Http2FrameLogger CLIENT_LOGGER = new Http2FrameLogger(DEBUG, "H2_CLIENT");
    public static final Http2FrameLogger SERVER_LOGGER = new Http2FrameLogger(INFO, "H2_SERVER");
    private static final Set<Http2SessionHandler> handlers = ConcurrentHashMap.newKeySet();
    private final ProtocolDetector detector = new Http2ProtocolDetector();

    @Override
    public ProtocolDetector detector() {
        return detector;
    }

    @Override
    public void close() {
        for (Http2SessionHandler handler : handlers) {
            handler.close();
        }
    }
}
