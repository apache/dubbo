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
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;

public class ClientTransportObserver implements TransportObserver {
    private final ChannelHandlerContext ctx;
    private final ChannelPromise promise;
    private volatile Http2StreamChannel streamChannel;

    public void setStreamChannel(Http2StreamChannel streamChannel) {
        this.streamChannel = streamChannel;
    }

    public ClientTransportObserver(ChannelHandlerContext ctx, ChannelPromise promise) {
        this.ctx = ctx;
        this.promise = promise;
    }

    @Override
    public void onMetadata(Metadata metadata, boolean endStream) {
        while (streamChannel == null) {
            // wait channel initialized
        }
        final Http2Headers headers = new DefaultHttp2Headers(true);
        metadata.forEach(e -> headers.set(e.getKey(), e.getValue()));
        streamChannel.writeAndFlush(new DefaultHttp2HeadersFrame(headers, endStream))
            .addListener(future -> {
                if (!future.isSuccess()) {
                    promise.tryFailure(future.cause());
                }
            });

    }

    @Override
    public void onReset(Http2Error http2Error) {
        streamChannel.writeAndFlush(new DefaultHttp2ResetFrame(http2Error))
            .addListener(future -> {
                if (future.isSuccess()) {
                    promise.trySuccess();
                } else {
                    promise.tryFailure(future.cause());
                }
            });
    }

    @Override
    public void onData(byte[] data, boolean endStream) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(getCompressFlag());
        buf.writeInt(data.length);
        buf.writeBytes(data);
        streamChannel.writeAndFlush(new DefaultHttp2DataFrame(buf, endStream))
            .addListener(future -> {
                if (future.isSuccess()) {
                    promise.trySuccess();
                } else {
                    promise.tryFailure(future.cause());
                }
            });
    }

    @Override
    public void onComplete() {
        streamChannel.writeAndFlush(new DefaultHttp2DataFrame(true))
            .addListener(future -> {
                if (future.isSuccess()) {
                    promise.trySuccess();
                } else {
                    promise.tryFailure(future.cause());
                }
            });
    }

    private int getCompressFlag() {
        AbstractClientStream stream = streamChannel.attr(TripleConstant.CLIENT_STREAM_KEY).get();
        return TransportObserver.calcCompressFlag(stream.getCompressor());
    }

}
