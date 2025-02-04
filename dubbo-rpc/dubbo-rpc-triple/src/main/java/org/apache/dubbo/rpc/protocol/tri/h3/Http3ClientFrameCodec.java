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

import org.apache.dubbo.common.logger.FluentLogger;
import org.apache.dubbo.remoting.http12.HttpConstants;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http3.netty4.Constants;
import org.apache.dubbo.remoting.http3.netty4.Http2HeadersAdapter;
import org.apache.dubbo.remoting.http3.netty4.Http3HeadersAdapter;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers.PseudoHeaderName;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3ErrorCode;
import io.netty.incubator.codec.http3.Http3Exception;
import io.netty.incubator.codec.http3.Http3GoAwayFrame;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInitializer;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_RECONNECT;

@Sharable
public class Http3ClientFrameCodec extends ChannelDuplexHandler {

    private static final FluentLogger LOGGER = FluentLogger.of(Http3ClientFrameCodec.class);
    public static final Http3ClientFrameCodec INSTANCE = new Http3ClientFrameCodec();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http3Headers headers = ((Http3HeadersFrame) msg).headers();
            if (headers.contains(Constants.TRI_PING)) {
                pingAck(ctx);
            } else {
                boolean endStream = headers.contains(TripleHeaderEnum.STATUS_KEY.getKey());
                ctx.fireChannelRead(new DefaultHttp2HeadersFrame(new Http2HeadersAdapter(headers), endStream));
            }
        } else if (msg instanceof Http3DataFrame) {
            ctx.fireChannelRead(new DefaultHttp2DataFrame(((Http3DataFrame) msg).content()));
        } else if (msg instanceof Http3GoAwayFrame) {
            ctx.fireUserEventTriggered(new DefaultHttp2GoAwayFrame(((Http3GoAwayFrame) msg).id()));
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void pingAck(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.channel().parent().pipeline();
        pipeline.fireChannelRead(new DefaultHttp2PingFrame(0, true));
        pipeline.fireChannelReadComplete();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        if (ctx instanceof QuicStreamChannel) {
            ctx.fireChannelRead(new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER, true));
        } else {
            ctx.fireChannelReadComplete();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            Http2HeadersFrame frame = (Http2HeadersFrame) msg;
            ctx.write(new DefaultHttp3HeadersFrame(new Http3HeadersAdapter(frame.headers())), promise);
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
        } else if (msg instanceof Http2PingFrame) {
            sendPing((QuicChannel) ctx.channel());
        } else {
            ctx.write(msg, promise);
        }
    }

    private void sendPing(QuicChannel channel) {
        Http3.newRequestStream(channel, new Http3RequestStreamInitializer() {
                    @Override
                    protected void initRequestStream(QuicStreamChannel ch) {
                        ch.pipeline().addLast(INSTANCE);
                    }
                })
                .addListener(future -> {
                    if (future.isSuccess()) {
                        QuicStreamChannel streamChannel = (QuicStreamChannel) future.getNow();

                        Http3Headers header = new DefaultHttp3Headers(false);
                        header.set(PseudoHeaderName.METHOD.value(), HttpMethods.OPTIONS.name());
                        header.set(PseudoHeaderName.PATH.value(), "*");
                        header.set(PseudoHeaderName.SCHEME.value(), HttpConstants.HTTPS);
                        header.set(Constants.TRI_PING, "0");

                        streamChannel.write(new DefaultHttp3HeadersFrame(header));
                        streamChannel.shutdownOutput();
                    } else {
                        LOGGER.warn(TRANSPORT_FAILED_RECONNECT, "Failed to send ping frame", future.cause());
                    }
                });
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
