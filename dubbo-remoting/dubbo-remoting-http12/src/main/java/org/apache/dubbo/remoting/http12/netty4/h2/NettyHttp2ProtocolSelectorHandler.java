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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2StreamChannel;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.h2.GenericHttp2ServerTransportListenerFactory;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2ServerTransportListenerFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.List;

public class NettyHttp2ProtocolSelectorHandler extends SimpleChannelInboundHandler<HttpMetadata> {

    private final URL url;

    private final FrameworkModel frameworkModel;

    public NettyHttp2ProtocolSelectorHandler(URL url, FrameworkModel frameworkModel) {
        this.url = url;
        this.frameworkModel = frameworkModel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpMetadata metadata) {
        HttpHeaders headers = metadata.headers();
        String contentType = headers.getFirst(HttpHeaderNames.CONTENT_TYPE.getName());
        Assert.assertTrue(StringUtils.hasText(contentType), "content-type must be not null.");
        Http2ServerTransportListenerFactory factory = adaptHttp2ServerTransportListenerFactory(contentType);
        H2StreamChannel h2StreamChannel = new NettyH2StreamChannel((Http2StreamChannel) ctx.channel());
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast(new NettyHttp2FrameHandler(factory.newInstance(h2StreamChannel, url, frameworkModel)));
        pipeline.remove(this);
        ctx.fireChannelRead(metadata);
    }

    private Http2ServerTransportListenerFactory adaptHttp2ServerTransportListenerFactory(String contentType) {
        List<Http2ServerTransportListenerFactory> http2ServerTransportListenerFactories = frameworkModel.getExtensionLoader(Http2ServerTransportListenerFactory.class).getActivateExtensions();
        for (Http2ServerTransportListenerFactory factory : http2ServerTransportListenerFactories) {
            if (factory.supportContentType(contentType)) {
                return factory;
            }
        }
        return GenericHttp2ServerTransportListenerFactory.INSTANCE;
    }
}
