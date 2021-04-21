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
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;

public class ClientTransportObserver implements TransportObserver {
    private final ChannelHandlerContext ctx;
    private final AbstractClientStream stream;
    private boolean headerSent = false;
    private boolean endStreamSent = false;
    private Http2StreamChannel streamChannel;

    public ClientTransportObserver(ChannelHandlerContext ctx, AbstractClientStream stream) {
        this.ctx = ctx;
        this.stream = stream;

        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(ctx.channel());
        streamChannel = streamChannelBootstrap.open().syncUninterruptibly().getNow();

        final TripleHttp2ClientResponseHandler responseHandler = new TripleHttp2ClientResponseHandler();
        streamChannel.pipeline().addLast(responseHandler)
            .addLast(new GrpcDataDecoder(Integer.MAX_VALUE))
            .addLast(new TripleClientInboundHandler());
        streamChannel.attr(TripleUtil.CLIENT_STREAM_KEY).set(stream);
    }

    @Override
    public void onMetadata(Metadata metadata, boolean endStream, Stream.OperationHandler handler) {
        final DefaultHttp2Headers headers = new DefaultHttp2Headers(true);
        metadata.forEach(e -> {
            headers.set(e.getKey(), e.getValue());
        });
        if (!headerSent) {
            headerSent = true;
            streamChannel.writeAndFlush(new DefaultHttp2HeadersFrame(headers, endStream));
        }
    }

    @Override
    public void onData(byte[] data, boolean endStream, Stream.OperationHandler handler) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(0);
        buf.writeInt(data.length);
        buf.writeBytes(data);
        streamChannel.writeAndFlush(new DefaultHttp2DataFrame(buf, endStream));
    }

    @Override
    public void onComplete(Stream.OperationHandler handler) {
        if (!endStreamSent) {
            endStreamSent = true;
            streamChannel.writeAndFlush(new DefaultHttp2DataFrame(true));
        }
    }
}
