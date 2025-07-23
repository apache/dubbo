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
package org.apache.dubbo.metadata;

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

public final class DubboMetadataServiceV2Triple {

    public static final String SERVICE_NAME = MetadataServiceV2.SERVICE_NAME;

    private static final StubServiceDescriptor serviceDescriptor =
            new StubServiceDescriptor(SERVICE_NAME, MetadataServiceV2.class);

    static {
        org.apache.dubbo.rpc.protocol.tri.service.SchemaDescriptorRegistry.addSchemaDescriptor(
                SERVICE_NAME, MetadataServiceV2OuterClass.getDescriptor());
        StubSuppliers.addSupplier(SERVICE_NAME, DubboMetadataServiceV2Triple::newStub);
        StubSuppliers.addSupplier(MetadataServiceV2.JAVA_SERVICE_NAME, DubboMetadataServiceV2Triple::newStub);
        StubSuppliers.addDescriptor(SERVICE_NAME, serviceDescriptor);
        StubSuppliers.addDescriptor(MetadataServiceV2.JAVA_SERVICE_NAME, serviceDescriptor);
    }

    @SuppressWarnings("unchecked")
    public static MetadataServiceV2 newStub(Invoker<?> invoker) {
        return new MetadataServiceV2Stub((Invoker<MetadataServiceV2>) invoker);
    }

    private static final StubMethodDescriptor getMetadataInfoMethod = new StubMethodDescriptor(
            "GetMetadataInfo",
            MetadataRequest.class,
            MetadataInfoV2.class,
            MethodDescriptor.RpcType.UNARY,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            MetadataRequest::parseFrom,
            MetadataInfoV2::parseFrom);

    private static final StubMethodDescriptor getMetadataInfoAsyncMethod = new StubMethodDescriptor(
            "GetMetadataInfo",
            MetadataRequest.class,
            CompletableFuture.class,
            MethodDescriptor.RpcType.UNARY,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            MetadataRequest::parseFrom,
            MetadataInfoV2::parseFrom);

    private static final StubMethodDescriptor getMetadataInfoProxyAsyncMethod = new StubMethodDescriptor(
            "GetMetadataInfoAsync",
            MetadataRequest.class,
            MetadataInfoV2.class,
            MethodDescriptor.RpcType.UNARY,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            MetadataRequest::parseFrom,
            MetadataInfoV2::parseFrom);

    private static final StubMethodDescriptor getOpenAPIInfoMethod = new StubMethodDescriptor(
            "GetOpenAPIInfo",
            OpenAPIRequest.class,
            OpenAPIInfo.class,
            MethodDescriptor.RpcType.UNARY,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            OpenAPIRequest::parseFrom,
            OpenAPIInfo::parseFrom);

    private static final StubMethodDescriptor getOpenAPIInfoAsyncMethod = new StubMethodDescriptor(
            "GetOpenAPIInfo",
            OpenAPIRequest.class,
            CompletableFuture.class,
            MethodDescriptor.RpcType.UNARY,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            OpenAPIRequest::parseFrom,
            OpenAPIInfo::parseFrom);

    private static final StubMethodDescriptor getOpenAPIInfoProxyAsyncMethod = new StubMethodDescriptor(
            "GetOpenAPIInfoAsync",
            OpenAPIRequest.class,
            OpenAPIInfo.class,
            MethodDescriptor.RpcType.UNARY,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            OpenAPIRequest::parseFrom,
            OpenAPIInfo::parseFrom);

    static {
        serviceDescriptor.addMethod(getMetadataInfoMethod);
        serviceDescriptor.addMethod(getMetadataInfoProxyAsyncMethod);
        serviceDescriptor.addMethod(getOpenAPIInfoMethod);
        serviceDescriptor.addMethod(getOpenAPIInfoProxyAsyncMethod);
    }

    public static class MetadataServiceV2Stub implements MetadataServiceV2, Destroyable {
        private final Invoker<MetadataServiceV2> invoker;

        public MetadataServiceV2Stub(Invoker<MetadataServiceV2> invoker) {
            this.invoker = invoker;
        }

        @Override
        public void $destroy() {
            invoker.destroy();
        }

        @Override
        public MetadataInfoV2 getMetadataInfo(MetadataRequest request) {
            return StubInvocationUtil.unaryCall(invoker, getMetadataInfoMethod, request);
        }

        public CompletableFuture<MetadataInfoV2> getMetadataInfoAsync(MetadataRequest request) {
            return StubInvocationUtil.unaryCall(invoker, getMetadataInfoAsyncMethod, request);
        }

        public void getMetadataInfo(MetadataRequest request, StreamObserver<MetadataInfoV2> responseObserver) {
            StubInvocationUtil.unaryCall(invoker, getMetadataInfoMethod, request, responseObserver);
        }

        @Override
        public OpenAPIInfo getOpenAPIInfo(OpenAPIRequest request) {
            return StubInvocationUtil.unaryCall(invoker, getOpenAPIInfoMethod, request);
        }

        public CompletableFuture<OpenAPIInfo> getOpenAPIInfoAsync(OpenAPIRequest request) {
            return StubInvocationUtil.unaryCall(invoker, getOpenAPIInfoAsyncMethod, request);
        }

        public void getOpenAPIInfo(OpenAPIRequest request, StreamObserver<OpenAPIInfo> responseObserver) {
            StubInvocationUtil.unaryCall(invoker, getOpenAPIInfoMethod, request, responseObserver);
        }
    }

    public abstract static class MetadataServiceV2ImplBase
            implements MetadataServiceV2, ServerService<MetadataServiceV2> {
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
        public CompletableFuture<MetadataInfoV2> getMetadataInfoAsync(MetadataRequest request) {
            return CompletableFuture.completedFuture(getMetadataInfo(request));
        }

        @Override
        public CompletableFuture<OpenAPIInfo> getOpenAPIInfoAsync(OpenAPIRequest request) {
            return CompletableFuture.completedFuture(getOpenAPIInfo(request));
        }

        // This server stream type unary method is <b>only</b> used for generated stub to support async unary method.
        // It will not be called if you are NOT using Dubbo3 generated triple stub and <b>DO NOT</b> implement this
        // method.

        public void getMetadataInfo(MetadataRequest request, StreamObserver<MetadataInfoV2> responseObserver) {
            getMetadataInfoAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        public void getOpenAPIInfo(OpenAPIRequest request, StreamObserver<OpenAPIInfo> responseObserver) {
            getOpenAPIInfoAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public final Invoker<MetadataServiceV2> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
                    .getExtensionLoader(PathResolver.class)
                    .getDefaultExtension();
            Map<String, StubMethodHandler<?, ?>> handlers = new HashMap<>();
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/GetMetadataInfo");
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/GetMetadataInfoAsync");
            // for compatibility
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/GetMetadataInfo");
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/GetMetadataInfoAsync");
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/GetOpenAPIInfo");
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/GetOpenAPIInfoAsync");
            // for compatibility
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/GetOpenAPIInfo");
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/GetOpenAPIInfoAsync");
            BiConsumer<MetadataRequest, StreamObserver<MetadataInfoV2>> getMetadataInfoFunc = this::getMetadataInfo;
            handlers.put(getMetadataInfoMethod.getMethodName(), new UnaryStubMethodHandler<>(getMetadataInfoFunc));
            BiConsumer<MetadataRequest, StreamObserver<MetadataInfoV2>> getMetadataInfoAsyncFunc =
                    syncToAsync(this::getMetadataInfo);
            handlers.put(
                    getMetadataInfoProxyAsyncMethod.getMethodName(),
                    new UnaryStubMethodHandler<>(getMetadataInfoAsyncFunc));
            BiConsumer<OpenAPIRequest, StreamObserver<OpenAPIInfo>> getOpenAPIInfoFunc = this::getOpenAPIInfo;
            handlers.put(getOpenAPIInfoMethod.getMethodName(), new UnaryStubMethodHandler<>(getOpenAPIInfoFunc));
            BiConsumer<OpenAPIRequest, StreamObserver<OpenAPIInfo>> getOpenAPIInfoAsyncFunc =
                    syncToAsync(this::getOpenAPIInfo);
            handlers.put(
                    getOpenAPIInfoProxyAsyncMethod.getMethodName(),
                    new UnaryStubMethodHandler<>(getOpenAPIInfoAsyncFunc));

            return new StubInvoker<>(this, url, MetadataServiceV2.class, handlers);
        }

        @Override
        public MetadataInfoV2 getMetadataInfo(MetadataRequest request) {
            throw unimplementedMethodException(getMetadataInfoMethod);
        }

        @Override
        public OpenAPIInfo getOpenAPIInfo(OpenAPIRequest request) {
            throw unimplementedMethodException(getOpenAPIInfoMethod);
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
