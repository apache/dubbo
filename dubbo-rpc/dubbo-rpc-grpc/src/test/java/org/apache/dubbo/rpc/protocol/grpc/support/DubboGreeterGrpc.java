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

package org.apache.dubbo.rpc.protocol.grpc.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ReferenceConfigBase;

import java.util.concurrent.TimeUnit;

import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.rpc.protocol.grpc.support.GreeterGrpc.getServiceDescriptor;

@javax.annotation.Generated(
    value = "by DubboGrpc generator",
    comments = "Source: helloworld.proto")
public final class DubboGreeterGrpc {
    private DubboGreeterGrpc() {
    }

    public static class DubboGreeterStub implements IGreeter {

        protected URL url;
        protected ReferenceConfigBase<?> referenceConfig;

        protected GreeterGrpc.GreeterBlockingStub blockingStub;
        protected GreeterGrpc.GreeterFutureStub futureStub;
        protected GreeterGrpc.GreeterStub stub;

        public DubboGreeterStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions, URL url,
                                ReferenceConfigBase<?> referenceConfig) {
            this.url = url;
            this.referenceConfig = referenceConfig;

            blockingStub = GreeterGrpc.newBlockingStub(channel).build(channel, callOptions);
            futureStub = GreeterGrpc.newFutureStub(channel).build(channel, callOptions);
            stub = GreeterGrpc.newStub(channel).build(channel, callOptions);
        }

        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
        public org.apache.dubbo.rpc.protocol.grpc.support.HelloReply sayHello(
            org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request) {
            return blockingStub
                .withDeadlineAfter(url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT), TimeUnit.MILLISECONDS)
                .sayHello(request);
        }

        public com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> sayHelloAsync(
            org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request) {
            return futureStub
                .withDeadlineAfter(url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT), TimeUnit.MILLISECONDS)
                .sayHello(request);
        }

        public void sayHello(org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request,
                             io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> responseObserver) {
            stub
                .withDeadlineAfter(url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT), TimeUnit.MILLISECONDS)
                .sayHello(request, responseObserver);
        }

    }

    public static DubboGreeterStub getDubboStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions, URL url,
                                                ReferenceConfigBase<?> referenceConfig) {
        return new DubboGreeterStub(channel, callOptions, url, referenceConfig);
    }

    public interface IGreeter {
        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
        default public org.apache.dubbo.rpc.protocol.grpc.support.HelloReply sayHello(
            org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request) {
            throw new UnsupportedOperationException(
                "No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
        default public com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> sayHelloAsync(
            org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request) {
            throw new UnsupportedOperationException(
                "No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
        public void sayHello(org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request,
                             io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> responseObserver);

    }

    /**
     * <pre>
     *  The greeting service definition.
     * </pre>
     */
    public static abstract class GreeterImplBase implements io.grpc.BindableService, IGreeter {

        private IGreeter proxiedImpl;

        public final void setProxiedImpl(IGreeter proxiedImpl) {
            this.proxiedImpl = proxiedImpl;
        }

        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
        @Override
        public final org.apache.dubbo.rpc.protocol.grpc.support.HelloReply sayHello(
            org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request) {
            throw new UnsupportedOperationException(
                "No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        /**
         * <pre>
         *  Sends a greeting
         * </pre>
         */
        @Override
        public final com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> sayHelloAsync(
            org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request) {
            throw new UnsupportedOperationException(
                "No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        public void sayHello(org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request,
                             io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> responseObserver) {
            asyncUnimplementedUnaryCall(org.apache.dubbo.rpc.protocol.grpc.support.GreeterGrpc.getSayHelloMethod(), responseObserver);
        }

        @Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                .addMethod(
                    org.apache.dubbo.rpc.protocol.grpc.support.GreeterGrpc.getSayHelloMethod(),
                    asyncUnaryCall(
                        new MethodHandlers<
                            org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest,
                            org.apache.dubbo.rpc.protocol.grpc.support.HelloReply>(
                            proxiedImpl, METHODID_SAY_HELLO)))
                .build();
        }
    }

    private static final int METHODID_SAY_HELLO = 0;

    private static final class MethodHandlers
        <Req, Resp> implements
        io.grpc.stub.ServerCalls.UnaryMethod
            <Req, Resp>,
        io.grpc.stub.ServerCalls.ServerStreamingMethod
            <Req, Resp>,
        io.grpc.stub.ServerCalls.ClientStreamingMethod
            <Req, Resp>,
        io.grpc.stub.ServerCalls.BidiStreamingMethod
            <Req, Resp> {
        private final IGreeter serviceImpl;
        private final int methodId;

        MethodHandlers(IGreeter serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver
            <Resp> responseObserver) {
            switch (methodId) {
                case METHODID_SAY_HELLO:
                    serviceImpl.sayHello((org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest) request,
                        (io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.support.HelloReply>) responseObserver);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver
            <Req> invoke(io.grpc.stub.StreamObserver
                             <Resp> responseObserver) {
            switch (methodId) {
                default:
                    throw new AssertionError();
            }
        }
    }

}
