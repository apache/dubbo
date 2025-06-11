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

package org.apache.dubbo.rpc.protocol.tri.proto;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.ServerService;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.model.StubServiceDescriptor;
import org.apache.dubbo.rpc.stub.BiStreamMethodHandler;
import org.apache.dubbo.rpc.stub.ServerStreamMethodHandler;
import org.apache.dubbo.rpc.stub.StubInvocationUtil;
import org.apache.dubbo.rpc.stub.StubInvoker;
import org.apache.dubbo.rpc.stub.StubMethodHandler;
import org.apache.dubbo.rpc.stub.StubSuppliers;
import org.apache.dubbo.rpc.stub.UnaryStubMethodHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import com.google.protobuf.Message;

public final class DubboDemoServiceTriple {

    public static final String SERVICE_NAME = DemoService.SERVICE_NAME;

    private static final StubServiceDescriptor serviceDescriptor = new StubServiceDescriptor(SERVICE_NAME,DemoService.class);

    static {
        org.apache.dubbo.rpc.protocol.tri.service.SchemaDescriptorRegistry.addSchemaDescriptor(SERVICE_NAME,DemoServiceProto.getDescriptor());
        StubSuppliers.addSupplier(SERVICE_NAME, DubboDemoServiceTriple::newStub);
        StubSuppliers.addSupplier(DemoService.JAVA_SERVICE_NAME,  DubboDemoServiceTriple::newStub);
        StubSuppliers.addDescriptor(SERVICE_NAME, serviceDescriptor);
        StubSuppliers.addDescriptor(DemoService.JAVA_SERVICE_NAME, serviceDescriptor);
    }

    @SuppressWarnings("all")
    public static DemoService newStub(Invoker<?> invoker) {
        return new DemoServiceStub((Invoker<DemoService>)invoker);
    }

    private static final StubMethodDescriptor helloUnaryMethod = new StubMethodDescriptor("helloUnary",
    HelloRequest.class, HelloResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom,
    HelloResponse::parseFrom);

    private static final StubMethodDescriptor helloUnaryAsyncMethod = new StubMethodDescriptor("helloUnary",
    HelloRequest.class, CompletableFuture.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom,
    HelloResponse::parseFrom);

    private static final StubMethodDescriptor helloUnaryProxyAsyncMethod = new StubMethodDescriptor("helloUnaryAsync",
    HelloRequest.class, HelloResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom,
    HelloResponse::parseFrom);

    private static final StubMethodDescriptor helloServerStreamMethod = new StubMethodDescriptor("helloServerStream",
    HelloRequest.class, HelloResponse.class, MethodDescriptor.RpcType.SERVER_STREAM,
    obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom,
    HelloResponse::parseFrom);
    private static final StubMethodDescriptor helloBIStreamMethod = new StubMethodDescriptor("helloBIStream",
    HelloRequest.class, HelloResponse.class, MethodDescriptor.RpcType.SERVER_STREAM,
    obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom,
    HelloResponse::parseFrom);

    private static final StubMethodDescriptor helloClientStreamMethod = new StubMethodDescriptor("helloClientStream",
    HelloRequest.class, HelloResponse.class, MethodDescriptor.RpcType.CLIENT_STREAM,
    obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom,
    HelloResponse::parseFrom);


    static{
        serviceDescriptor.addMethod(helloUnaryMethod);
        serviceDescriptor.addMethod(helloUnaryProxyAsyncMethod);
        serviceDescriptor.addMethod(helloServerStreamMethod);
        serviceDescriptor.addMethod(helloBIStreamMethod);
        serviceDescriptor.addMethod(helloClientStreamMethod);
    }

    public static class DemoServiceStub implements DemoService{
        private final Invoker<DemoService> invoker;

        public DemoServiceStub(Invoker<DemoService> invoker) {
            this.invoker = invoker;
        }

        @Override
        public HelloResponse helloUnary(HelloRequest request){
            return StubInvocationUtil.unaryCall(invoker, helloUnaryMethod, request);
        }

        public CompletableFuture<HelloResponse> helloUnaryAsync(HelloRequest request){
            return StubInvocationUtil.unaryCall(invoker, helloUnaryAsyncMethod, request);
        }

        public void helloUnary(HelloRequest request, StreamObserver<HelloResponse> responseObserver){
            StubInvocationUtil.unaryCall(invoker, helloUnaryMethod , request, responseObserver);
        }

        @Override
        public void helloServerStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver){
            StubInvocationUtil.serverStreamCall(invoker, helloServerStreamMethod , request, responseObserver);
        }
        @Override
        public void helloBIStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver){
            StubInvocationUtil.serverStreamCall(invoker, helloBIStreamMethod , request, responseObserver);
        }


        @Override
        public StreamObserver<HelloRequest> helloClientStream(StreamObserver<HelloResponse> responseObserver){
        return StubInvocationUtil.biOrClientStreamCall(invoker, helloClientStreamMethod , responseObserver);
        }
    }

    public static abstract class DemoServiceImplBase implements DemoService, ServerService<DemoService> {

        private <T, R> BiConsumer<T, StreamObserver<R>> syncToAsync(java.util.function.Function<T, R> syncFun) {
            return new BiConsumer<T, StreamObserver<R>>() {
                @Override
                public void accept(T t, StreamObserver<R> observer) {
                    try {
                        R ret = syncFun.apply(t);
                        observer.onNext(ret);
                        observer.onCompleted();
                    } catch (Throwable e) {
                        observer.onError(e);
                    }
                }
            };
        }

        @Override
        public CompletableFuture<HelloResponse> helloUnaryAsync(HelloRequest request){
                return CompletableFuture.completedFuture(helloUnary(request));
        }

        /**
        * This server stream type unary method is <b>only</b> used for generated stub to support async unary method.
        * It will not be called if you are NOT using Dubbo3 generated triple stub and <b>DO NOT</b> implement this method.
        */
        public void helloUnary(HelloRequest request, StreamObserver<HelloResponse> responseObserver){
            helloUnaryAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public final Invoker<DemoService> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
            .getExtensionLoader(PathResolver.class)
            .getDefaultExtension();
            Map<String,StubMethodHandler<?, ?>> handlers = new HashMap<>();

            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/helloUnary" );
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/helloUnaryAsync" );
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/helloClientStream" );
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/helloClientStreamAsync" );
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/helloServerStream" );
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/helloServerStreamAsync" );
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/helloBIStream" );
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/helloBIStreamAsync" );

            BiConsumer<HelloRequest, StreamObserver<HelloResponse>> helloUnaryFunc = this::helloUnary;
            handlers.put(helloUnaryMethod.getMethodName(), new UnaryStubMethodHandler<>(helloUnaryFunc));
            BiConsumer<HelloRequest, StreamObserver<HelloResponse>> helloUnaryAsyncFunc = syncToAsync(this::helloUnary);
            handlers.put(helloUnaryProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(helloUnaryAsyncFunc));

            handlers.put(helloServerStreamMethod.getMethodName(), new ServerStreamMethodHandler<>(this::helloServerStream));
            handlers.put(helloBIStreamMethod.getMethodName(), new ServerStreamMethodHandler<>(this::helloBIStream));

            handlers.put(helloClientStreamMethod.getMethodName(), new BiStreamMethodHandler<>(this::helloClientStream));


            return new StubInvoker<>(this, url, DemoService.class, handlers);
        }


        @Override
        public HelloResponse helloUnary(HelloRequest request){
            throw unimplementedMethodException(helloUnaryMethod);
        }


        @Override
        public void helloServerStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver){
            throw unimplementedMethodException(helloServerStreamMethod);
        }
        @Override
        public void helloBIStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver){
            throw unimplementedMethodException(helloBIStreamMethod);
        }


        @Override
        public StreamObserver<HelloRequest> helloClientStream(StreamObserver<HelloResponse> responseObserver){
            throw unimplementedMethodException(helloClientStreamMethod);
        }

        @Override
        public final ServiceDescriptor getServiceDescriptor() {
            return serviceDescriptor;
        }
        private RpcException unimplementedMethodException(StubMethodDescriptor methodDescriptor) {
            return TriRpcStatus.UNIMPLEMENTED.withDescription(String.format("Method %s is unimplemented",
                "/" + serviceDescriptor.getInterfaceName() + "/" + methodDescriptor.getMethodName())).asException();
        }
    }

}
