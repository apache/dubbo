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
package org.apache.dubbo.rpc.protocol.tri.h12.http2;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.MessageTypeToken;
import org.apache.dubbo.remoting.http12.h2.CancelStreamException;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.message.ListeningDecoder;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.remoting.http12.message.codec.JsonCodec;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.Http3Exchanger;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;
import org.apache.dubbo.rpc.protocol.tri.h12.AbstractServerTransportListener;
import org.apache.dubbo.rpc.protocol.tri.h12.BiStreamServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.HttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerStreamServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.UnaryServerCallListener;

public class GenericHttp2ServerTransportListener<INPUT, OUTPUT>
        extends AbstractServerTransportListener<Http2Header, Http2InputMessage<INPUT>, INPUT, OUTPUT>
        implements Http2TransportListener<INPUT, OUTPUT> {

    private final H2StreamChannel<OUTPUT> h2StreamChannel;
    private final StreamingDecoder<INPUT> streamingDecoder;
    private Http2ServerChannelObserver<INPUT, OUTPUT> responseObserver;
    private ServerCallListener serverCallListener;

    public GenericHttp2ServerTransportListener(
            H2StreamChannel<OUTPUT> h2StreamChannel,
            URL url,
            FrameworkModel frameworkModel,
            MessageTypeToken<INPUT, OUTPUT> typeToken) {
        super(frameworkModel, url, h2StreamChannel, typeToken);
        this.h2StreamChannel = h2StreamChannel;
        streamingDecoder = newStreamingDecoder();
        responseObserver = prepareResponseObserver(newResponseObserver(h2StreamChannel));
    }

    protected StreamingDecoder<INPUT> newStreamingDecoder() {
        return getMessageHandler().createStreamingDecoder();
    }

    protected Http2ServerChannelObserver<INPUT, OUTPUT> newResponseObserver(H2StreamChannel<OUTPUT> h2StreamChannel) {
        return new Http2UnaryServerChannelObserver<>(getFrameworkModel(), h2StreamChannel, getTypeToken());
    }

    protected Http2ServerChannelObserver<INPUT, OUTPUT> newStreamResponseObserver(
            H2StreamChannel<OUTPUT> h2StreamChannel) {
        Http2ServerChannelObserver<INPUT, OUTPUT> responseObserver =
                new Http2SseServerChannelObserver<>(getFrameworkModel(), h2StreamChannel, getTypeToken());
        responseObserver.addHeadersCustomizer(
                (hs, t) -> hs.set(HttpHeaderNames.CONTENT_TYPE.getKey(), MediaType.TEXT_EVENT_STREAM.getName()));
        return responseObserver;
    }

    protected Http2ServerChannelObserver<INPUT, OUTPUT> prepareResponseObserver(
            Http2ServerChannelObserver<INPUT, OUTPUT> responseObserver) {
        responseObserver.setExceptionCustomizer(getExceptionCustomizer());
        RpcInvocationBuildContext context = getContext();
        responseObserver.setResponseEncoder(context == null ? JsonCodec.INSTANCE : context.getHttpMessageEncoder());
        responseObserver.setCancellationContext(RpcContext.getCancellationContext());
        responseObserver.setStreamingDecoder(streamingDecoder);
        return responseObserver;
    }

    @Override
    protected HttpMessageListener<INPUT> buildHttpMessageListener() {
        RpcInvocationBuildContext context = getContext();
        RpcInvocation rpcInvocation = buildRpcInvocation(context);

        serverCallListener = startListener(rpcInvocation, context.getMethodDescriptor(), context.getInvoker());
        ListeningDecoder<INPUT> listeningDecoder = newListeningDecoder(context);
        listeningDecoder.setListener(new Http2StreamingDecodeListener(serverCallListener));
        streamingDecoder.setFragmentListener(new StreamingDecoder.DefaultFragmentListener<>(listeningDecoder));
        return new StreamingHttpMessageListener<>(streamingDecoder);
    }

    private ServerCallListener startListener(
            RpcInvocation invocation, MethodDescriptor methodDescriptor, Invoker<?> invoker) {
        switch (methodDescriptor.getRpcType()) {
            case UNARY:
                prepareUnaryServerCall();
                return new UnaryServerCallListener(invocation, invoker, responseObserver);
            case SERVER_STREAM:
                prepareStreamServerCall();
                return new ServerStreamServerCallListener(invocation, invoker, responseObserver);
            case BI_STREAM:
            case CLIENT_STREAM:
                prepareStreamServerCall();
                return new BiStreamServerCallListener(invocation, invoker, responseObserver);
            default:
                throw new IllegalStateException("Can not reach here");
        }
    }

    protected void prepareUnaryServerCall() {}

    protected void prepareStreamServerCall() {
        responseObserver = prepareResponseObserver(newStreamResponseObserver(h2StreamChannel));
    }

    protected ListeningDecoder<INPUT> newListeningDecoder(RpcInvocationBuildContext context) {
        return getMessageHandler()
                .createListeningDecoder(
                        context.getHttpMessageDecoder(),
                        context.getMethodMetadata().getActualRequestTypes());
    }

    @Override
    protected void initializeAltSvc(URL url) {
        if (Http3Exchanger.isEnabled(url)) {
            String value = "h3=\":" + url.getParameter(Constants.BIND_PORT_KEY, url.getPort()) + '"';
            responseObserver.addHeadersCustomizer((hs, t) -> hs.set(HttpHeaderNames.ALT_SVC.getKey(), value));
        }
    }

    @Override
    protected void onMetadataCompletion(Http2Header metadata) {
        responseObserver.setResponseEncoder(getContext().getHttpMessageEncoder());
        responseObserver.request(1);
        if (metadata.isEndStream()) {
            getStreamingDecoder().close();
        }
    }

    @Override
    protected void onDataCompletion(Http2InputMessage<INPUT> message) {
        if (message.isEndStream()) {
            getStreamingDecoder().close();
        }
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
    protected void onError(Http2InputMessage<INPUT> message, Throwable throwable) {
        try {
            message.close();
        } catch (Exception e) {
            throwable.addSuppressed(e);
        }
        onError(throwable);
    }

    @Override
    protected void onDataFinally(Http2InputMessage<INPUT> message) {}

    @Override
    public void cancelByRemote(long errorCode) {
        responseObserver.cancel(CancelStreamException.fromRemote(errorCode));
        if (serverCallListener != null) {
            serverCallListener.onCancel(errorCode);
        }
    }

    protected final StreamingDecoder<INPUT> getStreamingDecoder() {
        return streamingDecoder;
    }

    protected final Http2ServerChannelObserver<INPUT, OUTPUT> getResponseObserver() {
        return responseObserver;
    }

    @Override
    public void close() {
        responseObserver.close();
    }

    private static final class Http2StreamingDecodeListener implements ListeningDecoder.Listener {

        private final ServerCallListener serverCallListener;

        Http2StreamingDecodeListener(ServerCallListener serverCallListener) {
            this.serverCallListener = serverCallListener;
        }

        @Override
        public void onMessage(Object message) {
            serverCallListener.onMessage(message);
        }

        @Override
        public void onClose() {
            serverCallListener.onComplete();
        }
    }

    private static final class StreamingHttpMessageListener<INPUT> implements HttpMessageListener<INPUT> {

        private final StreamingDecoder<INPUT> streamingDecoder;

        StreamingHttpMessageListener(StreamingDecoder<INPUT> streamingDecoder) {
            this.streamingDecoder = streamingDecoder;
        }

        @Override
        public void onMessage(INPUT inputStream) {
            streamingDecoder.decode(inputStream);
        }
    }
}
