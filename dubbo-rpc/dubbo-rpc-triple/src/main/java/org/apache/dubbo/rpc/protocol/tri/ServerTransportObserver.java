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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2Error;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class ServerTransportObserver implements TransportObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerTransportObserver.class);

    private final ChannelHandlerContext ctx;
    private boolean headerSent = false;
    private boolean resetSent = false;

    public ServerTransportObserver(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onMetadata(Metadata metadata, boolean endStream) {
        if (resetSent) {
            return;
        }
        final DefaultHttp2Headers headers = new DefaultHttp2Headers(true);
        metadata.forEach(e -> {
            headers.set(e.getKey(), e.getValue());
        });
        if (!headerSent) {
            headerSent = true;
            headers.status(OK.codeAsText());
            headers.set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.CONTENT_PROTO);
        }
        // If endStream is true, the channel will be closed, so you cannot listen for errors and continue sending any frame
        ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, endStream))
            .addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("send header error endStream=" + endStream, future.cause());
                }
            });
    }

    @Override
    public void onReset(Http2Error http2Error) {
        resetSent = true;
        ctx.writeAndFlush(new DefaultHttp2ResetFrame(http2Error))
            .addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("write reset error", future.cause());
                }
            });
    }

    @Override
    public void onData(byte[] data, boolean endStream) {
        if (resetSent) {
            return;
        }
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(getCompressFlag());
        buf.writeInt(data.length);
        buf.writeBytes(data);
        ctx.writeAndFlush(new DefaultHttp2DataFrame(buf, false))
            .addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("send data error endStream=" + endStream, future.cause());
                }
            });
    }


    private int getCompressFlag() {
        AbstractServerStream stream = ctx.channel().attr(TripleConstant.SERVER_STREAM_KEY).get();
        return TransportObserver.calcCompressFlag(stream.getCompressor());
    }
}
