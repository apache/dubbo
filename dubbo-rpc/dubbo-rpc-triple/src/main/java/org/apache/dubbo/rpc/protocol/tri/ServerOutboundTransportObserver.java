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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;

public class ServerOutboundTransportObserver extends OutboundTransportObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerOutboundTransportObserver.class);

    private final ChannelHandlerContext ctx;

    public ServerOutboundTransportObserver(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public void onMetadata(Http2Headers headers, boolean endStream) {
        checkSendMeta(headers, endStream);
        ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, endStream))
            .addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("send header error endStream=" + endStream, future.cause());
                }
            });
    }

    @Override
    public void onMetadata(Metadata metadata, boolean endStream) {
        doOnMetadata(metadata, endStream);
    }

    @Override
    public void onData(byte[] data, boolean endStream) {
        doOnData(data, endStream);
    }

    @Override
    protected void doOnMetadata(Metadata metadata, boolean endStream) {
        final DefaultHttp2Headers headers = new DefaultHttp2Headers(true);
        metadata.forEach(e -> headers.set(e.getKey(), e.getValue()));
        onMetadata(headers, endStream);
    }

    @Override
    protected void doOnData(byte[] data, boolean endStream) {
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(getCompressFlag());
        buf.writeInt(data.length);
        buf.writeBytes(data);
        onData(buf, endStream);
    }

    @Override
    protected void doOnError(GrpcStatus status) {
        ctx.writeAndFlush(new DefaultHttp2ResetFrame(Http2Error.CANCEL))
            .addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("write reset error", future.cause());
                }
            });
    }

    @Override
    protected void doOnComplete() {

    }

    public void onData(String str, boolean endStream) {
        ByteBuf buf = ByteBufUtil.writeUtf8(ctx.alloc(), str);
        onData(buf, endStream);
    }

    public void onData(ByteBuf buf, boolean endStream) {
        checkSendData(endStream);
        ctx.writeAndFlush(new DefaultHttp2DataFrame(buf, endStream))
            .addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("send data error endStream=" + endStream, future.cause());
                }
            });
    }

    private int getCompressFlag() {
        AbstractServerStream stream = ctx.channel().attr(TripleConstant.SERVER_STREAM_KEY).get();
        return calcCompressFlag(stream.getCompressor());
    }
}
