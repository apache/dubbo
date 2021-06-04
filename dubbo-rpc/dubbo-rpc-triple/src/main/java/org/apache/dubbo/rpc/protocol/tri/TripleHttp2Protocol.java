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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.api.Http2WireProtocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.SslContext;

@Activate
public class TripleHttp2Protocol extends Http2WireProtocol {

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void configServerPipeline(ChannelPipeline pipeline, SslContext sslContext) {
        final Http2FrameCodec codec = Http2FrameCodecBuilder.forServer()
                .gracefulShutdownTimeoutMillis(10000)
                .initialSettings(new Http2Settings()
                        .maxHeaderListSize(8192)
                        .maxFrameSize(2 << 16)
                        .maxConcurrentStreams(Integer.MAX_VALUE)
                        .initialWindowSize(1048576))
                .frameLogger(SERVER_LOGGER)
                .build();
        final Http2MultiplexHandler handler = new Http2MultiplexHandler(new TripleServerInitializer());
        pipeline.addLast(codec, new TripleServerConnectionHandler(), handler,
                new SimpleChannelInboundHandler<Object>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                        // empty
                    }
                });
    }

    @Override
    public void configClientPipeline(ChannelPipeline pipeline, SslContext sslContext) {
        final Http2FrameCodec codec = Http2FrameCodecBuilder.forClient()
                .initialSettings(new Http2Settings()
                        .maxHeaderListSize(8192)
                        .maxFrameSize(2 << 16)
                        .maxConcurrentStreams(Integer.MAX_VALUE)
                        .initialWindowSize(1048576))
                .gracefulShutdownTimeoutMillis(10000)
                .frameLogger(CLIENT_LOGGER)
                .build();
        final Http2MultiplexHandler handler = new Http2MultiplexHandler(new SimpleChannelInboundHandler<Object>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                // empty
            }
        });
        pipeline.addLast(codec, handler, new TripleClientHandler());
    }
}
