package org.apache.dubbo.remoting.transport.netty4.invocation;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

public class DataBody extends DefaultByteBufHolder {
    public final boolean endStream;
    private int streamId;

    public int getStreamId(){
        return streamId;
    }

    public DataBody(ByteBuf data,  boolean endStream) {
        super(data);
        this.endStream = endStream;
    }
    public DataBody(ByteBuf data, int streamId, boolean endStream) {
        super(data);
        this.streamId = streamId;
        this.endStream = endStream;
    }
}
