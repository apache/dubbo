package org.apache.dubbo.remoting.transport.netty4.h2;

import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Settings;

public class Http2ServerConnectionHandler extends Http2ConnectionHandler {

    protected Http2ServerConnectionHandler(Http2ConnectionDecoder decoder,
        Http2ConnectionEncoder encoder,
        Http2Settings initialSettings) {
        super(decoder, encoder, initialSettings);
    }
}
