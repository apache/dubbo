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

package org.apache.dubbo.rpc.protocol.tri;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class ServerTransportObserver implements TransportObserver {
    private final ChannelHandlerContext ctx;
    private boolean headerSent = false;

    public ServerTransportObserver(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onMetadata(Metadata metadata, boolean endStream, Stream.OperationHandler handler) {
        final DefaultHttp2Headers headers = new DefaultHttp2Headers(true);
        metadata.forEach(e -> {
            headers.set(e.getKey(), e.getValue());
        });
        if (!headerSent) {
            headerSent = true;
            headers.status(OK.codeAsText());
            headers.set(TripleConstant.CONTENT_TYPE_KEY, TripleConstant.CONTENT_PROTO);
            ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, endStream));
        } else {
            ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, endStream));
        }
    }

    @Override
    public void onData(byte[] data, boolean endStream, Stream.OperationHandler handler) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(0);
        buf.writeInt(data.length);
        buf.writeBytes(data);
        ctx.writeAndFlush(new DefaultHttp2DataFrame(buf, false));
    }

    @Override
    public void onComplete(Stream.OperationHandler handler) {
    }
}
