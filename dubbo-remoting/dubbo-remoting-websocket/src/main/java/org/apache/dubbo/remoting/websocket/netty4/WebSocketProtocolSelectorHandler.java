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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.nested.TripleConfig;
import org.apache.dubbo.remoting.http12.command.HttpWriteQueue;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.command.Http2WriteQueueChannel;
import org.apache.dubbo.remoting.http12.netty4.HttpWriteQueueHandler;
import org.apache.dubbo.remoting.http12.netty4.h2.NettyHttp2FrameHandler;
import org.apache.dubbo.remoting.websocket.WebSocketServerTransportListenerFactory;
import org.apache.dubbo.remoting.websocket.WebSocketTransportListener;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

public class WebSocketProtocolSelectorHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final URL url;

    private final FrameworkModel frameworkModel;

    private final TripleConfig tripleConfig;

    private final WebSocketServerTransportListenerFactory defaultWebSocketServerTransportListenerFactory;

    public WebSocketProtocolSelectorHandler(
            URL url,
            FrameworkModel frameworkModel,
            TripleConfig tripleConfig,
            WebSocketServerTransportListenerFactory defaultWebSocketServerTransportListenerFactory) {
        this.url = url;
        this.frameworkModel = frameworkModel;
        this.tripleConfig = tripleConfig;
        this.defaultWebSocketServerTransportListenerFactory = defaultWebSocketServerTransportListenerFactory;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        H2StreamChannel streamChannel = new NettyWebSocketChannel(ctx.channel(), tripleConfig);
        HttpWriteQueueHandler writeQueueHandler = ctx.channel().pipeline().get(HttpWriteQueueHandler.class);
        if (writeQueueHandler != null) {
            HttpWriteQueue writeQueue = writeQueueHandler.getWriteQueue();
            streamChannel = new Http2WriteQueueChannel(streamChannel, writeQueue);
        }
        WebSocketTransportListener webSocketTransportListener =
                defaultWebSocketServerTransportListenerFactory.newInstance(streamChannel, url, frameworkModel);
        ctx.channel().closeFuture().addListener(future -> webSocketTransportListener.close());
        ctx.pipeline()
                .addLast(new NettyHttp2FrameHandler(streamChannel, webSocketTransportListener))
                .remove(this);
        ctx.fireChannelRead(msg.retain());
    }
}
