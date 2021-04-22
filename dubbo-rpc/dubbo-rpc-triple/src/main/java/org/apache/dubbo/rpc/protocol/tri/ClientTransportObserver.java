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
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.AsciiString;

public class ClientTransportObserver implements TransportObserver {
    private static final AsciiString SCHEME = AsciiString.of("http");
    private final ChannelHandlerContext ctx;
    private final Http2StreamChannel streamChannel;
    private final ChannelPromise promise;
    private boolean headerSent = false;
    private boolean endStreamSent = false;


    public ClientTransportObserver(ChannelHandlerContext ctx, AbstractClientStream stream, ChannelPromise promise) {
        this.ctx = ctx;
        this.promise = promise;

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
        if (!headerSent) {
            final Http2Headers headers = new DefaultHttp2Headers(true)
                    .path(metadata.get(TripleConstant.PATH_KEY))
                    .authority(metadata.get(TripleConstant.AUTHORITY_KEY))
                    .scheme(SCHEME)
                    .method(HttpMethod.POST.asciiName());
            metadata.forEach(e -> headers.set(e.getKey(), e.getValue()));
            headerSent = true;
            streamChannel.writeAndFlush(new DefaultHttp2HeadersFrame(headers, endStream))
                    .addListener(future -> {
                        if (!future.isSuccess()) {
                            promise.tryFailure(future.cause());
                        }
                    });
        }
    }

    @Override
    public void onData(byte[] data, boolean endStream, Stream.OperationHandler handler) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(0);
        buf.writeInt(data.length);
        buf.writeBytes(data);
        streamChannel.writeAndFlush(new DefaultHttp2DataFrame(buf, endStream))
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        promise.tryFailure(future.cause());
                    }
                });
    }

    @Override
    public void onComplete(Stream.OperationHandler handler) {
        if (!endStreamSent) {
            endStreamSent = true;
            streamChannel.writeAndFlush(new DefaultHttp2DataFrame(true))
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            promise.trySuccess();
                        } else {
                            promise.tryFailure(future.cause());
                        }
                    });
        }
    }
}
