package org.apache.dubbo.remoting.http3.h3;

import io.netty.buffer.ByteBufInputStream;

import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;

import java.io.IOException;
import java.io.InputStream;

public class Http3InputMessageFrame implements Http2InputMessage {
    public static final long END_STREAM_DATA = 0x12ACEF001L;

    private final ByteBufInputStream body;
    private final long streamId;
    private final boolean endStream;

    public Http3InputMessageFrame(ByteBufInputStream body, long streamId) {
        this.body = body;
        this.streamId = streamId;

        try {
            body.mark(8);
            long read = body.readLong();
            body.reset();
            this.endStream = (END_STREAM_DATA == read);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getBody() {
        return body;
    }

    @Override
    public int id() {
        return (int)streamId;
    }

    @Override
    public String name() {
        return "DATA";
    }

    @Override
    public boolean isEndStream() {
        return endStream;
    }
}
