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
package org.apache.dubbo.rpc.protocol.tri.h12.http1;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpInputMessage;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.remoting.http12.h1.Http1ServerChannelObserver;
import org.apache.dubbo.remoting.http12.h1.Http1ServerStreamChannelObserver;
import org.apache.dubbo.remoting.http12.h1.Http1ServerTransportListener;
import org.apache.dubbo.remoting.http12.message.DefaultListeningDecoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.ListeningDecoder;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.h12.AbstractServerTransportListener;
import org.apache.dubbo.rpc.protocol.tri.h12.DefaultHttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.HttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerStreamServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.UnaryServerCallListener;

public class DefaultHttp11ServerTransportListener
        extends AbstractServerTransportListener<RequestMetadata, HttpInputMessage>
        implements Http1ServerTransportListener {

    private final HttpChannel httpChannel;

    private final URL url;

    public DefaultHttp11ServerTransportListener(HttpChannel httpChannel, URL url, FrameworkModel frameworkModel) {
        super(frameworkModel, url, httpChannel);
        this.url = url;
        this.httpChannel = httpChannel;
    }

    private ServerCallListener startListener(
            RpcInvocation invocation, MethodDescriptor methodDescriptor, Invoker<?> invoker) {
        switch (methodDescriptor.getRpcType()) {
            case UNARY:
                Http1ServerChannelObserver http1ChannelObserver = new Http1ServerChannelObserver(httpChannel);
                http1ChannelObserver.findAndSetEncoder(
                        url,
                        getHttpMetadata().headers().getFirst(HttpHeaderNames.ACCEPT.getName()),
                        getFrameworkModel());
                return new AutoCompleteUnaryServerCallListener(invocation, invoker, http1ChannelObserver);
            case SERVER_STREAM:
                Http1ServerChannelObserver serverStreamChannelObserver =
                        new Http1ServerStreamChannelObserver(httpChannel);
                serverStreamChannelObserver.findAndSetEncoder(
                        url,
                        getHttpMetadata().headers().getFirst(HttpHeaderNames.ACCEPT.getName()),
                        getFrameworkModel());
                serverStreamChannelObserver.setHeadersCustomizer((headers) -> headers.set(
                        HttpHeaderNames.CONTENT_TYPE.getName(), MediaType.TEXT_EVENT_STREAM_VALUE.getName()));
                return new AutoCompleteServerStreamServerCallListener(invocation, invoker, serverStreamChannelObserver);
            default:
                throw new UnsupportedOperationException("HTTP1.x only support unary and server-stream");
        }
    }

    @Override
    protected HttpMessageListener newHttpMessageListener() {
        RequestMetadata httpMetadata = getHttpMetadata();
        String path = httpMetadata.path();
        String[] parts = path.split("/");
        String originalMethodName = parts[2];
        boolean hasStub = getPathResolver().hasNativeStub(path);
        MethodDescriptor methodDescriptor = findMethodDescriptor(getServiceDescriptor(), originalMethodName, hasStub);
        MethodMetadata methodMetadata = MethodMetadata.fromMethodDescriptor(methodDescriptor);
        RpcInvocation rpcInvocation = buildRpcInvocation(getInvoker(), getServiceDescriptor(), methodDescriptor);
        setMethodDescriptor(methodDescriptor);
        setMethodMetadata(methodMetadata);
        setRpcInvocation(rpcInvocation);
        HttpMessageCodec httpMessageCodec = getHttpMessageCodec();
        ListeningDecoder listeningDecoder =
                newListeningDecoder(httpMessageCodec, methodMetadata.getActualRequestTypes());
        return new DefaultHttpMessageListener(listeningDecoder);
    }

    private ListeningDecoder newListeningDecoder(HttpMessageCodec codec, Class<?>[] actualRequestTypes) {
        DefaultListeningDecoder defaultListeningDecoder = new DefaultListeningDecoder(codec, actualRequestTypes);
        ServerCallListener serverCallListener = startListener(getRpcInvocation(), getMethodDescriptor(), getInvoker());
        defaultListeningDecoder.setListener(serverCallListener::onMessage);
        return defaultListeningDecoder;
    }

    private static class AutoCompleteUnaryServerCallListener extends UnaryServerCallListener {

        public AutoCompleteUnaryServerCallListener(
                RpcInvocation invocation, Invoker<?> invoker, StreamObserver<Object> responseObserver) {
            super(invocation, invoker, responseObserver);
        }

        @Override
        public void onMessage(Object message) {
            super.onMessage(message);
            super.onComplete();
        }
    }

    private static class AutoCompleteServerStreamServerCallListener extends ServerStreamServerCallListener {

        public AutoCompleteServerStreamServerCallListener(
                RpcInvocation invocation, Invoker<?> invoker, StreamObserver<Object> responseObserver) {
            super(invocation, invoker, responseObserver);
        }

        @Override
        public void onMessage(Object message) {
            super.onMessage(message);
            super.onComplete();
        }
    }
}
