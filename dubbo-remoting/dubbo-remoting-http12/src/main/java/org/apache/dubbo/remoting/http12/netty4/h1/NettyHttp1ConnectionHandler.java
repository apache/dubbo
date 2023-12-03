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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.exception.UnsupportedMediaTypeException;
import org.apache.dubbo.remoting.http12.h1.Http1Request;
import org.apache.dubbo.remoting.http12.h1.Http1ServerChannelObserver;
import org.apache.dubbo.remoting.http12.h1.Http1ServerTransportListener;
import org.apache.dubbo.remoting.http12.h1.Http1ServerTransportListenerFactory;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodecFactory;
import org.apache.dubbo.remoting.http12.message.codec.CodecUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.List;
import java.util.concurrent.Executor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NettyHttp1ConnectionHandler extends SimpleChannelInboundHandler<Http1Request> {

    private Http1ServerTransportListenerFactory http1ServerTransportListenerFactory;

    private final FrameworkModel frameworkModel;

    private final URL url;

    private final Executor executor;

    private Http1ServerChannelObserver errorResponseObserver;

    public NettyHttp1ConnectionHandler(URL url, FrameworkModel frameworkModel) {
        this.url = url;
        this.frameworkModel = frameworkModel;
        this.executor = url.getOrDefaultFrameworkModel()
                .getExtensionLoader(ThreadPool.class)
                .getAdaptiveExtension()
                .getExecutor(url);
    }

    public NettyHttp1ConnectionHandler(
            URL url,
            FrameworkModel frameworkModel,
            Http1ServerTransportListenerFactory http1ServerTransportListenerFactory) {
        this.url = url;
        this.frameworkModel = frameworkModel;
        this.executor = url.getOrDefaultFrameworkModel()
                .getExtensionLoader(ThreadPool.class)
                .getAdaptiveExtension()
                .getExecutor(url);
        this.http1ServerTransportListenerFactory = http1ServerTransportListenerFactory;
    }

    public void setHttp1ServerTransportListenerFactory(
            Http1ServerTransportListenerFactory http1ServerTransportListenerFactory) {
        this.http1ServerTransportListenerFactory = http1ServerTransportListenerFactory;
    }

    protected void channelRead0(ChannelHandlerContext ctx, Http1Request http1Request) {
        // process h1 request
        Http1ServerTransportListener http1TransportListener = initTransportListenerIfNecessary(ctx, http1Request);
        initErrorResponseObserver(ctx, http1Request);
        executor.execute(() -> {
            try {
                http1TransportListener.onMetadata(http1Request);
                http1TransportListener.onData(http1Request);
            } catch (Exception e) {
                errorResponseObserver.onError(e);
            }
        });
    }

    private Http1ServerTransportListener initTransportListenerIfNecessary(
            ChannelHandlerContext ctx, Http1Request http1Request) {
        // each h1 request create http1TransportListener instance
        Http1ServerTransportListenerFactory http1ServerTransportListenerFactory =
                this.http1ServerTransportListenerFactory;
        Assert.notNull(http1ServerTransportListenerFactory, "http1ServerTransportListenerFactory must be not null.");
        Http1ServerTransportListener http1TransportListener = http1ServerTransportListenerFactory.newInstance(
                new NettyHttp1Channel(ctx.channel()), url, frameworkModel);

        HttpHeaders headers = http1Request.headers();
        String contentType = headers.getFirst(HttpHeaderNames.CONTENT_TYPE.getName());
        if (!StringUtils.hasText(contentType)) {
            throw new UnsupportedMediaTypeException(contentType);
        }
        HttpMessageCodecFactory codecFactory =
                CodecUtils.determineHttpMessageCodecFactory(frameworkModel, headers.getContentType(), true);
        if (codecFactory == null) {
            throw new UnsupportedMediaTypeException(contentType);
        }
        return http1TransportListener;
    }

    private void initErrorResponseObserver(ChannelHandlerContext ctx, Http1Request request) {
        this.errorResponseObserver = new Http1ServerChannelObserver(new NettyHttp1Channel(ctx.channel()));
        this.errorResponseObserver.setResponseEncoder(
                CodecUtils.determineHttpMessageCodec(frameworkModel, request.headers(), url, false));
    }

    private static HttpMessageCodecFactory findSuitableCodec(
            String contentType, List<HttpMessageCodecFactory> candidates) {
        for (HttpMessageCodecFactory factory : candidates) {
            if (factory.codecSupport().supportDecode(contentType)) {
                return factory;
            }
        }
        return null;
    }
}
