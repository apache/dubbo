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

import org.apache.dubbo.rpc.protocol.tri.AbstractStream;
import org.apache.dubbo.rpc.protocol.tri.Compressor;
import org.apache.dubbo.rpc.protocol.tri.IdentityCompressor;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;

public class DataQueueCommand extends QueuedCommand.AbstractQueuedCommand {

    private final byte[] data;

    private final boolean endStream;

    private final boolean client;

    private DataQueueCommand(byte[] data, boolean endStream, boolean client) {
        this.data = data;
        this.endStream = endStream;
        this.client = client;
    }

    private DataQueueCommand(boolean endStream, boolean client) {
        this(null, endStream, client);
    }

    private DataQueueCommand(boolean endStream) {
        this(null, endStream, false);
    }

    public static DataQueueCommand createGrpcCommand(byte[] data, boolean endStream, boolean client) {
        return new DataQueueCommand(data, endStream, client);
    }

    public static DataQueueCommand createGrpcCommand(boolean endStream) {
        return new DataQueueCommand(endStream);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        if (data == null) {
            ctx.write(new DefaultHttp2DataFrame(endStream), promise);
        } else {
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeByte(getCompressFlag(ctx));
            buf.writeInt(data.length);
            buf.writeBytes(data);
            ctx.write(new DefaultHttp2DataFrame(buf, endStream), promise);
        }
    }

    private int getCompressFlag(ChannelHandlerContext ctx) {
        AbstractStream stream = client ? ctx.channel().attr(TripleConstant.CLIENT_STREAM_KEY).get() : ctx.channel().attr(TripleConstant.SERVER_STREAM_KEY).get();
        return calcCompressFlag(stream.getCompressor());
    }

    protected int calcCompressFlag(Compressor compressor) {
        if (null == compressor || IdentityCompressor.NONE.getMessageEncoding().equals(compressor.getMessageEncoding())) {
            return 0;
        }
        return 1;
    }

    // for test
    public byte[] getData() {
        return data;
    }

    // for test
    public boolean isEndStream() {
        return endStream;
    }

    // for test
    public boolean isClient() {
        return client;
    }
}
