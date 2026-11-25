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
package org.apache.dubbo.rpc.protocol.tri.command;

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;

public class DataQueueCommand extends StreamQueueCommand {

    private final InputStream dataStream;

    private final Compressor compressor;

    private final boolean endStream;

    private DataQueueCommand(
            TripleStreamChannelFuture streamChannelFuture,
            InputStream dataStream,
            Compressor compressor,
            boolean endStream) {
        super(streamChannelFuture);
        this.dataStream = dataStream;
        this.compressor = compressor;
        this.endStream = endStream;
    }

    public static DataQueueCommand create(
            TripleStreamChannelFuture streamChannelFuture,
            InputStream dataStream,
            boolean endStream,
            Compressor compressor) {
        return new DataQueueCommand(streamChannelFuture, dataStream, compressor, endStream);
    }

    /**
     * Send data frame to the channel.
     *
     * <p>gRPC message frame format:
     * <pre>
     * +----------------------+
     * | Compressed-Flag (1B) |  0 = uncompressed, 1 = compressed
     * +----------------------+
     * | Message-Length  (4B) |  big-endian unsigned integer
     * +----------------------+
     * | Message Data    (N)  |  compressed or uncompressed payload
     * +----------------------+
     * </pre>
     */
    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        if (dataStream == null) {
            ctx.write(new DefaultHttp2DataFrame(endStream), promise);
        } else {
            ByteBuf buf = ctx.alloc().buffer();
            // Write compression flag (1 byte): 0 for identity, 1 for compressed
            int compressFlag = Identity.MESSAGE_ENCODING.equals(compressor.getMessageEncoding()) ? 0 : 1;
            buf.writeByte(compressFlag);
            // Record position for length field, write placeholder (4 bytes)
            int lengthIndex = buf.writerIndex();
            buf.writeInt(0);
            try {
                // Compress and write data directly into ByteBuf using decorator pattern
                // Use try-with-resources to ensure proper resource cleanup
                try (ByteBufOutputStream bbos = new ByteBufOutputStream(buf);
                        OutputStream compressedOut = compressor.decorate(bbos)) {
                    StreamUtils.copy(dataStream, compressedOut);
                }
                // Calculate actual message length: total written bytes minus the 4-byte length field itself
                int written = buf.writerIndex() - lengthIndex - 4;
                buf.setInt(lengthIndex, written);
            } catch (Exception e) {
                buf.release();
                promise.setFailure(e);
                return;
            } finally {
                // Always close the dataStream to prevent resource leaks
                closeQuietly(dataStream);
            }
            ctx.write(new DefaultHttp2DataFrame(buf, endStream), promise);
        }
    }

    private static void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
                // Ignore close exception
            }
        }
    }

    // for test
    public InputStream getDataStream() {
        return dataStream;
    }

    // for test
    public boolean isEndStream() {
        return endStream;
    }
}
