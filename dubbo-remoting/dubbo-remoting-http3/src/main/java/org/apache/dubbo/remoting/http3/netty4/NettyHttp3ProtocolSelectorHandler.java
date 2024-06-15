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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.command.HttpWriteQueue;
import org.apache.dubbo.remoting.http12.exception.UnsupportedMediaTypeException;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.command.Http2WriteQueueChannel;
import org.apache.dubbo.remoting.http12.netty4.HttpWriteQueueHandler;
import org.apache.dubbo.remoting.http12.netty4.h2.NettyHttp2FrameHandler;
import org.apache.dubbo.remoting.http3.Http3ServerTransportListenerFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.incubator.codec.quic.QuicStreamChannel;

@Sharable
public class NettyHttp3ProtocolSelectorHandler extends SimpleChannelInboundHandler<HttpMetadata> {

    private final URL url;
    private final FrameworkModel frameworkModel;

    public NettyHttp3ProtocolSelectorHandler(URL url, FrameworkModel frameworkModel) {
        this.url = url;
        this.frameworkModel = frameworkModel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpMetadata metadata) {
        HttpHeaders headers = metadata.headers();
        String contentType = headers.getFirst(HttpHeaderNames.CONTENT_TYPE.getName());
        Http3ServerTransportListenerFactory factory = determineHttp3ServerTransportListenerFactory(contentType);
        if (factory == null) {
            throw new UnsupportedMediaTypeException(contentType);
        }

        H2StreamChannel streamChannel = new NettyHttp3StreamChannel((QuicStreamChannel) ctx.channel());
        HttpWriteQueueHandler writeQueueHandler = ctx.channel().pipeline().get(HttpWriteQueueHandler.class);
        if (writeQueueHandler != null) {
            HttpWriteQueue writeQueue = writeQueueHandler.getWriteQueue();
            streamChannel = new Http2WriteQueueChannel(streamChannel, writeQueue);
        }

        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast(
                new NettyHttp2FrameHandler(streamChannel, factory.newInstance(streamChannel, url, frameworkModel)));
        pipeline.remove(this);
        ctx.fireChannelRead(metadata);
    }

    private Http3ServerTransportListenerFactory determineHttp3ServerTransportListenerFactory(String contentType) {
        for (Http3ServerTransportListenerFactory factory :
                frameworkModel.getActivateExtensions(Http3ServerTransportListenerFactory.class)) {
            if (factory.supportContentType(contentType)) {
                return factory;
            }
        }
        return null;
    }
}
