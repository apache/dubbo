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
package org.apache.dubbo.rpc.protocol.tri.websocket;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.nested.TripleConfig;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessageFrame;
import org.apache.dubbo.remoting.websocket.WebSocketHeaderNames;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static org.apache.dubbo.rpc.protocol.tri.websocket.WebSocketConstants.TRIPLE_WEBSOCKET_REMOTE_ADDRESS;

public class WebSocketStreamChannel implements H2StreamChannel {

    private final Session session;

    private final TripleConfig tripleConfig;

    private final InetSocketAddress remoteAddress;

    private final InetSocketAddress localAddress;

    public WebSocketStreamChannel(Session session, TripleConfig tripleConfig) {
        this.session = session;
        this.tripleConfig = tripleConfig;
        Map<String, List<String>> requestParameterMap = session.getRequestParameterMap();
        List<String> remoteAddressData = requestParameterMap.get(TRIPLE_WEBSOCKET_REMOTE_ADDRESS);
        this.remoteAddress = InetSocketAddress.createUnresolved(
                remoteAddressData.get(0), Integer.parseInt(remoteAddressData.get(1)));
        this.localAddress = InetSocketAddress.createUnresolved(
                session.getRequestURI().getHost(), session.getRequestURI().getPort());
    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        try {
            session.close();
            completableFuture.complete(null);
        } catch (IOException e) {
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }

    @Override
    public HttpOutputMessage newOutputMessage() {
        // Use ByteBuf for zero-copy with size limit awareness
        int maxSize = tripleConfig.getMaxResponseBodySizeOrDefault();
        int initialCapacity = Math.min(256, maxSize);
        return new Http2OutputMessageFrame(alloc().buffer(initialCapacity, maxSize), false);
    }

    @Override
    public HttpOutputMessage newOutputMessage(ByteBuf body) {
        // Zero-copy: directly use ByteBuf, but check size limit
        int maxSize = tripleConfig.getMaxResponseBodySizeOrDefault();
        if (body.readableBytes() > maxSize) {
            throw new IllegalArgumentException(
                    "Response body size " + body.readableBytes() + " exceeds limit " + maxSize);
        }
        return new Http2OutputMessageFrame(body, false);
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        Http2Header http2Header = (Http2Header) httpMetadata;
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        if (http2Header.isEndStream()) {
            try {
                session.close(encodeCloseReason(http2Header));
                completableFuture.complete(null);
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
            }
        }
        return completableFuture;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        // Adapt to zero-copy: getBody() now returns ByteBuf
        ByteBuf bodyBuffer = httpOutputMessage.getBody();
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        try {
            if (bodyBuffer != null && bodyBuffer.isReadable()) {
                // Convert ByteBuf to ByteBuffer for WebSocket
                ByteBuffer byteBuffer = bodyBuffer.nioBuffer();
                session.getBasicRemote().sendBinary(byteBuffer);
            }
            completableFuture.complete(null);
        } catch (IOException e) {
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }

    @Override
    public CompletableFuture<Void> sendMessage(Object message, boolean endStream) {
        if (message instanceof ByteBuf) {
            return writeMessage(newOutputMessage((ByteBuf) message));
        }
        return writeMessage((HttpOutputMessage) message);
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public SocketAddress localAddress() {
        return localAddress;
    }

    @Override
    public ByteBufAllocator alloc() {
        return H2StreamChannel.super.alloc();
    }

    @Override
    public void flush() {}

    private CloseReason encodeCloseReason(Http2Header http2Header) {
        HttpHeaders headers = http2Header.headers();
        List<String> statusHeaders = headers.remove(HttpHeaderNames.STATUS.getName());
        CloseReason closeReason;
        if (CollectionUtils.isNotEmpty(statusHeaders)
                && !HttpStatus.OK.getStatusString().equals(statusHeaders.get(0))) {
            List<String> messageHeaders = headers.remove(WebSocketHeaderNames.WEBSOCKET_MESSAGE.getName());
            closeReason = new CloseReason(
                    CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                    CollectionUtils.isNotEmpty(messageHeaders) ? messageHeaders.get(0) : "Internal server error");
        } else {
            closeReason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Bye");
        }
        return closeReason;
    }
}
