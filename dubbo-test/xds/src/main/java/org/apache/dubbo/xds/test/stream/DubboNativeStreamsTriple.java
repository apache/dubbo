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
package org.apache.dubbo.xds.test.stream;

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
import org.apache.dubbo.rpc.protocol.tri.service.SchemaDescriptorRegistry;
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.stub.BiStreamMethodHandler;
import org.apache.dubbo.rpc.stub.StubInvocationUtil;
import org.apache.dubbo.rpc.stub.StubInvoker;
import org.apache.dubbo.rpc.stub.StubMethodHandler;
import org.apache.dubbo.rpc.stub.StubSuppliers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.protobuf.Message;

public final class DubboNativeStreamsTriple {

    public static final String SERVICE_NAME = NativeStreams.SERVICE_NAME;

    private static final StubServiceDescriptor serviceDescriptor =
            new StubServiceDescriptor(SERVICE_NAME, NativeStreams.class);

    static {
        SchemaDescriptorRegistry.addSchemaDescriptor(SERVICE_NAME, NativeStreamsOuterClass.getDescriptor());
        StubSuppliers.addSupplier(SERVICE_NAME, DubboNativeStreamsTriple::newStub);
        StubSuppliers.addSupplier(NativeStreams.JAVA_SERVICE_NAME, DubboNativeStreamsTriple::newStub);
        StubSuppliers.addDescriptor(SERVICE_NAME, serviceDescriptor);
        StubSuppliers.addDescriptor(NativeStreams.JAVA_SERVICE_NAME, serviceDescriptor);
    }

    @SuppressWarnings("unchecked")
    public static NativeStreams newStub(Invoker<?> invoker) {
        return new NativeStreamsStub((Invoker<NativeStreams>) invoker);
    }

    private static final StubMethodDescriptor collectMethod = new StubMethodDescriptor(
            "Collect",
            NativeStreamMessage.class,
            NativeStreamMessage.class,
            MethodDescriptor.RpcType.CLIENT_STREAM,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            NativeStreamMessage::parseFrom,
            NativeStreamMessage::parseFrom);

    private static final StubMethodDescriptor chatMethod = new StubMethodDescriptor(
            "Chat",
            NativeStreamMessage.class,
            NativeStreamMessage.class,
            MethodDescriptor.RpcType.BI_STREAM,
            obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            NativeStreamMessage::parseFrom,
            NativeStreamMessage::parseFrom);

    static {
        serviceDescriptor.addMethod(collectMethod);
        serviceDescriptor.addMethod(chatMethod);
    }

    public static class NativeStreamsStub implements NativeStreams, Destroyable {
        private final Invoker<NativeStreams> invoker;

        public NativeStreamsStub(Invoker<NativeStreams> invoker) {
            this.invoker = invoker;
        }

        @Override
        public void $destroy() {
            invoker.destroy();
        }

        @Override
        public StreamObserver<NativeStreamMessage> chat(StreamObserver<NativeStreamMessage> responseObserver) {
            return StubInvocationUtil.biOrClientStreamCall(invoker, chatMethod, responseObserver);
        }

        @Override
        public StreamObserver<NativeStreamMessage> collect(StreamObserver<NativeStreamMessage> responseObserver) {
            return StubInvocationUtil.biOrClientStreamCall(invoker, collectMethod, responseObserver);
        }
    }

    public abstract static class NativeStreamsImplBase implements NativeStreams, ServerService<NativeStreams> {
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

        // This server stream type unary method is <b>only</b> used for generated stub to support async unary method.
        // It will not be called if you are NOT using Dubbo3 generated triple stub and <b>DO NOT</b> implement this
        // method.

        @Override
        public final Invoker<NativeStreams> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
                    .getExtensionLoader(PathResolver.class)
                    .getDefaultExtension();
            Map<String, StubMethodHandler<?, ?>> handlers = new HashMap<>();
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/Collect");
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/CollectAsync");
            // for compatibility
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/Collect");
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/CollectAsync");
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/Chat");
            pathResolver.addNativeStub("/" + SERVICE_NAME + "/ChatAsync");
            // for compatibility
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/Chat");
            pathResolver.addNativeStub("/" + JAVA_SERVICE_NAME + "/ChatAsync");
            handlers.put(collectMethod.getMethodName(), new BiStreamMethodHandler<>(this::collect));
            handlers.put(chatMethod.getMethodName(), new BiStreamMethodHandler<>(this::chat));

            return new StubInvoker<>(this, url, NativeStreams.class, handlers);
        }

        @Override
        public StreamObserver<NativeStreamMessage> chat(StreamObserver<NativeStreamMessage> responseObserver) {
            throw unimplementedMethodException(chatMethod);
        }

        @Override
        public StreamObserver<NativeStreamMessage> collect(StreamObserver<NativeStreamMessage> responseObserver) {
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
