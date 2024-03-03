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
package org.apache.dubbo.rpc.protocol.tri.h3;

import org.apache.dubbo.remoting.http3.netty4.Http2HeadersAdapter;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;

import java.util.Map;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3ErrorCode;
import io.netty.incubator.codec.http3.Http3Exception;
import io.netty.incubator.codec.http3.Http3GoAwayFrame;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.quic.QuicStreamChannel;

@Sharable
public class Http3ClientFrameCodec extends ChannelDuplexHandler {

    public static final Http3ClientFrameCodec INSTANCE = new Http3ClientFrameCodec();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http2HeadersAdapter headers = new Http2HeadersAdapter(((Http3HeadersFrame) msg).headers());
            boolean endStream = headers.contains(TripleHeaderEnum.STATUS_KEY.getHeader());
            ctx.fireChannelRead(new DefaultHttp2HeadersFrame(headers, endStream));
        } else if (msg instanceof Http3DataFrame) {
            ctx.fireChannelRead(new DefaultHttp2DataFrame(((Http3DataFrame) msg).content()));
        } else if (msg instanceof Http3GoAwayFrame) {
            ctx.fireUserEventTriggered(new DefaultHttp2GoAwayFrame(((Http3GoAwayFrame) msg).id()));
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.fireChannelRead(new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER, true));
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            Http2HeadersFrame frame = (Http2HeadersFrame) msg;
            Http3Headers headers = new DefaultHttp3Headers();
            for (Map.Entry<CharSequence, CharSequence> header : frame.headers()) {
                headers.set(header.getKey(), header.getValue());
            }
            ctx.write(new DefaultHttp3HeadersFrame(headers), promise);
            if (frame.isEndStream()) {
                ((QuicStreamChannel) ctx.channel()).shutdownOutput(promise);
            }
        } else if (msg instanceof Http2DataFrame) {
            Http2DataFrame frame = (Http2DataFrame) msg;
            if (frame.isEndStream()) {
                ((QuicStreamChannel) ctx.channel()).shutdownOutput(promise);
                return;
            }
            ctx.write(new DefaultHttp3DataFrame(frame.content()), promise);
        } else {
            ctx.write(msg, promise);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof Http3Exception) {
            Http3Exception e = (Http3Exception) cause;
            Http3ErrorCode errorCode = e.errorCode();
            if (errorCode == Http3ErrorCode.H3_CLOSED_CRITICAL_STREAM) {
                ctx.fireUserEventTriggered(new DefaultHttp2ResetFrame(256 + errorCode.ordinal()));
                return;
            }
        }
        super.exceptionCaught(ctx, cause);
    }
}
