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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;

public class TextDataQueueCommand extends StreamQueueCommand {

    private final String data;

    private final boolean endStream;

    private TextDataQueueCommand(TripleStreamChannelFuture streamChannelFuture, String text, boolean endStream) {
        super(streamChannelFuture);
        this.data = text;
        this.endStream = endStream;
    }

    public static TextDataQueueCommand createCommand(TripleStreamChannelFuture streamChannelFuture, String data, boolean endStream) {
        return new TextDataQueueCommand(streamChannelFuture, data, endStream);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        ByteBuf buf = ByteBufUtil.writeUtf8(ctx.alloc(), data);
        ctx.write(new DefaultHttp2DataFrame(buf, endStream), promise);
    }
}
