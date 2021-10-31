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
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;

/**
 * Send stream data to remote
 * {@link ClientOutboundTransportObserver#promise} will be set success after rst or complete sent,
 */
public class ClientOutboundTransportObserver extends OutboundTransportObserver {

    private final ChannelPromise promise;
    private final Http2StreamChannel streamChannel;

    public ClientOutboundTransportObserver(Http2StreamChannel channel, ChannelPromise promise) {
        this.streamChannel = channel;
        this.promise = promise;
    }

    @Override
    protected void doOnMetadata(Metadata metadata, boolean endStream) {
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
    protected void doOnData(byte[] data, boolean endStream) {
        ByteBuf buf = streamChannel.alloc().buffer();
        buf.writeByte(getCompressFlag());
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
    protected void doOnError(GrpcStatus status) {
        streamChannel.writeAndFlush(new DefaultHttp2ResetFrame(Http2Error.CANCEL))
            .addListener(future -> {
                if (future.isSuccess()) {
                    promise.trySuccess();
                } else {
                    promise.tryFailure(future.cause());
                }
            });
    }

    @Override
    protected void doOnComplete() {
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
        return calcCompressFlag(stream.getCompressor());
    }
}
