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
package org.apache.dubbo.remoting.http12.h2;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.threadpool.serial.SerializingExecutor;
import org.apache.dubbo.remoting.http12.AbstractServerTransportListener;
import org.apache.dubbo.remoting.http12.ServerCall;
import org.apache.dubbo.remoting.http12.UnaryServerCallListener;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.executor.ExecutorSupport;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import java.util.concurrent.Executor;

/**
 * @author icodening
 * @date 2023.06.13
 */
public class GenericHttp2ServerTransportListener extends AbstractServerTransportListener<Http2Header, Http2Message> implements Http2ServerTransportListener {

    private final Http2ChannelObserver responseObserver;

    private final ExecutorSupport executorSupport;

    protected SerializingExecutor serializingExecutor;

    public GenericHttp2ServerTransportListener(H2StreamChannel h2StreamChannel, URL url, FrameworkModel frameworkModel) {
        super(frameworkModel);
        this.responseObserver = new Http2ChannelObserver(h2StreamChannel);
        this.executorSupport = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel()).getExecutorSupport(url);
    }

    @Override
    public void onMetadata(Http2Header metadata) {
        if (serializingExecutor == null) {
            Executor executor = executorSupport.getExecutor(metadata);
            this.serializingExecutor = new SerializingExecutor(executor);
        }
        this.serializingExecutor.execute(() -> {
            if (metadata.isEndStream()) {
                return;
            }
            super.onMetadata(metadata);
            Http2ChannelObserver responseObserver = this.responseObserver;
            responseObserver.setHttpMessageCodec(getCodec());
        });
    }

    @Override
    public void onData(Http2Message message) {
        this.serializingExecutor.execute(() -> {
            super.onData(message);
            if (message.isEndStream()) {
                serverCallListener.onComplete();
            }
        });
    }

    @Override
    protected ServerCall.Listener startListener(RpcInvocation invocation,
                                                MethodDescriptor methodDescriptor,
                                                Invoker<?> invoker) {

        switch (methodDescriptor.getRpcType()) {
            case UNARY:
                return new UnaryServerCallListener(invocation, invoker, this.responseObserver);
            case SERVER_STREAM:
                return new ServerStreamServerCallListener(invocation, invoker, this.responseObserver);
            case BI_STREAM:
            case CLIENT_STREAM:
                break;
            default:
                throw new IllegalStateException("Can not reach here");
        }
        return null;
    }

    @Override
    public void cancelByRemote(long errorCode) {

    }
}
