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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;

public class DataQueueCommand extends StreamQueueCommand {

    private final byte[] data;

    private final int compressFlag;

    private final boolean endStream;

    private DataQueueCommand(TripleStreamChannelFuture streamChannelFuture, byte[] data, int compressFlag, boolean endStream) {
        super(streamChannelFuture);
        this.data = data;
        this.compressFlag = compressFlag;
        this.endStream = endStream;
    }

    public static DataQueueCommand create(TripleStreamChannelFuture streamChannelFuture, byte[] data, boolean endStream,
                                          int compressFlag) {
        return new DataQueueCommand(streamChannelFuture, data, compressFlag, endStream);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        if (data == null) {
            ctx.write(new DefaultHttp2DataFrame(endStream), promise);
        } else {
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeByte(compressFlag);
            buf.writeInt(data.length);
            buf.writeBytes(data);
            ctx.write(new DefaultHttp2DataFrame(buf, endStream), promise);
        }
    }


    // for test
    public byte[] getData() {
        return data;
    }

    // for test
    public boolean isEndStream() {
        return endStream;
    }

}
