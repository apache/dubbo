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
package org.apache.dubbo.remoting.http3.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3HeadersFrame;

import io.netty.incubator.codec.quic.QuicStreamChannel;

import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http3.h3.Http3InputMessageFrame;
import org.apache.dubbo.remoting.http3.h3.Http3MetadataFrame;

import java.util.Arrays;
import java.util.Map;

public class NettyHttp3FrameCodec extends ChannelInboundHandlerAdapter {

    @Override
    public final void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        QuicStreamChannel ch = (QuicStreamChannel) ctx.channel();
        if (msg instanceof Http3HeadersFrame) {
            Http2Header header = onHttp3HeadersFrame(ch, (Http3HeadersFrame) msg);
            super.channelRead(ctx, header);
        } else if (msg instanceof Http3DataFrame) {
            Http2InputMessage inputMessage = onHttp3DataFrame(ch, (Http3DataFrame) msg);
            super.channelRead(ctx, inputMessage);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private Http2Header onHttp3HeadersFrame(QuicStreamChannel ch, Http3HeadersFrame frame) {
        Http3Headers headers = frame.headers();
        HttpHeaders head = new HttpHeaders();
        for (Map.Entry<CharSequence, CharSequence> header: headers) {
            head.set(header.getKey().toString(), header.getValue().toString());
        }
        return new Http3MetadataFrame(head, ch.streamId());
    }

    private Http2InputMessage onHttp3DataFrame(QuicStreamChannel ch, Http3DataFrame frame) {
        ByteBuf content = frame.content();
        return new Http3InputMessageFrame(new ByteBufInputStream(content, true), ch.streamId());
    }
}
