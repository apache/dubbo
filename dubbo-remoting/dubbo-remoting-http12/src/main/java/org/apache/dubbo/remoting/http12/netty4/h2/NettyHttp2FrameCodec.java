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
package org.apache.dubbo.remoting.http12.netty4.h2;

import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.message.DefaultHttpHeaders;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpHeaders;

import java.io.OutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

public class NettyHttp2FrameCodec extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            super.channelRead(ctx, onHttp2HeadersFrame(((Http2HeadersFrame) msg)));
        } else if (msg instanceof Http2DataFrame) {
            super.channelRead(ctx, onHttp2DataFrame(((Http2DataFrame) msg)));
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Http2Header) {
            super.write(ctx, encodeHttp2HeadersFrame((Http2Header) msg), promise);
        } else if (msg instanceof Http2OutputMessage) {
            super.write(ctx, encodeHttp2DataFrame((Http2OutputMessage) msg), promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    private Http2Header onHttp2HeadersFrame(Http2HeadersFrame headersFrame) {
        return new Http2MetadataFrame(
                headersFrame.stream().id(), new DefaultHttpHeaders(headersFrame.headers()), headersFrame.isEndStream());
    }

    private Http2InputMessage onHttp2DataFrame(Http2DataFrame dataFrame) {
        return new Http2InputMessageFrame(
                dataFrame.stream().id(), new ByteBufInputStream(dataFrame.content(), true), dataFrame.isEndStream());
    }

    @SuppressWarnings("unchecked")
    private Http2HeadersFrame encodeHttp2HeadersFrame(Http2Header http2Header) {
        return new DefaultHttp2HeadersFrame(
                ((NettyHttpHeaders<Http2Headers>) http2Header.headers()).getHeaders(), http2Header.isEndStream());
    }

    private Http2DataFrame encodeHttp2DataFrame(Http2OutputMessage outputMessage) {
        OutputStream body = outputMessage.getBody();
        if (body == null) {
            return new DefaultHttp2DataFrame(outputMessage.isEndStream());
        }
        if (body instanceof ByteBufOutputStream) {
            ByteBuf buffer = ((ByteBufOutputStream) body).buffer();
            return new DefaultHttp2DataFrame(buffer, outputMessage.isEndStream());
        }
        throw new IllegalArgumentException("Http2OutputMessage body must be ByteBufOutputStream");
    }
}
