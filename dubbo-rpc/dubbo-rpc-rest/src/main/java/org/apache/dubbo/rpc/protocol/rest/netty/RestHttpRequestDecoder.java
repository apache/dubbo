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

package org.apache.dubbo.rpc.protocol.rest.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

import io.netty.handler.codec.http.HttpHeaders;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;
import org.apache.dubbo.rpc.protocol.rest.handler.NettyHttpHandler;
import org.apache.dubbo.rpc.protocol.rest.request.NettyRequestFacade;


public class RestHttpRequestDecoder extends MessageToMessageDecoder<io.netty.handler.codec.http.FullHttpRequest> {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final NettyHttpHandler handler;
    private final Executor executor;


    public RestHttpRequestDecoder(NettyHttpHandler handler, URL url) {
        this.handler = handler;
        executor = url.getOrDefaultFrameworkModel().getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);
    }


    @Override
    protected void decode(ChannelHandlerContext ctx, io.netty.handler.codec.http.FullHttpRequest request, List<Object> out) throws Exception {
        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        NettyHttpResponse nettyHttpResponse = new NettyHttpResponse(ctx, keepAlive);
        NettyRequestFacade requestFacade = new NettyRequestFacade(request, ctx);

        executor.execute(() -> {

            // business handler
            try {
                handler.handle(requestFacade, nettyHttpResponse);

            } catch (IOException e) {
                logger.error("", e.getCause().getMessage(), "dubbo rest rest http request handler error", e.getMessage(), e);
            } finally {
                // write response
                try {
                    nettyHttpResponse.addOutputHeaders(RestHeaderEnum.CONNECTION.getHeader(), "close");
                    nettyHttpResponse.finish();
                } catch (IOException e) {
                    logger.error("", e.getCause().getMessage(), "dubbo rest rest http response flush error", e.getMessage(), e);
                }
            }

        });


    }
}

