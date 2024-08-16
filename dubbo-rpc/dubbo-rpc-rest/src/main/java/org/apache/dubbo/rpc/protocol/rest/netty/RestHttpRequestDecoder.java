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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.handler.NettyHttpHandler;
import org.apache.dubbo.rpc.protocol.rest.request.NettyRequestFacade;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpHeaders;

import static org.apache.dubbo.config.Constants.SERVER_THREAD_POOL_NAME;

public class RestHttpRequestDecoder extends MessageToMessageDecoder<io.netty.handler.codec.http.FullHttpRequest> {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final Executor executor;
    private final ServiceDeployer serviceDeployer;
    private final URL url;
    private final NettyHttpHandler nettyHttpHandler;

    public RestHttpRequestDecoder(URL url, ServiceDeployer serviceDeployer) {

        this.url = url;
        this.serviceDeployer = serviceDeployer;
        executor = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel())
                .createExecutorIfAbsent(ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME));
        nettyHttpHandler = new NettyHttpHandler(serviceDeployer, url);
    }

    @Override
    protected void decode(
            ChannelHandlerContext ctx, io.netty.handler.codec.http.FullHttpRequest request, List<Object> out)
            throws Exception {
        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        NettyHttpResponse nettyHttpResponse = new NettyHttpResponse(ctx, keepAlive, url);
        NettyRequestFacade requestFacade = new NettyRequestFacade(request, ctx, serviceDeployer);

        executor.execute(() -> {

            // business handler
            try {
                nettyHttpHandler.handle(requestFacade, nettyHttpResponse);

            } catch (IOException e) {
                logger.error(
                        "", e.getCause().getMessage(), "dubbo rest rest http request handler error", e.getMessage(), e);
            } finally {
                // write response
                try {
                    nettyHttpResponse.addOutputHeaders(RestHeaderEnum.CONNECTION.getHeader(), "close");
                    nettyHttpResponse.finish();
                } catch (IOException e) {
                    logger.error(
                            "",
                            e.getCause().getMessage(),
                            "dubbo rest rest http response flush error",
                            e.getMessage(),
                            e);
                }
            }
        });
    }
}
