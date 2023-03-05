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
package org.apache.dubbo.rpc.protocol.rest.handler;

import io.netty.handler.codec.http.FullHttpRequest;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.RPCInvocationBuilder;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.exception.PathNoFoundException;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionMapper;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacadeFactory;
import org.apache.dubbo.rpc.protocol.rest.util.HttpHeaderUtil;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;
import org.apache.dubbo.rpc.protocol.rest.util.Pair;

import javax.servlet.ServletException;
import java.io.IOException;

public class NettyHttpHandler implements HttpHandler<FullHttpRequest, NettyHttpResponse> {

    @Override
    public void handle(FullHttpRequest nettyHttpRequest, NettyHttpResponse nettyHttpResponse) throws IOException, ServletException {

        RequestFacade request = RequestFacadeFactory.createRequestFacade(nettyHttpRequest);
//        RpcContext.getServiceContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
//            dispatcher.service(request, servletResponse);

        Pair<RpcInvocation, Invoker> build = null;
        try {
            build = RPCInvocationBuilder.build(request, nettyHttpRequest, nettyHttpResponse);
        } catch (PathNoFoundException e) {
            nettyHttpResponse.setStatus(404);
        }

        Invoker invoker = build.getSecond();

        Result invoke = invoker.invoke(build.getFirst());

        if (invoke.hasException()) {

            if (ExceptionMapper.hasExceptionMapper(invoke.getException())) {
                writeResult(nettyHttpResponse, request, invoker, ExceptionMapper.exceptionToResult(invoke.getException()));
            } else {
                nettyHttpResponse.setStatus(500);
            }

        } else {
            Object value = invoke.getValue();
            writeResult(nettyHttpResponse, request, invoker, value);
        }

        HttpHeaderUtil.addProviderAttachments(nettyHttpResponse);
    }

    private void writeResult(NettyHttpResponse nettyHttpResponse, RequestFacade request, Invoker invoker, Object value) {
        try {
            String accept = request.getHeader(RestConstant.ACCEPT);
            MediaType mediaType = MediaTypeUtil.convertMediaType(accept);

            Pair<Boolean, MediaType> booleanMediaTypePair = HttpMessageCodecManager.httpMessageEncode(nettyHttpResponse.getOutputStream(), value, invoker.getUrl(), mediaType);

            Boolean encoded = booleanMediaTypePair.getFirst();

            if (encoded) {
                nettyHttpResponse.addOutputHeaders(RestConstant.CONTENT_TYPE, booleanMediaTypePair.getSecond().value);
            }


            nettyHttpResponse.setStatus(200);
        } catch (Throwable e) {
            nettyHttpResponse.setStatus(500);
        }
    }
}
