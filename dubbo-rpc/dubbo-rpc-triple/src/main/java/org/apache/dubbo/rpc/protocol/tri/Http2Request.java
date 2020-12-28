package org.apache.dubbo.rpc.protocol.tri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;

public class Http2Request {
    private final int streamId;
    private final String path;
    private volatile Http2Headers headers;
    private CompositeByteBuf pending;
    private Http2Stream http2Stream;
    private Http2Connection.PropertyKey streamKey;
    private int bytesToRead;
    private final ByteBufAllocator alloc;

    public Http2Request(int streamId, String path, Http2Stream http2Stream, Http2Headers headers,
                        Http2Connection.PropertyKey streamKey, ByteBufAllocator allocator) {
        this.streamId = streamId;
        this.path = path;
        this.http2Stream = http2Stream;
        this.headers = headers;
        this.alloc=allocator;
        this.streamKey = streamKey;
        this.pending = allocator.compositeBuffer();
    }

    public Http2Headers getHeaders() {
        return headers;
    }

    public ByteBuf getData() {
        return pending;
    }

    public int getStreamId() {
        return streamId;
    }

    public void appendData(ByteBuf data) {
        pending.addComponent(true, data);
    }

    public ByteBuf getAvailableTrunk(){

        int type = pending.readUnsignedByte();
//        if ((type & RESERVED_MASK) != 0) {
//            throw Status.INTERNAL.withDescription(
//                    "gRPC frame header malformed: reserved bits not zero")
//                    .asRuntimeException();
//        }
//        compressedFlag = (type & COMPRESSED_FLAG_MASK) != 0;

        // Update the required length to include the length of the frame.
        this.bytesToRead = pending.readInt();
/*        if (requiredLength < 0 || requiredLength > maxInboundMessageSize) {
            throw Status.RESOURCE_EXHAUSTED.withDescription(
                    String.format("gRPC message exceeds maximum size %d: %d",
                            maxInboundMessageSize, requiredLength))
                    .asRuntimeException();
        }*/
        return tryRead();

    }
    private ByteBuf tryRead(){
        if(bytesToRead>0&&pending.readableBytes()>=bytesToRead){
            final ByteBuf ready = alloc.buffer();
            pending.readBytes(ready,bytesToRead);
            return ready;
        }
        return null;
    }
}
