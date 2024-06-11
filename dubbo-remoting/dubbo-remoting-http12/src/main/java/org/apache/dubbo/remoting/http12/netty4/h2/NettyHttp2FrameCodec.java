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

import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

public class NettyHttp2FrameCodec extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            Http2Header http2Header = onHttp2HeadersFrame(((Http2HeadersFrame) msg));
            super.channelRead(ctx, http2Header);
        } else if (msg instanceof Http2DataFrame) {
            Http2InputMessage http2Message = onHttp2DataFrame(((Http2DataFrame) msg));
            super.channelRead(ctx, http2Message);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Http2Header) {
            Http2Header http2Header = (Http2Header) msg;
            Http2HeadersFrame http2HeadersFrame = encodeHttp2HeadersFrame(http2Header);
            super.write(ctx, http2HeadersFrame, promise);
        } else if (msg instanceof Http2OutputMessage) {
            Http2OutputMessage http2OutputMessage = (Http2OutputMessage) msg;
            Http2DataFrame http2DataFrame = encodeHttp2DataFrame(ctx, http2OutputMessage);
            super.write(ctx, http2DataFrame, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    private Http2Header onHttp2HeadersFrame(Http2HeadersFrame headersFrame) {
        Http2Headers headers = headersFrame.headers();
        boolean endStream = headersFrame.isEndStream();
        HttpHeaders head = new HttpHeaders();
        for (Map.Entry<CharSequence, CharSequence> header : headers) {
            head.set(header.getKey().toString(), header.getValue().toString());
        }
        return new Http2MetadataFrame(headersFrame.stream().id(), head, endStream);
    }

    private Http2InputMessage onHttp2DataFrame(Http2DataFrame dataFrame) {
        ByteBuf content = dataFrame.content();
        Http2InputMessageFrame message =
                new Http2InputMessageFrame(new ByteBufInputStream(content, true), dataFrame.isEndStream());
        message.setId(dataFrame.stream().id());
        return message;
    }

    private Http2HeadersFrame encodeHttp2HeadersFrame(Http2Header http2Header) {
        HttpHeaders headers = http2Header.headers();
        DefaultHttp2Headers http2Headers = new DefaultHttp2Headers(false);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            List<String> value = entry.getValue();
            http2Headers.set(name, value);
        }
        return new DefaultHttp2HeadersFrame(http2Headers, http2Header.isEndStream());
    }

    private Http2DataFrame encodeHttp2DataFrame(ChannelHandlerContext ctx, Http2OutputMessage outputMessage)
            throws IOException {
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
