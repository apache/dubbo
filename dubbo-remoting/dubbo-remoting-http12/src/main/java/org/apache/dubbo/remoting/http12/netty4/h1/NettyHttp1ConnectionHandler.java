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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.remoting.http12.HttpMessage;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.remoting.http12.ServerTransportListener;
import org.apache.dubbo.remoting.http12.h1.Http1Request;
import org.apache.dubbo.remoting.http12.h1.Http1ServerTransportListener;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.concurrent.Executor;


public class NettyHttp1ConnectionHandler extends SimpleChannelInboundHandler<Http1Request> {

    private ServerTransportListener<RequestMetadata, HttpMessage> transportListener;

    private final FrameworkModel frameworkModel;

    private final URL url;

    private final Executor executor;

    public NettyHttp1ConnectionHandler(URL url, FrameworkModel frameworkModel) {
        this.url = url;
        this.frameworkModel = frameworkModel;
        this.executor = url.getOrDefaultFrameworkModel().getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);
    }

    protected void channelRead0(ChannelHandlerContext ctx, Http1Request http1Request) {
        //process h1 request
        initTransportListenerIfNecessary(ctx);
        executor.execute(() -> {
            transportListener.onMetadata(http1Request);
            transportListener.onData(http1Request);
        });
    }

    private void initTransportListenerIfNecessary(ChannelHandlerContext ctx) {
        if (transportListener == null) {
            transportListener = new Http1ServerTransportListener(new NettyHttp1Channel(ctx.channel()), frameworkModel);
        }
    }
}
