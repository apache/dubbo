/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.rpc.protocol.tri.frame;
import org.apache.dubbo.rpc.protocol.tri.TripleFlowControlFrame;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import io.netty.handler.codec.http2.DefaultHttp2WindowUpdateFrame;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

public class TriDecoder implements Deframer {

    private static final int HEADER_LENGTH = 5;
    private static final int COMPRESSED_FLAG_MASK = 1;
    private static final int RESERVED_MASK = 0xFE;
    private final CompositeByteBuf accumulate = Unpooled.compositeBuffer();
    private final Listener listener;
    private final DeCompressor decompressor;
    private boolean compressedFlag;
    private long pendingDeliveries;
    private boolean inDelivery = false;
    private boolean closing;
    private boolean closed;
    private Http2Connection connection;
    private int flowControlledBytes;
    private Http2FrameStream stream;

    private int requiredLength = HEADER_LENGTH;

    private GrpcDecodeState state = GrpcDecodeState.HEADER;

    public TriDecoder(DeCompressor decompressor, Listener listener,Http2Connection connection) {
        this.decompressor = decompressor;
        this.listener = listener;
        this.connection = connection;
    }

    @Override
    public void deframe(Http2DataFrame msg) {
        if (closing || closed) {
            // ignored
            return;
        }
        final int bytes = msg.initialFlowControlledBytes();

        // It is important that we increment the flowControlledBytes before we call fireChannelRead(...)
        // as it may cause a read() that will call updateLocalWindowIfNeeded() and we need to ensure
        // in this case that we accounted for it.
        //
        // See https://github.com/netty/netty/issues/9663
        flowControlledBytes += bytes;
        stream = msg.stream();
        //  new DefaultHttp2WindowUpdateFrame(bytes).stream(stream);
        accumulate.addComponent(true, msg.content());
        deliver();
    }

    public void request(int numMessages) {
        pendingDeliveries += numMessages;
        deliver();
    }

    @Override
    public void close() {
        closing = true;
        deliver();
    }

    private void deliver() {
        // We can have reentrancy here when using a direct executor, triggered by calls to
        // request more messages. This is safe as we simply loop until pendingDelivers = 0
        if (inDelivery) {
            return;
        }
        inDelivery = true;
        try {
            // Process the uncompressed bytes.
            while (pendingDeliveries > 0 && hasEnoughBytes()) {
                switch (state) {
                    case HEADER:
                        processHeader();
                        break;
                    case PAYLOAD:
                        // Read the body and deliver the message.
                        processBody();

                        // Since we've delivered a message, decrement the number of pending
                        // deliveries remaining.
                        pendingDeliveries--;
                        break;
                    default:
                        throw new AssertionError("Invalid state: " + state);
                }
            }
            if (closing) {
                if (!closed) {
                    closed = true;
                    accumulate.clear();
                    accumulate.release();
                    listener.close();
                    stream=null;
                }
            }
        } finally {
            inDelivery = false;
        }
    }

    private boolean hasEnoughBytes() {
        return requiredLength - accumulate.readableBytes() <= 0;
    }

    /**
     * Processes the GRPC compression header which is composed of the compression flag and the outer
     * frame length.
     */
    private void processHeader() {
        int type = accumulate.readUnsignedByte();
        if ((type & RESERVED_MASK) != 0) {
            throw new RpcException("gRPC frame header malformed: reserved bits not zero");
        }
        compressedFlag = (type & COMPRESSED_FLAG_MASK) != 0;

        requiredLength = accumulate.readInt();

        // Continue reading the frame body.
        state = GrpcDecodeState.PAYLOAD;
    }

    /**
     * Processes the GRPC message body, which depending on frame header flags may be compressed.
     */
    private void processBody() {
        // There is no reliable way to get the uncompressed size per message when it's compressed,
        // because the uncompressed bytes are provided through an InputStream whose total size is
        // unknown until all bytes are read, and we don't know when it happens.
        byte[] streams = compressedFlag ? getCompressedBody() : getUncompressedBody();
        int bytes = flowControlledBytes;
        flowControlledBytes = 0;
        TripleFlowControlFrame tripleFlowControlFrame = new TripleFlowControlFrame(connection,0,new DefaultHttp2WindowUpdateFrame(bytes).stream(stream),streams);
        listener.onRawMessage(tripleFlowControlFrame);

        // Done with this frame, begin processing the next header.
        state = GrpcDecodeState.HEADER;
        requiredLength = HEADER_LENGTH;
    }

    private byte[] getCompressedBody() {
        final byte[] compressedBody = getUncompressedBody();
        return decompressor.decompress(compressedBody);
    }

    private byte[] getUncompressedBody() {
        byte[] data = new byte[requiredLength];
        accumulate.readBytes(data);
        return data;
    }


    private enum GrpcDecodeState {
        HEADER,
        PAYLOAD
    }

    public interface Listener {

        void onRawMessage(TripleFlowControlFrame data);

        void close();

    }

}
