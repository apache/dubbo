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

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;

import java.io.OutputStream;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;
import io.netty.incubator.codec.quic.QuicStreamChannel;

@Sharable
public class NettyHttp3FrameCodec extends Http3RequestStreamInboundHandler implements ChannelOutboundHandler {

    public static final NettyHttp3FrameCodec INSTANCE = new NettyHttp3FrameCodec();

    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        HttpHeaders headers = new HttpHeaders();
        for (Map.Entry<CharSequence, CharSequence> header : frame.headers()) {
            headers.set(header.getKey().toString(), header.getValue().toString());
        }
        ctx.fireChannelRead(new Http2MetadataFrame(getStreamId(ctx), headers, false));
    }

    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame) {
        Http2InputMessageFrame msg = new Http2InputMessageFrame(new ByteBufInputStream(frame.content(), true));
        msg.setId(getStreamId(ctx));
        ctx.fireChannelRead(msg);
    }

    private static long getStreamId(ChannelHandlerContext ctx) {
        return ((QuicStreamChannel) ctx.channel()).streamId();
    }

    @Override
    protected void channelInputClosed(ChannelHandlerContext ctx) {
        Http2InputMessageFrame msg = new Http2InputMessageFrame(StreamUtils.EMPTY, true);
        msg.setId(getStreamId(ctx));
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Http2Header) {
            Http3Headers headers = new DefaultHttp3Headers();
            Http2Header http2Header = (Http2Header) msg;
            for (Entry<String, List<String>> entry : http2Header.headers().entrySet()) {
                headers.add(entry.getKey(), entry.getValue());
            }
            ctx.write(new DefaultHttp3HeadersFrame(headers), promise);
            if (http2Header.isEndStream()) {
                ctx.close();
            }
        } else if (msg instanceof Http2OutputMessage) {
            Http2OutputMessage outputMessage = (Http2OutputMessage) msg;
            try {
                OutputStream body = outputMessage.getBody();
                if (body == null) {
                    Http3DataFrame frame = new DefaultHttp3DataFrame(Unpooled.EMPTY_BUFFER);
                    ctx.write(frame, promise);
                    return;
                }
                if (body instanceof ByteBufOutputStream) {
                    Http3DataFrame frame = new DefaultHttp3DataFrame(((ByteBufOutputStream) body).buffer());
                    ctx.write(frame, promise);
                    return;
                }
            } finally {
                if (outputMessage.isEndStream()) {
                    ctx.close();
                }
            }
            throw new IllegalArgumentException("Http2OutputMessage body must be ByteBufOutputStream");
        } else {
            ctx.write(msg, promise);
        }
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    @Override
    public void connect(
            ChannelHandlerContext ctx,
            SocketAddress remoteAddress,
            SocketAddress localAddress,
            ChannelPromise promise) {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.disconnect(promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.deregister(promise);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void flush(ChannelHandlerContext ctx) {
        ctx.flush();
    }
}
