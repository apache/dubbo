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
package org.apache.dubbo.remoting.http12.netty4.h2;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.nested.TripleConfig;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.command.HttpWriteQueue;
import org.apache.dubbo.remoting.http12.exception.UnsupportedMediaTypeException;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2ServerTransportListenerFactory;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.h2.command.Http2WriteQueueChannel;
import org.apache.dubbo.remoting.http12.netty4.HttpWriteQueueHandler;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Set;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2StreamChannel;

public class NettyHttp2ProtocolSelectorHandler extends SimpleChannelInboundHandler<HttpMetadata> {

    private static final String TRANSPORT_LISTENER_FACTORY_CACHE = "TRANSPORT_LISTENER_FACTORY_CACHE";
    private final URL url;

    private final FrameworkModel frameworkModel;

    private final TripleConfig tripleConfig;

    private final Http2ServerTransportListenerFactory defaultHttp2ServerTransportListenerFactory;

    public NettyHttp2ProtocolSelectorHandler(
            URL url,
            FrameworkModel frameworkModel,
            TripleConfig tripleConfig,
            Http2ServerTransportListenerFactory defaultHttp2ServerTransportListenerFactory) {
        this.url = url;
        this.frameworkModel = frameworkModel;
        this.tripleConfig = tripleConfig;
        this.defaultHttp2ServerTransportListenerFactory = defaultHttp2ServerTransportListenerFactory;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpMetadata metadata) {
        String contentType = metadata.contentType();
        Http2ServerTransportListenerFactory factory = UrlUtils.computeServiceAttribute(
                        url,
                        TRANSPORT_LISTENER_FACTORY_CACHE,
                        url -> CollectionUtils.<String, Http2ServerTransportListenerFactory>newConcurrentHashMap())
                .computeIfAbsent(
                        contentType == null ? StringUtils.EMPTY_STRING : contentType,
                        key -> determineHttp2ServerTransportListenerFactory(contentType));
        if (factory == null) {
            throw new UnsupportedMediaTypeException(contentType);
        }
        Channel channel = ctx.channel();
        H2StreamChannel h2StreamChannel = new NettyH2StreamChannel((Http2StreamChannel) channel, tripleConfig);
        HttpWriteQueueHandler writeQueueHandler = channel.parent().pipeline().get(HttpWriteQueueHandler.class);
        if (writeQueueHandler != null) {
            HttpWriteQueue writeQueue = writeQueueHandler.getWriteQueue();
            h2StreamChannel = new Http2WriteQueueChannel(h2StreamChannel, writeQueue);
        }
        Http2TransportListener http2TransportListener = factory.newInstance(h2StreamChannel, url, frameworkModel);
        channel.closeFuture().addListener(future -> http2TransportListener.close());
        ctx.pipeline()
                .addLast(new NettyHttp2FrameHandler(h2StreamChannel, http2TransportListener))
                .remove(this);
        ctx.fireChannelRead(metadata);
    }

    private Http2ServerTransportListenerFactory determineHttp2ServerTransportListenerFactory(String contentType) {
        Set<Http2ServerTransportListenerFactory> http2ServerTransportListenerFactories = frameworkModel
                .getExtensionLoader(Http2ServerTransportListenerFactory.class)
                .getSupportedExtensionInstances();
        for (Http2ServerTransportListenerFactory factory : http2ServerTransportListenerFactories) {
            if (factory.supportContentType(contentType)) {
                return factory;
            }
        }
        return defaultHttp2ServerTransportListenerFactory;
    }
}
