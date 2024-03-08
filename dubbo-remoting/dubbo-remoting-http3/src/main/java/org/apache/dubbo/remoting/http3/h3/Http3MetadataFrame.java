package org.apache.dubbo.remoting.http3.h3;

import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h2.Http2Header;

public class Http3MetadataFrame implements Http2Header {
    private final HttpHeaders httpHeaders;

    public Http3MetadataFrame(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    @Override
    public HttpHeaders headers() {
        return httpHeaders;
    }

    @Override
    public int id() {
        return -1;
    }

    @Override
    public boolean isEndStream() {
        // always return false. endStream will be triggered by a user event
        return false;
    }
}
