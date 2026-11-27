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
package org.apache.dubbo.xds.fixture;

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
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.stub.BiStreamMethodHandler;
import org.apache.dubbo.rpc.stub.StubInvocationUtil;
import org.apache.dubbo.rpc.stub.StubInvoker;
import org.apache.dubbo.rpc.stub.StubMethodHandler;
import org.apache.dubbo.rpc.stub.StubSuppliers;
import org.apache.dubbo.rpc.stub.UnaryStubMethodHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.google.protobuf.Message;

public final class DubboWireEchoTriple {

    public static final String SERVICE_NAME = WireEcho.SERVICE_NAME;

    private static final StubServiceDescriptor serviceDescriptor =
            new StubServiceDescriptor(SERVICE_NAME, WireEcho.class);

    static {
        org.apache.dubbo.rpc.protocol.tri.service.SchemaDescriptorRegistry.addSchemaDescriptor(
                SERVICE_NAME, WireEchoOuterClass.getDescriptor());
        StubSuppliers.addSupplier(SERVICE_NAME, DubboWireEchoTriple::newStub);
        StubSuppliers.addSupplier(WireEcho.JAVA_SERVICE_NAME, DubboWireEchoTriple::newStub);
        StubSuppliers.addDescriptor(SERVICE_NAME, serviceDescriptor);
        StubSuppliers.addDescriptor(WireEcho.JAVA_SERVICE_NAME, serviceDescriptor);
    }

    @SuppressWarnings("unchecked")
    public static WireEcho newStub(Invoker<?> invoker) {
        return new WireEchoStub((Invoker<WireEcho>) invoker);
    }

    private static final StubMethodDescriptor sayHelloMethod = new StubMethodDescriptor(
            "SayHello",
            WireMessage.class,
            WireMessage.class,
            MethodDescriptor.RpcType.UNARY,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            WireMessage::parseFrom,
            WireMessage::parseFrom);

    private static final StubMethodDescriptor sayHelloAsyncMethod = new StubMethodDescriptor(
            "SayHello",
            WireMessage.class,
            java.util.concurrent.CompletableFuture.class,
            MethodDescriptor.RpcType.UNARY,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            WireMessage::parseFrom,
            WireMessage::parseFrom);

    private static final StubMethodDescriptor sayHelloProxyAsyncMethod = new StubMethodDescriptor(
            "SayHelloAsync",
            WireMessage.class,
            WireMessage.class,
            MethodDescriptor.RpcType.UNARY,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            WireMessage::parseFrom,
            WireMessage::parseFrom);

    private static final StubMethodDescriptor collectMethod = new StubMethodDescriptor(
            "Collect",
            WireMessage.class,
            WireMessage.class,
            MethodDescriptor.RpcType.CLIENT_STREAM,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            WireMessage::parseFrom,
            WireMessage::parseFrom);

    static {
        serviceDescriptor.addMethod(sayHelloMethod);
        serviceDescriptor.addMethod(sayHelloProxyAsyncMethod);
        serviceDescriptor.addMethod(collectMethod);
    }

    public static class WireEchoStub implements WireEcho, Destroyable {
        private final Invoker<WireEcho> invoker;

        public WireEchoStub(Invoker<WireEcho> invoker) {
            this.invoker = invoker;
        }

        @Override
        public void $destroy() {
            invoker.destroy();
        }

        @Override
        public WireMessage sayHello(WireMessage request) {
            return StubInvocationUtil.unaryCall(invoker, sayHelloMethod, request);
        }

        public CompletableFuture<WireMessage> sayHelloAsync(WireMessage request) {
            return StubInvocationUtil.unaryCall(invoker, sayHelloAsyncMethod, request);
        }

        public void sayHello(WireMessage request, StreamObserver<WireMessage> responseObserver) {
            StubInvocationUtil.unaryCall(invoker, sayHelloMethod, request, responseObserver);
        }

        @Override
        public StreamObserver<WireMessage> collect(StreamObserver<WireMessage> responseObserver) {
            return StubInvocationUtil.biOrClientStreamCall(invoker, collectMethod, responseObserver);
        }
    }

    public abstract static class WireEchoImplBase implements WireEcho, ServerService<WireEcho> {
        private <T, R> BiConsumer<T, StreamObserver<R>> syncToAsync(Function<T, R> syncFun) {
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
        public CompletableFuture<WireMessage> sayHelloAsync(WireMessage request) {
            return CompletableFuture.completedFuture(sayHello(request));
        }

        // This server stream type unary method is <b>only</b> used for generated stub to support async unary method.
        // It will not be called if you are NOT using Dubbo3 generated triple stub and <b>DO NOT</b> implement this
        // method.

        public void sayHello(WireMessage request, StreamObserver<WireMessage> responseObserver) {
            sayHelloAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public final Invoker<WireEcho> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
                    .getExtensionLoader(PathResolver.class)
                    .getDefaultExtension();
            Map<String, StubMethodHandler<?, ?>> handlers = new HashMap<>();
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/SayHello");
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/SayHelloAsync");
            // for compatibility
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/SayHello");
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/SayHelloAsync");
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/Collect");
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/CollectAsync");
            // for compatibility
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/Collect");
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/CollectAsync");
            BiConsumer<WireMessage, StreamObserver<WireMessage>> sayHelloFunc = this::sayHello;
            handlers.put(sayHelloMethod.getMethodName(), new UnaryStubMethodHandler<>(sayHelloFunc));
            BiConsumer<WireMessage, StreamObserver<WireMessage>> sayHelloAsyncFunc = syncToAsync(this::sayHello);
            handlers.put(sayHelloProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(sayHelloAsyncFunc));
            handlers.put(collectMethod.getMethodName(), new BiStreamMethodHandler<>(this::collect));

            return new StubInvoker<>(this, url, WireEcho.class, handlers);
        }

        @Override
        public WireMessage sayHello(WireMessage request) {
            throw unimplementedMethodException(sayHelloMethod);
        }

        @Override
        public StreamObserver<WireMessage> collect(StreamObserver<WireMessage> responseObserver) {
            throw unimplementedMethodException(collectMethod);
        }

        @Override
        public final ServiceDescriptor getServiceDescriptor() {
            return serviceDescriptor;
        }

        private RpcException unimplementedMethodException(StubMethodDescriptor methodDescriptor) {
            return TriRpcStatus.UNIMPLEMENTED
                    .withDescription(String.format(
                            "Method %s is unimplemented",
                            "/" + serviceDescriptor.getInterfaceName() + "/" + methodDescriptor.getMethodName()))
                    .asException();
        }
    }
}
