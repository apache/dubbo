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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import io.netty.handler.codec.http.HttpHeaders;
import org.apache.dubbo.rpc.protocol.rest.handler.NettyHttpHandler;
import org.apache.dubbo.rpc.protocol.rest.request.NettyRequestFacade;


@ChannelHandler.Sharable
public class RestHttpRequestDecoder extends MessageToMessageDecoder<io.netty.handler.codec.http.FullHttpRequest> {


    private final NettyHttpHandler handler;


    public RestHttpRequestDecoder(NettyHttpHandler handler) {
        this.handler = handler;
    }


    @Override
    protected void decode(ChannelHandlerContext ctx, io.netty.handler.codec.http.FullHttpRequest request, List<Object> out) throws Exception {
        boolean keepAlive = HttpHeaders.isKeepAlive(request);


        NettyHttpResponse nettyHttpResponse = new NettyHttpResponse(ctx, keepAlive);
        NettyRequestFacade requestFacade = new NettyRequestFacade(request, ctx);

        // business handler
        handler.handle(requestFacade, nettyHttpResponse);


    }
}

