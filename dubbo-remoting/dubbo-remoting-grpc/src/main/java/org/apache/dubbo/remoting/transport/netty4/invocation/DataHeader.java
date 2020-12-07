package org.apache.dubbo.remoting.transport.netty4.invocation;

public class DataHeader {
    public final Object header;
    public final boolean endStream;
    public int streamId;

    public DataHeader(Object header) {
        this.header = header;
        this.endStream = false;
    }

    public DataHeader(Object header, boolean endStream) {
        this.header = header;
        this.endStream = endStream;
    }

    public DataHeader(Object header, int streamId, boolean endStream) {
        this.header = header;
        this.streamId = streamId;
        this.endStream = endStream;
    }
}
