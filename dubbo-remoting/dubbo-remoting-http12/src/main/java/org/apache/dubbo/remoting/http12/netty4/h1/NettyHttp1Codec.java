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
package org.apache.dubbo.remoting.http12.netty4.h1;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h1.DefaultHttp1Request;
import org.apache.dubbo.remoting.http12.h1.Http1InputMessage;
import org.apache.dubbo.remoting.http12.h1.Http1RequestMetadata;

import java.io.OutputStream;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

public class NettyHttp1Codec extends ChannelDuplexHandler {

    private boolean keepAlive;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // decode FullHttpRequest
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            keepAlive = HttpUtil.isKeepAlive(request);
            super.channelRead(
                    ctx,
                    new DefaultHttp1Request(
                            new Http1RequestMetadata(
                                    new NettyHttp1HttpHeaders(request.headers()),
                                    request.method().name(),
                                    request.uri()),
                            new Http1InputMessage(new ByteBufInputStream(request.content(), true))));
            return;
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpMetadata) {
            doWriteHeader(ctx, ((HttpMetadata) msg), promise);
            return;
        }
        if (msg instanceof HttpOutputMessage) {
            doWriteMessage(ctx, ((HttpOutputMessage) msg), promise);
            return;
        }
        super.write(ctx, msg, promise);
    }

    private void doWriteHeader(ChannelHandlerContext ctx, HttpMetadata msg, ChannelPromise promise) {
        // process status
        NettyHttp1HttpHeaders headers = (NettyHttp1HttpHeaders) msg.headers();
        List<String> statusHeaders = headers.remove(HttpHeaderNames.STATUS.getKey());
        HttpResponseStatus status = HttpResponseStatus.OK;
        if (CollectionUtils.isNotEmpty(statusHeaders)) {
            status = HttpResponseStatus.valueOf(Integer.parseInt(statusHeaders.get(0)));
        }
        if (keepAlive) {
            headers.add(HttpHeaderNames.CONNECTION.getKey(), String.valueOf(HttpHeaderValues.KEEP_ALIVE));
        } else {
            headers.add(HttpHeaderNames.CONNECTION.getKey(), String.valueOf(HttpHeaderValues.CLOSE));
        }
        // process normal headers
        ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers.getHeaders()), promise);
    }

    private void doWriteMessage(ChannelHandlerContext ctx, HttpOutputMessage msg, ChannelPromise promise) {
        if (HttpOutputMessage.EMPTY_MESSAGE == msg) {
            if (keepAlive) {
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, promise);
            } else {
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, promise).addListener(ChannelFutureListener.CLOSE);
            }
            return;
        }
        OutputStream body = msg.getBody();
        if (body instanceof ByteBufOutputStream) {
            ByteBuf buffer = ((ByteBufOutputStream) body).buffer();
            ctx.writeAndFlush(buffer, promise);
            return;
        }
        throw new IllegalArgumentException("HttpOutputMessage body must be 'io.netty.buffer.ByteBufOutputStream'");
    }
}
