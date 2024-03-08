package org.apache.dubbo.remoting.http3.h3;

import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;

import java.io.InputStream;

public class Http3InputMessageFrame implements Http2InputMessage {
    private final InputStream body;

    public Http3InputMessageFrame(InputStream body) {
        this.body = body;
    }

    @Override
    public InputStream getBody() {
        return body;
    }

    @Override
    public int id() {
        return -1;
    }

    @Override
    public String name() {
        return "DATA";
    }

    @Override
    public boolean isEndStream() {
        // always return false. endStream will be triggered by a user event
        return false;
    }
}
