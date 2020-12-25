package org.apache.dubbo.rpc.protocol.tri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;

public class Http2Request {
    private int streamId;
    private ByteBuf commulation;
    private volatile Http2Headers headers;
    private Http2Stream http2Stream;
    private Http2Connection.PropertyKey streamKey;
    private String marshaller;
    private ByteBufAllocator allocator;
    private byte[] content;

    public Http2Request(int streamId, Http2Stream http2Stream
        , Http2Headers headers
        //, AbstractHttp2CodecHandler http2CodecHandler
        , Http2Connection.PropertyKey streamKey, String marshaller, ByteBufAllocator allocator) {
        this.streamId = streamId;
        this.http2Stream = http2Stream;
        this.headers = headers;
        //this.http2CodecHandler = http2CodecHandler;
        this.streamKey = streamKey;
        this.marshaller = marshaller;
        this.allocator = allocator;
        this.commulation = allocator.buffer();
    }

    public Http2Headers getHeaders() {
        return headers;
    }

    public ByteBuf getData() {
        return commulation;
    }

    public int getStreamId() {
        return streamId;
    }

    public byte[] content() {
        if (content != null) {
            return content;
        }
        this.content = new byte[commulation.readableBytes()];
        commulation.readBytes(content);
        commulation.release();
        return content;
    }

    public void cumulate(ByteBuf byteBuf) {
        commulation = cumulate(allocator, commulation, byteBuf);
    }

    public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
        final ByteBuf buffer;
        if (cumulation.writerIndex() > cumulation.maxCapacity() - in.readableBytes()
            || cumulation.refCnt() > 1 || cumulation.isReadOnly()) {
            // Expand cumulation (by replace it) when either there is not more room in the buffer
            // or if the refCnt is greater then 1 which may happen when the user use slice().retain() or
            // duplicate().retain() or if its read-only.
            //
            // See:
            // - https://github.com/netty/netty/issues/2327
            // - https://github.com/netty/netty/issues/1764
            buffer = expandCumulation(alloc, cumulation, in.readableBytes());
        } else {
            buffer = cumulation;
        }
        buffer.writeBytes(in);
        return buffer;
    }

    private ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf cumulation, int readable) {
        ByteBuf oldCumulation = cumulation;
        cumulation = alloc.buffer(oldCumulation.readableBytes() + readable);
        cumulation.writeBytes(oldCumulation);
        oldCumulation.release();
        return cumulation;
    }
}
