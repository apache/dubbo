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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.SimpleHttpMessage;
import org.apache.dubbo.remoting.http12.h1.DefaultHttp1Request;
import org.apache.dubbo.remoting.http12.h1.Http1Request;
import org.apache.dubbo.remoting.http12.h1.Http1Response;
import org.apache.dubbo.remoting.http12.h1.Http1RequestMetadata;

import java.io.InputStream;
import java.util.List;
import java.util.Map;


public class NettyHttp1Codec extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //decode FullHttpRequest
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
            HttpHeaders headers = fullHttpRequest.headers();
            Http1RequestMetadata http1RequestMetadata = new Http1RequestMetadata();
            http1RequestMetadata.setHeaders(new org.apache.dubbo.remoting.http12.HttpHeaders());
            http1RequestMetadata.setPath(fullHttpRequest.uri());
            http1RequestMetadata.setMethod(fullHttpRequest.method().name());
            org.apache.dubbo.remoting.http12.HttpHeaders httpHeaders = new org.apache.dubbo.remoting.http12.HttpHeaders();
            for (Map.Entry<String, String> header : headers) {
                String key = header.getKey();
                httpHeaders.set(key, header.getValue());
            }
            http1RequestMetadata.setHeaders(httpHeaders);
            Http1Request http1Request = new DefaultHttp1Request(http1RequestMetadata, new SimpleHttpMessage(new ByteBufInputStream(fullHttpRequest.content())));
            super.channelRead(ctx, http1Request);
            return;
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof Http1Response)) {
            super.write(ctx, msg, promise);
            return;
        }
        //encode Http1Response
        Http1Response message = (Http1Response) msg;
        InputStream body = message.getBody();
        ByteBuf buffer = ctx.alloc().buffer(body.available());
        //copy bytes
        byte[] data = new byte[4096];
        int len;
        while ((len = body.read(data)) != -1) {
            buffer.writeBytes(data, 0, len);
        }
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        HttpHeaders headers = fullHttpResponse.headers();
        for (Map.Entry<String, List<String>> entry : message.headers().entrySet()) {
            headers.set(entry.getKey(), entry.getValue());
        }

        //add default headers
        if (!headers.contains(HttpHeaderNames.CONTENT_TYPE.getName())) {
            headers.set(HttpHeaderNames.CONTENT_TYPE.getName(), fullHttpResponse.content().readableBytes());
        }
        super.write(ctx, fullHttpResponse, promise);
    }
}
