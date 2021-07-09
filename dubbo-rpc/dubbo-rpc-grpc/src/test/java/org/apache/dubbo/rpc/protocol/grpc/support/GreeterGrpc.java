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

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * The greeting service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.34.1)",
    comments = "Source: helloworld.proto")
public final class GreeterGrpc {

    private GreeterGrpc() {
    }

    public static final String SERVICE_NAME = "helloworld.Greeter";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest,
        org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> getSayHelloMethod;

    @io.grpc.stub.annotations.RpcMethod(
        fullMethodName = SERVICE_NAME + '/' + "SayHello",
        requestType = org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest.class,
        responseType = org.apache.dubbo.rpc.protocol.grpc.support.HelloReply.class,
        methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest,
        org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> getSayHelloMethod() {
        io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest, org.apache.dubbo.rpc.protocol.grpc.support.HelloReply>
            getSayHelloMethod;
        if ((getSayHelloMethod = org.apache.dubbo.rpc.protocol.grpc.support.GreeterGrpc.getSayHelloMethod) == null) {
            synchronized (org.apache.dubbo.rpc.protocol.grpc.support.GreeterGrpc.class) {
                if ((getSayHelloMethod = org.apache.dubbo.rpc.protocol.grpc.support.GreeterGrpc.getSayHelloMethod) == null) {
                    org.apache.dubbo.rpc.protocol.grpc.support.GreeterGrpc.getSayHelloMethod = getSayHelloMethod =
                        io.grpc.MethodDescriptor.<org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest, org.apache.dubbo.rpc.protocol.grpc.support.HelloReply>newBuilder()
                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                            .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SayHello"))
                            .setSampledToLocalTracing(true)
                            .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest.getDefaultInstance()))
                            .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                org.apache.dubbo.rpc.protocol.grpc.support.HelloReply.getDefaultInstance()))
                            .setSchemaDescriptor(new GreeterMethodDescriptorSupplier("SayHello"))
                            .build();
                }
            }
        }
        return getSayHelloMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static GreeterStub newStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<GreeterStub> factory =
            new io.grpc.stub.AbstractStub.StubFactory<GreeterStub>() {
                @Override
                public GreeterStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                    return new GreeterStub(channel, callOptions);
                }
            };
        return GreeterStub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static GreeterBlockingStub newBlockingStub(
        io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<GreeterBlockingStub> factory =
            new io.grpc.stub.AbstractStub.StubFactory<GreeterBlockingStub>() {
                @Override
                public GreeterBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                    return new GreeterBlockingStub(channel, callOptions);
                }
            };
        return GreeterBlockingStub.newStub(factory, channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static GreeterFutureStub newFutureStub(
        io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<GreeterFutureStub> factory =
            new io.grpc.stub.AbstractStub.StubFactory<GreeterFutureStub>() {
                @Override
                public GreeterFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                    return new GreeterFutureStub(channel, callOptions);
                }
            };
        return GreeterFutureStub.newStub(factory, channel);
    }

    /**
     * <pre>
     * The greeting service definition.
     * </pre>
     */
    public static abstract class GreeterImplBase implements io.grpc.BindableService {

        /**
         * <pre>
         * Sends a greeting
         * </pre>
         */
        public void sayHello(org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request,
                             io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> responseObserver) {
            asyncUnimplementedUnaryCall(getSayHelloMethod(), responseObserver);
        }

        @Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                .addMethod(
                    getSayHelloMethod(),
                    asyncUnaryCall(
                        new MethodHandlers<
                            org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest,
                            org.apache.dubbo.rpc.protocol.grpc.support.HelloReply>(
                            this, METHODID_SAY_HELLO)))
                .build();
        }
    }

    /**
     * <pre>
     * The greeting service definition.
     * </pre>
     */
    public static final class GreeterStub extends io.grpc.stub.AbstractAsyncStub<GreeterStub> {
        private GreeterStub(
            io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected GreeterStub build(
            io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new GreeterStub(channel, callOptions);
        }

        /**
         * <pre>
         * Sends a greeting
         * </pre>
         */
        public void sayHello(org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request,
                             io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> responseObserver) {
            asyncUnaryCall(
                getChannel().newCall(getSayHelloMethod(), getCallOptions()), request, responseObserver);
        }
    }

    /**
     * <pre>
     * The greeting service definition.
     * </pre>
     */
    public static final class GreeterBlockingStub extends io.grpc.stub.AbstractBlockingStub<GreeterBlockingStub> {
        private GreeterBlockingStub(
            io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected GreeterBlockingStub build(
            io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new GreeterBlockingStub(channel, callOptions);
        }

        /**
         * <pre>
         * Sends a greeting
         * </pre>
         */
        public org.apache.dubbo.rpc.protocol.grpc.support.HelloReply sayHello(
            org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request) {
            return blockingUnaryCall(
                getChannel(), getSayHelloMethod(), getCallOptions(), request);
        }
    }

    /**
     * <pre>
     * The greeting service definition.
     * </pre>
     */
    public static final class GreeterFutureStub extends io.grpc.stub.AbstractFutureStub<GreeterFutureStub> {
        private GreeterFutureStub(
            io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected GreeterFutureStub build(
            io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new GreeterFutureStub(channel, callOptions);
        }

        /**
         * <pre>
         * Sends a greeting
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.support.HelloReply> sayHello(
            org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest request) {
            return futureUnaryCall(
                getChannel().newCall(getSayHelloMethod(), getCallOptions()), request);
        }
    }

    private static final int METHODID_SAY_HELLO = 0;

    private static final class MethodHandlers<Req, Resp> implements
        io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
        io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
        io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
        io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
        private final GreeterImplBase serviceImpl;
        private final int methodId;

        MethodHandlers(GreeterImplBase serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
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
        public io.grpc.stub.StreamObserver<Req> invoke(
            io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                default:
                    throw new AssertionError();
            }
        }
    }

    private static abstract class GreeterBaseDescriptorSupplier
        implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
        GreeterBaseDescriptorSupplier() {
        }

        @Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return org.apache.dubbo.rpc.protocol.grpc.support.HelloWorldProto.getDescriptor();
        }

        @Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("Greeter");
        }
    }

    private static final class GreeterFileDescriptorSupplier
        extends GreeterBaseDescriptorSupplier {
        GreeterFileDescriptorSupplier() {
        }
    }

    private static final class GreeterMethodDescriptorSupplier
        extends GreeterBaseDescriptorSupplier
        implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
        private final String methodName;

        GreeterMethodDescriptorSupplier(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
            return getServiceDescriptor().findMethodByName(methodName);
        }
    }

    private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

    public static io.grpc.ServiceDescriptor getServiceDescriptor() {
        io.grpc.ServiceDescriptor result = serviceDescriptor;
        if (result == null) {
            synchronized (org.apache.dubbo.rpc.protocol.grpc.support.GreeterGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                        .setSchemaDescriptor(new GreeterFileDescriptorSupplier())
                        .addMethod(getSayHelloMethod())
                        .build();
                }
            }
        }
        return result;
    }
}
