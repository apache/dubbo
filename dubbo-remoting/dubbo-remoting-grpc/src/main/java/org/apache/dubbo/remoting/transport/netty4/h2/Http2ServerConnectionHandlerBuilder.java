package org.apache.dubbo.remoting.transport.netty4.h2;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Settings;

public class Http2ServerConnectionHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2ServerConnectionHandler, Http2ServerConnectionHandlerBuilder> {

    public Http2ServerConnectionHandlerBuilder frameLogger(Http2FrameLogger frameLogger) {
        return super.frameLogger(frameLogger);
    }


    public Http2ServerConnectionHandlerBuilder frameListener(Http2FrameListener frameListener) {
        return super.frameListener(frameListener);
    }

    public Http2ServerConnectionHandler build() {
        return super.build();
    }

    @Override
    protected Http2ServerConnectionHandler build(Http2ConnectionDecoder decoder,
        Http2ConnectionEncoder encoder, Http2Settings initialSettings) throws Exception {
        return new Http2ServerConnectionHandler(decoder, encoder, initialSettings);
    }
}
