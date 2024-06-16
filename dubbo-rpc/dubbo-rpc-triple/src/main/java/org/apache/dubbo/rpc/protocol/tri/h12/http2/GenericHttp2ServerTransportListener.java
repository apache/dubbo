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
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.threadpool.serial.SerializingExecutor;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.http12.h2.CancelStreamException;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2ServerChannelObserver;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.message.DefaultListeningDecoder;
import org.apache.dubbo.remoting.http12.message.DefaultStreamingDecoder;
import org.apache.dubbo.remoting.http12.message.ListeningDecoder;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.remoting.http12.message.codec.JsonCodec;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.executor.ExecutorSupport;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.ReflectionPackableMethod;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.h12.AbstractServerTransportListener;
import org.apache.dubbo.rpc.protocol.tri.h12.BiStreamServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.HttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.ServerStreamServerCallListener;
import org.apache.dubbo.rpc.protocol.tri.h12.StreamingHttpMessageListener;
import org.apache.dubbo.rpc.protocol.tri.h12.UnaryServerCallListener;

import java.util.concurrent.Executor;

public class GenericHttp2ServerTransportListener extends AbstractServerTransportListener<Http2Header, Http2InputMessage>
        implements Http2TransportListener {

    private final ExecutorSupport executorSupport;
    private final StreamingDecoder streamingDecoder;
    private final FrameworkModel frameworkModel;
    private final H2StreamChannel h2StreamChannel;
    private Http2ServerChannelObserver serverChannelObserver;
    private ServerCallListener serverCallListener;

    public GenericHttp2ServerTransportListener(
            H2StreamChannel h2StreamChannel, URL url, FrameworkModel frameworkModel) {
        super(frameworkModel, url, h2StreamChannel);
        executorSupport = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel())
                .getExecutorSupport(url);
        streamingDecoder = newStreamingDecoder();
        serverChannelObserver = newHttp2ServerChannelObserver(frameworkModel, h2StreamChannel);
        serverChannelObserver.setResponseEncoder(JsonCodec.INSTANCE);
        serverChannelObserver.setStreamingDecoder(streamingDecoder);
        this.frameworkModel = frameworkModel;
        this.h2StreamChannel = h2StreamChannel;
    }

    protected StreamingDecoder newStreamingDecoder() {
        return new DefaultStreamingDecoder();
    }

    protected Http2ServerChannelObserver newHttp2ServerChannelObserver(
            FrameworkModel frameworkModel, H2StreamChannel h2StreamChannel) {
        return new Http2ServerCallToObserverAdapter(frameworkModel, h2StreamChannel);
    }

    @Override
    protected Executor initializeExecutor(Http2Header metadata) {
        return new SerializingExecutor(executorSupport.getExecutor(metadata));
    }

    @Override
    protected HttpMessageListener buildHttpMessageListener() {
        RpcInvocationBuildContext context = getContext();
        RpcInvocation rpcInvocation = buildRpcInvocation(context);

        serverCallListener = startListener(rpcInvocation, context.getMethodDescriptor(), context.getInvoker());

        DefaultListeningDecoder listeningDecoder = new DefaultListeningDecoder(
                context.getHttpMessageDecoder(), context.getMethodMetadata().getActualRequestTypes());
        listeningDecoder.setListener(new Http2StreamingDecodeListener(serverCallListener));
        streamingDecoder.setFragmentListener(new StreamingDecoder.DefaultFragmentListener(listeningDecoder));
        getServerChannelObserver().setStreamingDecoder(streamingDecoder);
        return new StreamingHttpMessageListener(streamingDecoder);
    }

    private ServerCallListener startListener(
            RpcInvocation invocation, MethodDescriptor methodDescriptor, Invoker<?> invoker) {
        switch (methodDescriptor.getRpcType()) {
            case UNARY:
                onUnary();
                boolean applyCustomizeException = false;
                if (!getContext().isHasStub()) {
                    MethodMetadata methodMetadata = getContext().getMethodMetadata();
                    applyCustomizeException = ReflectionPackableMethod.needWrap(
                            methodDescriptor,
                            methodMetadata.getActualRequestTypes(),
                            methodMetadata.getActualResponseType());
                }
                onListenerStart();
                UnaryServerCallListener unaryServerCallListener =
                        startUnary(invocation, invoker, getServerChannelObserver());
                unaryServerCallListener.setApplyCustomizeException(applyCustomizeException);
                return unaryServerCallListener;
            case SERVER_STREAM:
                onListenerStart();
                return startServerStreaming(invocation, invoker, getServerChannelObserver());
            case BI_STREAM:
            case CLIENT_STREAM:
                onListenerStart();
                return startBiStreaming(invocation, invoker, getServerChannelObserver());
            default:
                throw new IllegalStateException("Can not reach here");
        }
    }

    protected void onUnary() {
        serverChannelObserver = new Http2ServerUnaryChannelObserver(frameworkModel, h2StreamChannel);
        serverChannelObserver.setResponseEncoder(JsonCodec.INSTANCE);
        serverChannelObserver.setStreamingDecoder(streamingDecoder);
    }

    protected void onListenerStart() {
        CancellationContext cancellationContext = RpcContext.getCancellationContext();
        serverChannelObserver.setCancellationContext(cancellationContext);
    }

    private UnaryServerCallListener startUnary(
            RpcInvocation invocation, Invoker<?> invoker, Http2ServerChannelObserver responseObserver) {
        return new UnaryServerCallListener(invocation, invoker, responseObserver);
    }

    private ServerStreamServerCallListener startServerStreaming(
            RpcInvocation invocation, Invoker<?> invoker, Http2ServerChannelObserver responseObserver) {
        return new ServerStreamServerCallListener(invocation, invoker, responseObserver);
    }

    private BiStreamServerCallListener startBiStreaming(
            RpcInvocation invocation, Invoker<?> invoker, Http2ServerChannelObserver responseObserver) {
        return new BiStreamServerCallListener(invocation, invoker, responseObserver);
    }

    @Override
    protected void initializeAltSvc(URL url) {
        if (TripleProtocol.isHttp3Enabled(url)) {
            int bindPort = url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
            serverChannelObserver.setAltSvc("h3=\":" + bindPort + "\"");
        }
    }

    @Override
    protected void onMetadataCompletion(Http2Header metadata) {
        serverChannelObserver.setResponseEncoder(getContext().getHttpMessageEncoder());
        serverChannelObserver.request(1);
        if (metadata.isEndStream()) {
            getStreamingDecoder().close();
        }
    }

    @Override
    protected void onDataCompletion(Http2InputMessage message) {
        if (message.isEndStream()) {
            getStreamingDecoder().close();
        }
    }

    @Override
    protected void onError(Throwable throwable) {
        serverChannelObserver.onError(throwable);
    }

    @Override
    protected void onError(Http2InputMessage message, Throwable throwable) {
        try {
            message.close();
        } catch (Exception e) {
            throwable.addSuppressed(e);
        }
        onError(throwable);
    }

    @Override
    protected void onFinally(Http2InputMessage message) {}

    @Override
    public void cancelByRemote(long errorCode) {
        serverChannelObserver.cancel(CancelStreamException.fromRemote(errorCode));
        serverCallListener.onCancel(errorCode);
    }

    protected StreamingDecoder getStreamingDecoder() {
        return streamingDecoder;
    }

    protected final Http2ServerChannelObserver getServerChannelObserver() {
        return serverChannelObserver;
    }

    @Override
    public void close() throws Exception {
        getServerChannelObserver().close();
    }

    private static class Http2StreamingDecodeListener implements ListeningDecoder.Listener {

        private final ServerCallListener serverCallListener;

        private Http2StreamingDecodeListener(ServerCallListener serverCallListener) {
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
}
