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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import org.apache.dubbo.remoting.buffer.ChannelWritableBuffer;
import org.apache.dubbo.remoting.buffer.WritableBuffer;
import org.apache.dubbo.remoting.buffer.WritableBufferAllocator;
import org.apache.dubbo.rpc.protocol.tri.command.FrameQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

public class MessageFramer implements Framer {
    private static final int HEADER_LENGTH = 5;

    private final Channel sink;
    private final TripleWriteQueue writeQueue;
    private final WritableBufferAllocator bufferAllocator;
    private WritableBuffer buffer;
    private Compressor compressor = Compressor.NONE;

    private boolean closed;

    public MessageFramer(Channel sink, TripleWriteQueue writeQueue, WritableBufferAllocator bufferAllocator) {
        this.sink = sink;
        this.writeQueue = writeQueue;
        this.bufferAllocator = bufferAllocator;
    }

    @Override
    public Compressor getCompressor() {
        return compressor;
    }

    @Override
    public void setCompressor(Compressor compressor) {
        this.compressor = compressor;
    }

    @Override
    public void writePayload(byte[] cmd) {
        int compressed =
            Identity.MESSAGE_ENCODING.equals(compressor.getMessageEncoding())
                ? 0 : 1;
        final byte[] compress = compressor.compress(cmd);
        WritableBuffer allocate = bufferAllocator.allocate(HEADER_LENGTH + compress.length);
        allocate.write((byte) compressed);
        allocate.writeInt(compress.length);
        allocate.write(compress, 0, compress.length);
        this.buffer = allocate;
    }

    @Override
    public ChannelFuture close() {
        if(!closed) {
            closed = true;
            return commitToSink(true, true);
        }
        return null;
    }

    @Override
    public void flush() {
        if (buffer != null && buffer.readableBytes() > 0) {
            commitToSink(false, true);
        }
    }

    private ChannelFuture commitToSink(boolean endOfStream, boolean flush) {
        WritableBuffer buf = buffer;
        buffer = null;
        ByteBuf bytebuf = buf == null ? Unpooled.EMPTY_BUFFER : ((ChannelWritableBuffer)buf).bytebuf().touch();
        FrameQueueCommand grpcCommand = FrameQueueCommand.createGrpcCommand(new DefaultHttp2DataFrame(bytebuf, endOfStream)).channel(sink);
        writeQueue.enqueueSoon(grpcCommand, flush);
        return grpcCommand.promise();
    }
}
