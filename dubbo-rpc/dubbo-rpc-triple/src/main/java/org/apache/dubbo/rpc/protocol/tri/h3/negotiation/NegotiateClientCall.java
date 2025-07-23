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
package org.apache.dubbo.rpc.protocol.tri.h3.negotiation;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.transport.H2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleHttp2ClientResponseHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.Future;

public class NegotiateClientCall {

    private final AbstractConnectionClient connectionClient;
    private final Executor executor;

    public NegotiateClientCall(AbstractConnectionClient connectionClient, Executor executor) {
        this.connectionClient = connectionClient;
        this.executor = executor;
    }

    public CompletableFuture<String> start(URL url) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            Channel channel = connectionClient.getChannel(true);
            if (channel == null) {
                future.completeExceptionally(new IllegalStateException("Channel is null"));
                return future;
            }
            Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(channel);
            bootstrap.handler(new ChannelInboundHandlerAdapter() {
                @Override
                public void handlerAdded(ChannelHandlerContext ctx) {
                    ctx.channel()
                            .pipeline()
                            .addLast(new ReadTimeoutHandler(12, TimeUnit.SECONDS))
                            .addLast(new TripleHttp2ClientResponseHandler(new Listener(executor, future)));
                }
            });
            Future<Http2StreamChannel> streamFuture = bootstrap.open();
            streamFuture.addListener(f -> {
                if (f.isSuccess()) {
                    streamFuture.getNow().writeAndFlush(buildHeaders(url)).addListener(cf -> {
                        if (cf.isSuccess()) {
                            return;
                        }
                        future.completeExceptionally(cf.cause());
                    });
                    return;
                }
                future.completeExceptionally(f.cause());
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private Http2HeadersFrame buildHeaders(URL url) {
        Http2Headers headers = new DefaultHttp2Headers(false);
        boolean ssl = url.getParameter(CommonConstants.SSL_ENABLED_KEY, false);
        CharSequence scheme = ssl ? TripleConstants.HTTPS_SCHEME : TripleConstants.HTTP_SCHEME;
        headers.scheme(scheme)
                .authority(url.getAddress())
                .method(HttpMethod.OPTIONS.asciiName())
                .path("/")
                .set(TripleHeaderEnum.SERVICE_TIMEOUT.name(), "10000");
        return new DefaultHttp2HeadersFrame(headers, true);
    }

    private static final class Listener implements H2TransportListener {

        private final Executor executor;
        private final CompletableFuture<String> future;

        Listener(Executor executor, CompletableFuture<String> future) {
            this.executor = executor;
            this.future = future;
        }

        @Override
        public void onHeader(Http2Headers headers, boolean endStream) {
            CharSequence line = headers.status();
            if (line != null) {
                HttpResponseStatus status = HttpResponseStatus.parseLine(line);
                if (status.code() < 500) {
                    CharSequence altSvc = headers.get(HttpHeaderNames.ALT_SVC.getKey());
                    executor.execute(() -> future.complete(String.valueOf(altSvc)));
                    return;
                }
            }
            if (endStream) {
                return;
            }
            executor.execute(() -> future.completeExceptionally(new RuntimeException("Status: " + line)));
        }

        @Override
        public void onData(ByteBuf data, boolean endStream) {}

        @Override
        public void cancelByRemote(long errorCode) {
            executor.execute(() -> future.completeExceptionally(new RuntimeException("Canceled by remote")));
        }

        @Override
        public void onClose() {
            if (future.isDone()) {
                return;
            }
            cancelByRemote(TriRpcStatus.CANCELLED.code.code);
        }
    }
}
