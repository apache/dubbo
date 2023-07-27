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
import org.apache.dubbo.remoting.http12.HttpMessage;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.remoting.http12.ServerCallListener;
import org.apache.dubbo.remoting.http12.ServerStreamServerCallListener;
import org.apache.dubbo.remoting.http12.UnaryServerCallListener;
import org.apache.dubbo.remoting.http12.h1.Http1ChannelObserver;
import org.apache.dubbo.remoting.http12.h1.Http1ServerStreamChannelObserver;
import org.apache.dubbo.remoting.http12.h1.Http1ServerTransportListener;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.h12.AbstractServerTransportListener;

public class DefaultHttp11ServerTransportListener extends AbstractServerTransportListener<RequestMetadata, HttpMessage> implements Http1ServerTransportListener {

    private final HttpChannel httpChannel;

    private final URL url;

    public DefaultHttp11ServerTransportListener(HttpChannel httpChannel, URL url, FrameworkModel frameworkModel) {
        super(frameworkModel);
        this.url = url;
        this.httpChannel = httpChannel;
    }

    @Override
    protected ServerCallListener startListener(RpcInvocation invocation,
                                               MethodDescriptor methodDescriptor,
                                               Invoker<?> invoker) {
        switch (methodDescriptor.getRpcType()) {
            case UNARY:
                Http1ChannelObserver http1ChannelObserver = new Http1ChannelObserver(httpChannel, getCodec());
                return new AutoCompleteUnaryServerCallListener(invocation, invoker, http1ChannelObserver);
            case SERVER_STREAM:
                Http1ChannelObserver serverStreamChannelObserver = new Http1ServerStreamChannelObserver(httpChannel, getCodec());
                return new AutoCompleteServerStreamServerCallListener(invocation, invoker, serverStreamChannelObserver);
            default:
                throw new UnsupportedOperationException("HTTP1.x only support unary and server stream");
        }
    }

    @Override
    public HttpChannel getHttpChannel() {
        return this.httpChannel;
    }

    private static class AutoCompleteUnaryServerCallListener extends UnaryServerCallListener {

        public AutoCompleteUnaryServerCallListener(RpcInvocation invocation, Invoker<?> invoker, StreamObserver<Object> responseObserver) {
            super(invocation, invoker, responseObserver);
        }

        @Override
        public void onMessage(Object message) {
            super.onMessage(message);
            super.onComplete();
        }
    }

    private static class AutoCompleteServerStreamServerCallListener extends ServerStreamServerCallListener {

        public AutoCompleteServerStreamServerCallListener(RpcInvocation invocation, Invoker<?> invoker, StreamObserver<Object> responseObserver) {
            super(invocation, invoker, responseObserver);
        }

        @Override
        public void onMessage(Object message) {
            super.onMessage(message);
            super.onComplete();
        }
    }
}
