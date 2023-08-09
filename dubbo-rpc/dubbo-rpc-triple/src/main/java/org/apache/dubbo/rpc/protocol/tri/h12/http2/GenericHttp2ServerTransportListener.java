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
import org.apache.dubbo.remoting.http12.BiStreamServerCallListener;
import org.apache.dubbo.remoting.http12.ServerCallListener;
import org.apache.dubbo.remoting.http12.ServerStreamServerCallListener;
import org.apache.dubbo.remoting.http12.UnaryServerCallListener;
import org.apache.dubbo.remoting.http12.h2.DefaultHttp2StreamingDecoder;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2ServerChannelObserver;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.JsonCodec;
import org.apache.dubbo.remoting.http12.message.ListeningDecoder;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.executor.ExecutorSupport;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.h12.AbstractServerTransportListener;

import java.util.concurrent.Executor;

public class GenericHttp2ServerTransportListener extends AbstractServerTransportListener<Http2Header, Http2InputMessage> implements Http2TransportListener {

    private final Http2ServerChannelObserver serverChannelObserver;

    private final H2StreamChannel h2StreamChannel;

    private final ExecutorSupport executorSupport;

    private ServerCallListener serverCallListener;

    public GenericHttp2ServerTransportListener(H2StreamChannel h2StreamChannel, URL url, FrameworkModel frameworkModel) {
        super(frameworkModel, h2StreamChannel);
        this.h2StreamChannel = h2StreamChannel;
        this.serverChannelObserver = new Http2ServerChannelObserver(h2StreamChannel);
        this.serverChannelObserver.setHttpMessageCodec(JsonCodec.INSTANCE);
        this.executorSupport = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel()).getExecutorSupport(url);
    }

    @Override
    protected Executor initializeExecutor(Http2Header metadata) {
        Executor executor = executorSupport.getExecutor(metadata);
        return new SerializingExecutor(executor);
    }

    private ServerCallListener startListener(RpcInvocation invocation,
                                               MethodDescriptor methodDescriptor,
                                               Invoker<?> invoker) {
        Http2ServerChannelObserver responseObserver = getServerChannelObserver();
        CancellationContext cancellationContext = RpcContext.getCancellationContext();
        responseObserver.setCancellationContext(cancellationContext);
        switch (methodDescriptor.getRpcType()) {
            case UNARY:
                return startUnary(invocation, invoker, responseObserver);
            case SERVER_STREAM:
                return startServerStreaming(invocation, invoker, responseObserver);
            case BI_STREAM:
            case CLIENT_STREAM:
                return startBiStreaming(invocation, invoker, responseObserver);
            default:
                throw new IllegalStateException("Can not reach here");
        }
    }

    public Http2ServerChannelObserver getServerChannelObserver() {
        return serverChannelObserver;
    }

    @Override
    public void cancelByRemote(long errorCode) {
        this.serverCallListener.onCancel(errorCode);
    }

    @Override
    protected ListeningDecoder newListeningDecoder(HttpMessageCodec codec, Class<?>[] actualRequestTypes) {
        StreamingDecoder streamingDecoder = newStreamingDecoder(codec, actualRequestTypes);
        initializeServerCallListener();
        streamingDecoder.setListener(new Http2StreamingDecodeListener(serverCallListener));
        getServerChannelObserver().setStreamingDecoder(streamingDecoder);
        return streamingDecoder;
    }


    protected StreamingDecoder newStreamingDecoder(HttpMessageCodec codec, Class<?>[] actualRequestTypes) {
        return new DefaultHttp2StreamingDecoder(codec, actualRequestTypes);
    }

    protected void doOnMetadata(Http2Header metadata) {
        if (metadata.isEndStream()) {
            return;
        }
        super.doOnMetadata(metadata);
    }

    @Override
    protected void onMetadataCompletion(Http2Header metadata) {
        super.onMetadataCompletion(metadata);
        this.serverChannelObserver.setHttpMessageCodec(getHttpMessageCodec());
        this.serverChannelObserver.request(1);
    }

    @Override
    protected void onDataCompletion(Http2InputMessage message) {
        if (message.isEndStream()) {
            serverCallListener.onComplete();
        }
    }

    @Override
    protected void onError(Throwable throwable) {
        serverChannelObserver.onError(throwable);
    }

    protected StreamingDecoder getStreamingDecoder() {
        return (StreamingDecoder) this.getListeningDecoder();
    }

    private static class Http2StreamingDecodeListener implements ListeningDecoder.Listener {

        private final ServerCallListener serverCallListener;

        private Http2StreamingDecodeListener(ServerCallListener serverCallListener) {
            this.serverCallListener = serverCallListener;
        }

        @Override
        public void onMessage(Object message) {
            this.serverCallListener.onMessage(message);
        }

        @Override
        public void onClose() {
            this.serverCallListener.onComplete();
        }
    }

    private void initializeServerCallListener() {
        this.serverCallListener = startListener(getRpcInvocation(), getMethodDescriptor(), getInvoker());
    }

    private UnaryServerCallListener startUnary(RpcInvocation invocation,
                                               Invoker<?> invoker,
                                               Http2ServerChannelObserver responseObserver) {
        return new UnaryServerCallListener(invocation, invoker, responseObserver);
    }

    private ServerStreamServerCallListener startServerStreaming(RpcInvocation invocation, Invoker<?> invoker, Http2ServerChannelObserver responseObserver) {
        return new ServerStreamServerCallListener(invocation, invoker, responseObserver);
    }

    private BiStreamServerCallListener startBiStreaming(RpcInvocation invocation, Invoker<?> invoker, Http2ServerChannelObserver responseObserver) {
        return new BiStreamServerCallListener(invocation, invoker, responseObserver);
    }
}
