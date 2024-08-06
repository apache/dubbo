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
package org.apache.dubbo.rpc.protocol.tri.websocket;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.ExceptionHandler;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2ServerChannelObserver;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.remoting.websocket.FinalFragmentStreamingDecoder;
import org.apache.dubbo.remoting.websocket.WebSocketHeaderNames;
import org.apache.dubbo.remoting.websocket.WebSocketTransportListener;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerStreamServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.UnaryServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.GenericHttp2ServerTransportListener;

public class DefaultWebSocketServerTransportListener extends GenericHttp2ServerTransportListener
        implements WebSocketTransportListener {

    public DefaultWebSocketServerTransportListener(
            H2StreamChannel h2StreamChannel, URL url, FrameworkModel frameworkModel) {
        super(h2StreamChannel, url, frameworkModel);
        getServerChannelObserver().setTrailersCustomizer(this::webSocketTrailersCustomize);
    }

    private void webSocketTrailersCustomize(HttpHeaders httpHeaders, Throwable throwable) {
        if (throwable != null) {
            httpHeaders.set(WebSocketHeaderNames.WEBSOCKET_MESSAGE.getName(), throwable.getMessage());
        }
    }

    @Override
    protected StreamingDecoder newStreamingDecoder() {
        return new FinalFragmentStreamingDecoder();
    }

    @Override
    protected Http2ServerChannelObserver newHttp2ServerChannelObserver(
            FrameworkModel frameworkModel, H2StreamChannel h2StreamChannel) {
        return new WebSocketServerChannelObserver(frameworkModel, h2StreamChannel);
    }

    @Override
    protected void onUnary() {}

    @Override
    protected UnaryServerCallListener startUnary(
            RpcInvocation invocation, Invoker<?> invoker, Http2ServerChannelObserver responseObserver) {
        return new AutoCloseUnaryServerCallListener(
                invocation, invoker, responseObserver, getStreamingDecoder(), applyCustomizeException());
    }

    @Override
    protected ServerStreamServerCallListener startServerStreaming(
            RpcInvocation invocation, Invoker<?> invoker, Http2ServerChannelObserver responseObserver) {
        return new AutoCloseServerStreamServerCallListener(
                invocation, invoker, responseObserver, getStreamingDecoder());
    }

    @Override
    protected ExceptionHandler<Throwable, ?> getExceptionHandler() {
        return null;
    }
}
