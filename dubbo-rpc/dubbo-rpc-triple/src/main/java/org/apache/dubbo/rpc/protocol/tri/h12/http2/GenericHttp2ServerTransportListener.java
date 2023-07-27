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
import org.apache.dubbo.remoting.http12.ServerCallListener;
import org.apache.dubbo.remoting.http12.ServerStreamServerCallListener;
import org.apache.dubbo.remoting.http12.UnaryServerCallListener;
import org.apache.dubbo.remoting.http12.h2.BiStreamServerCallListener;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2ChannelObserver;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2Message;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.executor.ExecutorSupport;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.h12.AbstractServerTransportListener;

import java.io.InputStream;
import java.util.concurrent.Executor;

public class GenericHttp2ServerTransportListener extends AbstractServerTransportListener<Http2Header, Http2Message> implements Http2TransportListener {

    protected final Http2ChannelObserver responseObserver;

    protected final H2StreamChannel h2StreamChannel;

    private final ExecutorSupport executorSupport;

    protected SerializingExecutor serializingExecutor;

    public GenericHttp2ServerTransportListener(H2StreamChannel h2StreamChannel, URL url, FrameworkModel frameworkModel) {
        super(frameworkModel);
        this.h2StreamChannel = h2StreamChannel;
        this.responseObserver = new Http2ChannelObserver(h2StreamChannel);
        this.executorSupport = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel()).getExecutorSupport(url);
    }

    @Override
    public void onMetadata(Http2Header metadata) {
        if (serializingExecutor == null) {
            Executor executor = executorSupport.getExecutor(metadata);
            this.serializingExecutor = new SerializingExecutor(executor);
        }
        this.serializingExecutor.execute(() -> doOnMetadata(metadata));
    }

    @Override
    public void onData(Http2Message message) {
        this.serializingExecutor.execute(() -> doOnData(message));
    }

    @Override
    public H2StreamChannel getHttpChannel() {
        return this.h2StreamChannel;
    }

    @Override
    protected ServerCallListener startListener(RpcInvocation invocation,
                                               MethodDescriptor methodDescriptor,
                                               Invoker<?> invoker) {
        Http2ChannelObserver responseObserver = getResponseObserver();
        configurerResponseObserver(responseObserver);
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

    protected void configurerResponseObserver(Http2ChannelObserver responseObserver) {

    }

    public Http2ChannelObserver getResponseObserver() {
        return responseObserver;
    }

    @Override
    public void cancelByRemote(long errorCode) {
        this.serverCallListener.onCancel((int) errorCode);
    }

    protected void doOnMetadata(Http2Header metadata) {
        if (metadata.isEndStream()) {
            return;
        }
        super.onMetadata(metadata);
        Http2ChannelObserver responseObserver = this.responseObserver;
        responseObserver.setHttpMessageCodec(getCodec());
    }

    protected void doOnData(Http2Message message) {
        try {
            InputStream body = message.getBody();
            if (body.available() != 0) {
                super.onData(message);
            }
            if (message.isEndStream()) {
                serverCallListener.onComplete();
            }
        } catch (Throwable e) {
            this.responseObserver.onError(e);
        }
    }

    protected UnaryServerCallListener startUnary(RpcInvocation invocation,
                                                 Invoker<?> invoker,
                                                 Http2ChannelObserver responseObserver) {
        return new UnaryServerCallListener(invocation, invoker, responseObserver);
    }

    protected ServerCallListener startServerStreaming(RpcInvocation invocation, Invoker<?> invoker, Http2ChannelObserver responseObserver) {
        return new ServerStreamServerCallListener(invocation, invoker, responseObserver);
    }

    protected BiStreamServerCallListener startBiStreaming(RpcInvocation invocation, Invoker<?> invoker, Http2ChannelObserver responseObserver) {
        return new BiStreamServerCallListener(invocation, invoker, responseObserver);
    }
}
