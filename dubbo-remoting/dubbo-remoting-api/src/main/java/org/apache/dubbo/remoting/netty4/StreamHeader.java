package org.apache.dubbo.remoting.netty4;

import io.netty.handler.codec.http2.Http2Headers;

public class StreamHeader extends BaseStreamState implements StreamState {
    private final Http2Headers headers;

    public StreamHeader(int id, Http2Headers headers, boolean endOfStream) {
        super(endOfStream, id);
        this.headers = headers;
    }

    public Http2Headers headers() {
        return headers;
    }

}
