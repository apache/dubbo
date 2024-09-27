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
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2ServerChannelObserver;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.remoting.websocket.FinalFragmentStreamingDecoder;
import org.apache.dubbo.remoting.websocket.WebSocketHeaderNames;
import org.apache.dubbo.remoting.websocket.WebSocketTransportListener;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor.RpcType;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.GenericHttp2ServerTransportListener;

import java.util.concurrent.Executor;

public class DefaultWebSocketServerTransportListener extends GenericHttp2ServerTransportListener
        implements WebSocketTransportListener {

    private boolean autoClose = false;

    public DefaultWebSocketServerTransportListener(
            H2StreamChannel h2StreamChannel, URL url, FrameworkModel frameworkModel) {
        super(h2StreamChannel, url, frameworkModel);
    }

    @Override
    protected void onBeforeMetadata(Http2Header metadata) {}

    @Override
    protected Executor initializeExecutor(URL url, Http2Header metadata) {
        return getExecutor(url, metadata);
    }

    @Override
    protected void onPrepareMetadata(Http2Header metadata) {
        doRoute(metadata);
    }

    @Override
    protected StreamingDecoder newStreamingDecoder() {
        return new FinalFragmentStreamingDecoder();
    }

    @Override
    protected Http2ServerChannelObserver newResponseObserver(H2StreamChannel h2StreamChannel) {
        return new WebSocketServerChannelObserver(getFrameworkModel(), h2StreamChannel);
    }

    @Override
    protected Http2ServerChannelObserver newStreamResponseObserver(H2StreamChannel h2StreamChannel) {
        return new WebSocketServerChannelObserver(getFrameworkModel(), h2StreamChannel);
    }

    @Override
    protected Http2ServerChannelObserver prepareResponseObserver(Http2ServerChannelObserver responseObserver) {
        responseObserver.addTrailersCustomizer(this::customizeWebSocketStatus);
        return super.prepareResponseObserver(responseObserver);
    }

    @Override
    protected void prepareUnaryServerCall() {
        autoClose = true;
        super.prepareUnaryServerCall();
    }

    @Override
    protected void prepareStreamServerCall() {
        if (getContext().getMethodDescriptor().getRpcType().equals(RpcType.SERVER_STREAM)) {
            autoClose = true;
        }
        super.prepareStreamServerCall();
    }

    @Override
    protected void onDataCompletion(Http2InputMessage message) {
        if (autoClose) {
            getStreamingDecoder().close();
            return;
        }
        super.onDataCompletion(message);
    }

    private void customizeWebSocketStatus(HttpHeaders httpHeaders, Throwable throwable) {
        if (throwable != null) {
            httpHeaders.set(WebSocketHeaderNames.WEBSOCKET_MESSAGE.getName(), throwable.getMessage());
        }
    }
}
