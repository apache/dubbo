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
package org.apache.dubbo.remoting.websocket.netty4;

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.netty4.h1.NettyHttp1HttpHeaders;
import org.apache.dubbo.remoting.websocket.FinalFragmentByteBufInputStream;
import org.apache.dubbo.remoting.websocket.WebSocketHeaderNames;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;

public class WebSocketFrameCodec extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof BinaryWebSocketFrame || msg instanceof TextWebSocketFrame) {
            Http2InputMessage http2InputMessage = onDataFrame((WebSocketFrame) msg);
            super.channelRead(ctx, http2InputMessage);
        } else if (msg instanceof CloseWebSocketFrame) {
            Object closeMessage = onCloseFrame((CloseWebSocketFrame) msg);
            super.channelRead(ctx, closeMessage);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Http2OutputMessage) {
            WebSocketFrame webSocketFrame = encodeWebSocketFrame(ctx, (Http2OutputMessage) msg);
            super.write(ctx, webSocketFrame, promise);
        } else if (msg instanceof Http2Header) {
            Http2Header http2Header = (Http2Header) msg;
            if (http2Header.isEndStream()) {
                CloseWebSocketFrame closeWebSocketFrame = encodeCloseWebSocketFrame(http2Header);
                super.write(ctx, closeWebSocketFrame, promise);
            }
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            Http2Header http2Header = onHandshakeComplete((WebSocketServerProtocolHandler.HandshakeComplete) evt);
            super.channelRead(ctx, http2Header);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private Http2Header onHandshakeComplete(WebSocketServerProtocolHandler.HandshakeComplete evt) {
        HttpHeaders httpHeaders = new NettyHttp1HttpHeaders(evt.requestHeaders());
        httpHeaders.set(HttpHeaderNames.PATH.getName(), evt.requestUri());
        httpHeaders.set(HttpHeaderNames.METHOD.getName(), HttpMethods.POST.name());
        return new Http2MetadataFrame(httpHeaders);
    }

    private Http2InputMessageFrame onDataFrame(WebSocketFrame webSocketFrame) {
        ByteBuf data = webSocketFrame.content();
        return new Http2InputMessageFrame(
                new FinalFragmentByteBufInputStream(data, true, webSocketFrame.isFinalFragment()), false);
    }

    private Object onCloseFrame(CloseWebSocketFrame closeWebSocketFrame) {
        if (closeWebSocketFrame.statusCode() != WebSocketCloseStatus.NORMAL_CLOSURE.code()) {
            return new DefaultHttp2ResetFrame(closeWebSocketFrame.statusCode());
        }
        return new Http2InputMessageFrame(StreamUtils.EMPTY, true);
    }

    private CloseWebSocketFrame encodeCloseWebSocketFrame(Http2Header http2Header) {
        HttpHeaders headers = http2Header.headers();
        List<String> statusHeaders = headers.remove(HttpHeaderNames.STATUS.getName());
        WebSocketCloseStatus status = WebSocketCloseStatus.NORMAL_CLOSURE;
        if (CollectionUtils.isNotEmpty(statusHeaders)
                && !HttpStatus.OK.getStatusString().equals(statusHeaders.get(0))) {
            List<String> messageHeaders = headers.remove(WebSocketHeaderNames.WEBSOCKET_MESSAGE.getName());
            status = new WebSocketCloseStatus(
                    WebSocketCloseStatus.INTERNAL_SERVER_ERROR.code(),
                    CollectionUtils.isNotEmpty(messageHeaders)
                            ? messageHeaders.get(0)
                            : WebSocketCloseStatus.INTERNAL_SERVER_ERROR.reasonText());
        }
        return new CloseWebSocketFrame(status);
    }

    private WebSocketFrame encodeWebSocketFrame(ChannelHandlerContext ctx, Http2OutputMessage outputMessage)
            throws IOException {
        OutputStream body = outputMessage.getBody();
        if (body == null) {
            return new BinaryWebSocketFrame();
        }
        if (body instanceof ByteBufOutputStream) {
            ByteBuf buffer = ((ByteBufOutputStream) body).buffer();
            return new BinaryWebSocketFrame(buffer);
        }
        throw new IllegalArgumentException("Http2OutputMessage body must be ByteBufOutputStream");
    }
}
