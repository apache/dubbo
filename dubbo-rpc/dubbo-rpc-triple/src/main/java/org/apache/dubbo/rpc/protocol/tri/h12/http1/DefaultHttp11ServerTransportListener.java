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
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpInputMessage;
import org.apache.dubbo.remoting.http12.MessageTypeToken;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.remoting.http12.h1.Http1ServerTransportListener;
import org.apache.dubbo.remoting.http12.message.ListeningDecoder;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.codec.JsonCodec;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.Http3Exchanger;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;
import org.apache.dubbo.rpc.protocol.tri.h12.AbstractServerTransportListener;
import org.apache.dubbo.rpc.protocol.tri.h12.DefaultHttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.HttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerStreamServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.UnaryServerCallListener;

public class DefaultHttp11ServerTransportListener<INPUT, OUTPUT>
        extends AbstractServerTransportListener<RequestMetadata, HttpInputMessage<INPUT>, INPUT, OUTPUT>
        implements Http1ServerTransportListener<INPUT, OUTPUT> {

    private final HttpChannel<OUTPUT> httpChannel;
    private Http1ServerChannelObserver<OUTPUT> responseObserver;

    public DefaultHttp11ServerTransportListener(
            HttpChannel<OUTPUT> httpChannel,
            URL url,
            FrameworkModel frameworkModel,
            MessageTypeToken<INPUT, OUTPUT> typeToken) {
        super(frameworkModel, url, httpChannel, typeToken);
        this.httpChannel = httpChannel;
        responseObserver = prepareResponseObserver(new Http1UnaryServerChannelObserver<>(
                frameworkModel, httpChannel, getTypeToken().getOutputType()));
    }

    private Http1ServerChannelObserver<OUTPUT> prepareResponseObserver(
            Http1ServerChannelObserver<OUTPUT> responseObserver) {
        responseObserver.setExceptionCustomizer(getExceptionCustomizer());
        RpcInvocationBuildContext context = getContext();
        responseObserver.setResponseEncoder(context == null ? JsonCodec.INSTANCE : context.getHttpMessageEncoder());
        return responseObserver;
    }

    @Override
    protected HttpMessageListener<INPUT> buildHttpMessageListener() {
        RpcInvocationBuildContext context = getContext();
        RpcInvocation rpcInvocation = buildRpcInvocation(context);

        ServerCallListener serverCallListener =
                startListener(rpcInvocation, context.getMethodDescriptor(), context.getInvoker());
        ListeningDecoder<INPUT> listeningDecoder = getMessageHandler()
                .createListeningDecoder(
                        context.getHttpMessageDecoder(),
                        context.getMethodMetadata().getActualRequestTypes());
        listeningDecoder.setListener(serverCallListener::onMessage);
        return new DefaultHttpMessageListener<>(listeningDecoder);
    }

    private ServerCallListener startListener(
            RpcInvocation invocation, MethodDescriptor methodDescriptor, Invoker<?> invoker) {
        switch (methodDescriptor.getRpcType()) {
            case UNARY:
                return new AutoCompleteUnaryServerCallListener(invocation, invoker, responseObserver);
            case SERVER_STREAM:
                responseObserver = prepareResponseObserver(new Http1SseServerChannelObserver<>(
                        getFrameworkModel(), httpChannel, getTypeToken().getOutputType()));
                responseObserver.addHeadersCustomizer((hs, t) ->
                        hs.set(HttpHeaderNames.CONTENT_TYPE.getKey(), MediaType.TEXT_EVENT_STREAM.getName()));
                return new AutoCompleteServerStreamServerCallListener(invocation, invoker, responseObserver);
            default:
                throw new UnsupportedOperationException("HTTP1.x only support unary and server-stream");
        }
    }

    @Override
    protected void onMetadataCompletion(RequestMetadata metadata) {
        responseObserver.setResponseEncoder(getContext().getHttpMessageEncoder());
    }

    @Override
    protected void onError(Throwable throwable) {
        responseObserver.onError(throwable);
    }

    @Override
    protected String protocol() {
        return "http";
    }

    @Override
    protected void initializeAltSvc(URL url) {
        String protocolId = Http3Exchanger.isEnabled(url) ? "h3" : "h2";
        String value = protocolId + "=\":" + url.getParameter(Constants.BIND_PORT_KEY, url.getPort()) + '"';
        responseObserver.addHeadersCustomizer((hs, t) -> hs.set(HttpHeaderNames.ALT_SVC.getKey(), value));
    }

    private static final class AutoCompleteUnaryServerCallListener extends UnaryServerCallListener {

        AutoCompleteUnaryServerCallListener(
                RpcInvocation invocation, Invoker<?> invoker, StreamObserver<Object> responseObserver) {
            super(invocation, invoker, responseObserver);
        }

        @Override
        public void onMessage(Object message) {
            super.onMessage(message);
            onComplete();
        }
    }

    private static final class AutoCompleteServerStreamServerCallListener extends ServerStreamServerCallListener {

        AutoCompleteServerStreamServerCallListener(
                RpcInvocation invocation, Invoker<?> invoker, StreamObserver<Object> responseObserver) {
            super(invocation, invoker, responseObserver);
        }

        @Override
        public void onMessage(Object message) {
            super.onMessage(message);
            onComplete();
        }
    }
}
