package org.apache.dubbo.rpc.protocol.tri;

import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2WindowUpdateFrame;

public class TripleFlowControl {

    private Http2Connection http2Connection;

    private int windowSizeIncrement;

    private Http2WindowUpdateFrame http2WindowUpdateFrame;

    public TripleFlowControl(Http2Connection http2Connection,int windowSizeIncrement,Http2WindowUpdateFrame http2WindowUpdateFrame){
        this.http2Connection = http2Connection;
        this.windowSizeIncrement = windowSizeIncrement;
        this.http2WindowUpdateFrame = http2WindowUpdateFrame;
    }

    public Http2Connection getHttp2Connection() {
        return http2Connection;
    }

    public void setHttp2Connection(Http2Connection http2Connection) {
        this.http2Connection = http2Connection;
    }

    public int getWindowSizeIncrement() {
        return windowSizeIncrement;
    }

    public void setWindowSizeIncrement(int windowSizeIncrement) {
        this.windowSizeIncrement = windowSizeIncrement;
    }

    public Http2WindowUpdateFrame getHttp2WindowUpdateFrame() {
        return http2WindowUpdateFrame;
    }

    public void setHttp2WindowUpdateFrame(Http2WindowUpdateFrame http2WindowUpdateFrame) {
        this.http2WindowUpdateFrame = http2WindowUpdateFrame;
    }

}
