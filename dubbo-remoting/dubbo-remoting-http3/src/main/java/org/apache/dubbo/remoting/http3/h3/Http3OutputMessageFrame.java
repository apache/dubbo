package org.apache.dubbo.remoting.http3.h3;

import org.apache.dubbo.remoting.http12.h2.Http2OutputMessageFrame;

import java.io.OutputStream;

public class Http3OutputMessageFrame extends Http2OutputMessageFrame {
    public Http3OutputMessageFrame(OutputStream body) {
        super(body);
    }

    public Http3OutputMessageFrame(boolean endStream) {
        super(endStream);
    }

    public Http3OutputMessageFrame(OutputStream body, boolean endStream) {
        super(body, endStream);
    }
}
