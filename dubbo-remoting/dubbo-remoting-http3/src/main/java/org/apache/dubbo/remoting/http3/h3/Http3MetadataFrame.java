package org.apache.dubbo.remoting.http3.h3;

import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h2.Http2Header;

public class Http3MetadataFrame implements Http2Header {
    private final HttpHeaders httpHeaders;
    private final long streamId;

    public Http3MetadataFrame(HttpHeaders httpHeaders) {
        this(httpHeaders, -1);
    }

    public Http3MetadataFrame(HttpHeaders httpHeaders, long streamId) {
        this.httpHeaders = httpHeaders;
        this.streamId = streamId;
    }

    @Override
    public HttpHeaders headers() {
        return httpHeaders;
    }

    @Override
    public int id() {
        return (int)streamId;
    }

    @Override
    public boolean isEndStream() {
        return false;
    }
}
