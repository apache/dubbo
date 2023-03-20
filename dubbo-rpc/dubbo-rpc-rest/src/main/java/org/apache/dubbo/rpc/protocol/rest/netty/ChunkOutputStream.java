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
package org.apache.dubbo.rpc.protocol.rest.netty;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;

import java.io.IOException;
import java.io.OutputStream;

public class ChunkOutputStream extends OutputStream {
    final ByteBuf buffer;
    final ChannelHandlerContext ctx;
    final NettyHttpResponse response;

    ChunkOutputStream(final NettyHttpResponse response, final ChannelHandlerContext ctx, final int chunksize) {
        this.response = response;
        if (chunksize < 1) {
            throw new IllegalArgumentException();
        }
        // TODO  buffer pool
        this.buffer = Unpooled.buffer(0, chunksize);
        this.ctx = ctx;
    }

    @Override
    public void write(int b) throws IOException {
        if (buffer.maxWritableBytes() < 1) {
            flush();
        }
        buffer.writeByte(b);
    }

    public void reset()
    {
        if (response.isCommitted()) throw new IllegalStateException();
        buffer.clear();
    }

    @Override
    public void close() throws IOException {
        flush();
        super.close();
    }


    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int dataLengthLeftToWrite = len;
        int dataToWriteOffset = off;
        int spaceLeftInCurrentChunk;
        while ((spaceLeftInCurrentChunk = buffer.maxWritableBytes()) < dataLengthLeftToWrite) {
            buffer.writeBytes(b, dataToWriteOffset, spaceLeftInCurrentChunk);
            dataToWriteOffset = dataToWriteOffset + spaceLeftInCurrentChunk;
            dataLengthLeftToWrite = dataLengthLeftToWrite - spaceLeftInCurrentChunk;
            flush();
        }
        if (dataLengthLeftToWrite > 0) {
            buffer.writeBytes(b, dataToWriteOffset, dataLengthLeftToWrite);
        }
    }

    @Override
    public void flush() throws IOException {
        int readable = buffer.readableBytes();
        if (readable == 0) return;
        if (!response.isCommitted()) response.prepareChunkStream();
        ctx.writeAndFlush(new DefaultHttpContent(buffer.copy()));
        buffer.clear();
        super.flush();
    }

}

