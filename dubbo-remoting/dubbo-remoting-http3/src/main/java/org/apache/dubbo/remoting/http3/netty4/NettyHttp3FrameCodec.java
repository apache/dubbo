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
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.message.DefaultHttpHeaders;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpHeaders;

import java.io.OutputStream;
import java.net.SocketAddress;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Headers.PseudoHeaderName;
import io.netty.handler.codec.http3.DefaultHttp3DataFrame;
import io.netty.handler.codec.http3.DefaultHttp3Headers;
import io.netty.handler.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.handler.codec.http3.Http3DataFrame;
import io.netty.handler.codec.http3.Http3Headers;
import io.netty.handler.codec.http3.Http3HeadersFrame;
import io.netty.handler.codec.http3.Http3RequestStreamInboundHandler;
import io.netty.handler.codec.quic.QuicStreamChannel;

import static org.apache.dubbo.remoting.http3.netty4.Constants.TRI_PING;

@Sharable
public class NettyHttp3FrameCodec extends Http3RequestStreamInboundHandler implements ChannelOutboundHandler {

    public static final NettyHttp3FrameCodec INSTANCE = new NettyHttp3FrameCodec();

    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        Http3Headers headers = frame.headers();
        if (headers.contains(TRI_PING)) {
            pingReceived(ctx);
            return;
        }

        ctx.fireChannelRead(new Http2MetadataFrame(getStreamId(ctx), new DefaultHttpHeaders(headers), false));
    }

    private void pingReceived(ChannelHandlerContext ctx) {
        Http3Headers pongHeader = new DefaultHttp3Headers(false);
        pongHeader.set(TRI_PING, "0");
        pongHeader.set(PseudoHeaderName.STATUS.value(), HttpStatus.OK.getStatusString());
        ChannelFuture future = ctx.write(new DefaultHttp3HeadersFrame(pongHeader), ctx.newPromise());
        if (future.isDone()) {
            ctx.close();
        } else {
            future.addListener((ChannelFutureListener) f -> ctx.close());
        }
    }

    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame) {
        ctx.fireChannelRead(
                new Http2InputMessageFrame(getStreamId(ctx), new ByteBufInputStream(frame.content(), true), false));
    }

    private static long getStreamId(ChannelHandlerContext ctx) {
        return ((QuicStreamChannel) ctx.channel()).streamId();
    }

    @Override
    protected void channelInputClosed(ChannelHandlerContext ctx) {
        ctx.fireChannelRead(new Http2InputMessageFrame(getStreamId(ctx), StreamUtils.EMPTY, true));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Http2Header) {
            Http2Header headers = (Http2Header) msg;
            if (headers.isEndStream()) {
                ChannelFuture future = ctx.write(
                        new DefaultHttp3HeadersFrame(((NettyHttpHeaders<Http3Headers>) headers.headers()).getHeaders()),
                        ctx.newPromise());
                if (future.isDone()) {
                    ctx.close(promise);
                } else {
                    future.addListener((ChannelFutureListener) f -> ctx.close(promise));
                }
                return;
            }
            ctx.write(
                    new DefaultHttp3HeadersFrame(((NettyHttpHeaders<Http3Headers>) headers.headers()).getHeaders()),
                    promise);
        } else if (msg instanceof Http2OutputMessage) {
            Http2OutputMessage message = (Http2OutputMessage) msg;
            OutputStream body = message.getBody();
            assert body instanceof ByteBufOutputStream || body == null;
            if (message.isEndStream()) {
                if (body == null) {
                    ctx.close(promise);
                    return;
                }
                ChannelFuture future =
                        ctx.write(new DefaultHttp3DataFrame(((ByteBufOutputStream) body).buffer()), ctx.newPromise());
                if (future.isDone()) {
                    ctx.close(promise);
                } else {
                    future.addListener((ChannelFutureListener) f -> ctx.close(promise));
                }
                return;
            }
            if (body == null) {
                promise.trySuccess();
                return;
            }
            ctx.write(new DefaultHttp3DataFrame(((ByteBufOutputStream) body).buffer()), promise);
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
