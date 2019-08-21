package org.apache.dubbo.rpc.protocol.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.*;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.*;

/**
 *
 */
@javax.annotation.Generated(
        value = "by gRPC proto compiler (version 1.19.0-SNAPSHOT)",
        comments = "Source: src/main/java/proto/HelloService.proto")
public final class GreeterGrpc {

    private GreeterGrpc() {
    }

    public static final String SERVICE_NAME = "helloworld.Greeter";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getHelloWorldMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "HelloWorld",
            requestType = org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest.class,
            responseType = org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getHelloWorldMethod() {
        io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest, org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getHelloWorldMethod;
        if ((getHelloWorldMethod = GreeterGrpc.getHelloWorldMethod) == null) {
            synchronized (GreeterGrpc.class) {
                if ((getHelloWorldMethod = GreeterGrpc.getHelloWorldMethod) == null) {
                    GreeterGrpc.getHelloWorldMethod = getHelloWorldMethod =
                            io.grpc.MethodDescriptor.<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest, org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "helloworld.Greeter", "HelloWorld"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse.getDefaultInstance()))
                                    .setSchemaDescriptor(new GreeterMethodDescriptorSupplier("HelloWorld"))
                                    .build();
                }
            }
        }
        return getHelloWorldMethod;
    }

    private static volatile io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getRequestStreamMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "RequestStream",
            requestType = org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest.class,
            responseType = org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse.class,
            methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
    public static io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getRequestStreamMethod() {
        io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest, org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getRequestStreamMethod;
        if ((getRequestStreamMethod = GreeterGrpc.getRequestStreamMethod) == null) {
            synchronized (GreeterGrpc.class) {
                if ((getRequestStreamMethod = GreeterGrpc.getRequestStreamMethod) == null) {
                    GreeterGrpc.getRequestStreamMethod = getRequestStreamMethod =
                            io.grpc.MethodDescriptor.<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest, org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
                                    .setFullMethodName(generateFullMethodName(
                                            "helloworld.Greeter", "RequestStream"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse.getDefaultInstance()))
                                    .setSchemaDescriptor(new GreeterMethodDescriptorSupplier("RequestStream"))
                                    .build();
                }
            }
        }
        return getRequestStreamMethod;
    }

    private static volatile io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getResponseStreamMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "ResponseStream",
            requestType = org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest.class,
            responseType = org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getResponseStreamMethod() {
        io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest, org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getResponseStreamMethod;
        if ((getResponseStreamMethod = GreeterGrpc.getResponseStreamMethod) == null) {
            synchronized (GreeterGrpc.class) {
                if ((getResponseStreamMethod = GreeterGrpc.getResponseStreamMethod) == null) {
                    GreeterGrpc.getResponseStreamMethod = getResponseStreamMethod =
                            io.grpc.MethodDescriptor.<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest, org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "helloworld.Greeter", "ResponseStream"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse.getDefaultInstance()))
                                    .setSchemaDescriptor(new GreeterMethodDescriptorSupplier("ResponseStream"))
                                    .build();
                }
            }
        }
        return getResponseStreamMethod;
    }

    private static volatile io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getAllStreamMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "AllStream",
            requestType = org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest.class,
            responseType = org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse.class,
            methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
    public static io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getAllStreamMethod() {
        io.grpc.MethodDescriptor<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest, org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> getAllStreamMethod;
        if ((getAllStreamMethod = GreeterGrpc.getAllStreamMethod) == null) {
            synchronized (GreeterGrpc.class) {
                if ((getAllStreamMethod = GreeterGrpc.getAllStreamMethod) == null) {
                    GreeterGrpc.getAllStreamMethod = getAllStreamMethod =
                            io.grpc.MethodDescriptor.<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest, org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
                                    .setFullMethodName(generateFullMethodName(
                                            "helloworld.Greeter", "AllStream"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse.getDefaultInstance()))
                                    .setSchemaDescriptor(new GreeterMethodDescriptorSupplier("AllStream"))
                                    .build();
                }
            }
        }
        return getAllStreamMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static GreeterStub newStub(io.grpc.Channel channel) {
        return new GreeterStub(channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static GreeterBlockingStub newBlockingStub(
            io.grpc.Channel channel) {
        return new GreeterBlockingStub(channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static GreeterFutureStub newFutureStub(
            io.grpc.Channel channel) {
        return new GreeterFutureStub(channel);
    }

    /**
     *
     */
    public static final class GreeterStub extends io.grpc.stub.AbstractStub<GreeterStub> {
        private GreeterStub(io.grpc.Channel channel) {
            super(channel);
        }

        private GreeterStub(io.grpc.Channel channel,
                            io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected GreeterStub build(io.grpc.Channel channel,
                                    io.grpc.CallOptions callOptions) {
            return new GreeterStub(channel, callOptions);
        }

        /**
         *
         */
        public void helloWorld(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request,
                               io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getHelloWorldMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         *
         */
        public io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest> requestStream(
                io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            return asyncClientStreamingCall(
                    getChannel().newCall(getRequestStreamMethod(), getCallOptions()), responseObserver);
        }

        /**
         *
         */
        public void responseStream(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request,
                                   io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getResponseStreamMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         *
         */
        public io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest> allStream(
                io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            return asyncBidiStreamingCall(
                    getChannel().newCall(getAllStreamMethod(), getCallOptions()), responseObserver);
        }
    }

    /**
     *
     */
    public static final class GreeterBlockingStub extends io.grpc.stub.AbstractStub<GreeterBlockingStub> {
        private GreeterBlockingStub(io.grpc.Channel channel) {
            super(channel);
        }

        private GreeterBlockingStub(io.grpc.Channel channel,
                                    io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected GreeterBlockingStub build(io.grpc.Channel channel,
                                            io.grpc.CallOptions callOptions) {
            return new GreeterBlockingStub(channel, callOptions);
        }

        /**
         *
         */
        public org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse helloWorld(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            return blockingUnaryCall(
                    getChannel(), getHelloWorldMethod(), getCallOptions(), request);
        }

        /**
         *
         */
        public org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse responseStream(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            return blockingUnaryCall(
                    getChannel(), getResponseStreamMethod(), getCallOptions(), request);
        }
    }

    /**
     *
     */
    public static final class GreeterFutureStub extends io.grpc.stub.AbstractStub<GreeterFutureStub> {
        private GreeterFutureStub(io.grpc.Channel channel) {
            super(channel);
        }

        private GreeterFutureStub(io.grpc.Channel channel,
                                  io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected GreeterFutureStub build(io.grpc.Channel channel,
                                          io.grpc.CallOptions callOptions) {
            return new GreeterFutureStub(channel, callOptions);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> helloWorld(
                org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getHelloWorldMethod(), getCallOptions()), request);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseStream(
                org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getResponseStreamMethod(), getCallOptions()), request);
        }
    }

    public static abstract class GreeterImplBase implements io.grpc.BindableService, IGreeter {

        @Override
        public final org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse helloWorld(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        @Override
        public final com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> helloWorldAsync(
                org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        public void helloWorld(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request,
                               io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            asyncUnimplementedUnaryCall(getHelloWorldMethod(), responseObserver);
        }

        public io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest> requestStream(
                io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            return asyncUnimplementedStreamingCall(getRequestStreamMethod(), responseObserver);
        }

        @Override
        public final org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse responseStream(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        @Override
        public final com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseStreamAsync(
                org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        public void responseStream(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request,
                                   io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            asyncUnimplementedUnaryCall(getResponseStreamMethod(), responseObserver);
        }

        public io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest> allStream(
                io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            return asyncUnimplementedStreamingCall(getAllStreamMethod(), responseObserver);
        }


        @Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                    .addMethod(
                            getHelloWorldMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>(
                                            this, METHODID_HELLO_WORLD)))
                    .addMethod(
                            getRequestStreamMethod(),
                            asyncClientStreamingCall(
                                    new MethodHandlers<
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>(
                                            this, METHODID_REQUEST_STREAM)))
                    .addMethod(
                            getResponseStreamMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>(
                                            this, METHODID_RESPONSE_STREAM)))
                    .addMethod(
                            getAllStreamMethod(),
                            asyncBidiStreamingCall(
                                    new MethodHandlers<
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest,
                                            org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>(
                                            this, METHODID_ALL_STREAM)))
                    .build();
        }
    }

    /**
     * Code generated for Dubbo
     */
    public interface IGreeter {

        default public org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse helloWorld(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        default public com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> helloWorldAsync(
                org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        public void helloWorld(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request,
                               io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver);

        public io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest> requestStream(
                io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver);

        default public org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse responseStream(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        default public com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseStreamAsync(
                org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            throw new UnsupportedOperationException("No need to override this method, extend XxxImplBase and override all methods it allows.");
        }

        public void responseStream(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request,
                                   io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver);

        public io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest> allStream(
                io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver);

    }

    public static class DubboGreeterStub implements IGreeter {

        private GreeterBlockingStub blockingStub;
        private GreeterFutureStub futureStub;
        private GreeterStub stub;

        public DubboGreeterStub(io.grpc.Channel channel) {
            blockingStub = GreeterGrpc.newBlockingStub(channel);
            futureStub = GreeterGrpc.newFutureStub(channel);
            stub = GreeterGrpc.newStub(channel);
        }

        public org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse helloWorld(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            return blockingStub.helloWorld(request);
        }

        public com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> helloWorldAsync(
                org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            return futureStub.helloWorld(request);
        }

        public void helloWorld(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request,
                               io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            stub.helloWorld(request, responseObserver);
        }

        public io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest> requestStream(
                io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            return stub.requestStream(responseObserver);
        }

        public org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse responseStream(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            return blockingStub.responseStream(request);
        }

        public com.google.common.util.concurrent.ListenableFuture<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseStreamAsync(
                org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request) {
            return futureStub.responseStream(request);
        }

        public void responseStream(org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest request,
                                   io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            stub.responseStream(request, responseObserver);
        }

        public io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest> allStream(
                io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse> responseObserver) {
            return stub.allStream(responseObserver);
        }

    }

    public static DubboGreeterStub getDubboStub(io.grpc.Channel channel) {

        return new DubboGreeterStub(channel);
    }

    private static final int METHODID_HELLO_WORLD = 0;
    private static final int METHODID_RESPONSE_STREAM = 1;
    private static final int METHODID_REQUEST_STREAM = 2;
    private static final int METHODID_ALL_STREAM = 3;

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
                case METHODID_HELLO_WORLD:
                    serviceImpl.helloWorld((org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest) request,
                            (io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>) responseObserver);
                    break;
                case METHODID_RESPONSE_STREAM:
                    serviceImpl.responseStream((org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloRequest) request,
                            (io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>) responseObserver);
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
                case METHODID_REQUEST_STREAM:
                    return (io.grpc.stub.StreamObserver<Req>) serviceImpl.requestStream(
                            (io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>) responseObserver);
                case METHODID_ALL_STREAM:
                    return (io.grpc.stub.StreamObserver<Req>) serviceImpl.allStream(
                            (io.grpc.stub.StreamObserver<org.apache.dubbo.rpc.protocol.grpc.HelloService.HelloResponse>) responseObserver);
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
            return org.apache.dubbo.rpc.protocol.grpc.HelloService.getDescriptor();
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
            synchronized (GreeterGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                            .setSchemaDescriptor(new GreeterFileDescriptorSupplier())
                            .addMethod(getHelloWorldMethod())
                            .addMethod(getRequestStreamMethod())
                            .addMethod(getResponseStreamMethod())
                            .addMethod(getAllStreamMethod())
                            .build();
                }
            }
        }
        return result;
    }
}
