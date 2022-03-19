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

package org.apache.dubbo.sample.tri;

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

import com.google.protobuf.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class DubboIGreeterTriple {

    public static final String SERVICE_NAME = "org.apache.dubbo.sample.tri.IGreeter";
    private static final StubServiceDescriptor serviceDescriptor = new StubServiceDescriptor(SERVICE_NAME,
        IGreeter.class);


    private static final StubMethodDescriptor SAY_HELLO_METHOD = new StubMethodDescriptor("sayHello",
        HelloRequest.class, HelloReply.class, serviceDescriptor, MethodDescriptor.RpcType.UNARY,
        obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom,
        HelloReply::parseFrom);
    private static final StubMethodDescriptor SAY_HELLO_STREAM_METHOD = new StubMethodDescriptor("sayHelloStream",
        HelloRequest.class, HelloReply.class, serviceDescriptor, MethodDescriptor.RpcType.BI_STREAM,
        obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom,
        HelloReply::parseFrom);

    private static final StubMethodDescriptor SAY_HELLO_CLIENT_STREAM_METHOD = new StubMethodDescriptor(
        "sayHelloClientStream", HelloRequest.class, HelloReply.class, serviceDescriptor,
        MethodDescriptor.RpcType.CLIENT_STREAM, obj -> ((Message) obj).toByteArray(),
        obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom, HelloReply::parseFrom);

    private static final StubMethodDescriptor SAY_HELLO_SERVER_STREAM_METHOD = new StubMethodDescriptor(
        "sayHelloServerStream", HelloRequest.class, HelloReply.class, serviceDescriptor,
        MethodDescriptor.RpcType.SERVER_STREAM, obj -> ((Message) obj).toByteArray(),
        obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom, HelloReply::parseFrom);

    static {
        StubSuppliers.addSupplier(IGreeter.class.getName(), i -> newStub((Invoker<IGreeter>) i));
        StubSuppliers.addDescriptor(IGreeter.class.getName(), serviceDescriptor);
    }

    public static IGreeter newStub(Invoker<IGreeter> invoker) {
        return new GreeterStub(invoker);
    }

    public static StubMethodDescriptor getSayHelloStreamMethod() {
        return SAY_HELLO_STREAM_METHOD;
    }

    public static StubMethodDescriptor getSayHelloMethod() {
        return SAY_HELLO_METHOD;
    }

    public static class GreeterStub implements IGreeter {
        private final Invoker<IGreeter> invoker;

        public GreeterStub(Invoker<IGreeter> invoker) {
            this.invoker = invoker;
        }

        @Override
        public HelloReply sayHello(HelloRequest request) {
            return StubInvocationUtil.unaryCall(invoker, SAY_HELLO_METHOD, request);
        }

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            StubInvocationUtil.unaryCall(invoker, SAY_HELLO_METHOD, request, responseObserver);
        }

        @Override
        public void sayHelloServerStream(HelloRequest request, StreamObserver<HelloReply> replyStream) {
            StubInvocationUtil.serverStreamCall(invoker, SAY_HELLO_SERVER_STREAM_METHOD, request, replyStream);

        }

        @Override
        public StreamObserver<HelloRequest> sayHelloClientStream(StreamObserver<HelloReply> replyStream) {
            return StubInvocationUtil.biOrClientStreamCall(invoker, SAY_HELLO_CLIENT_STREAM_METHOD, replyStream);
        }

        @Override
        public StreamObserver<HelloRequest> sayHelloStream(StreamObserver<HelloReply> replyStream) {
            return StubInvocationUtil.biOrClientStreamCall(invoker, SAY_HELLO_STREAM_METHOD, replyStream);
        }
    }

    public static abstract class IGreeterImplBase implements IGreeter, ServerService<IGreeter> {

        @Override
        public final Invoker<IGreeter> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
                .getExtensionLoader(PathResolver.class)
                .getDefaultExtension();
            pathResolver.addNativeStub(
                "/" + serviceDescriptor.getInterfaceName() + "/" + getSayHelloMethod().getMethodName());
            Map<String, StubMethodHandler<?, ?>> handlers = new HashMap<>();

            pathResolver.addNativeStub(
                "/" + serviceDescriptor.getInterfaceName() + "/" + SAY_HELLO_SERVER_STREAM_METHOD.getMethodName());
            pathResolver.addNativeStub(
                "/" + serviceDescriptor.getInterfaceName() + "/" + SAY_HELLO_CLIENT_STREAM_METHOD.getMethodName());
            pathResolver.addNativeStub(
                "/" + serviceDescriptor.getInterfaceName() + "/" + SAY_HELLO_STREAM_METHOD.getMethodName());
            BiConsumer<HelloRequest, StreamObserver<HelloReply>> sayHelloFunc = this::sayHello;
            handlers.put(getSayHelloMethod().getMethodName(), new UnaryStubMethodHandler<>(sayHelloFunc));

            handlers.put(getSayHelloStreamMethod().getMethodName(), new BiStreamMethodHandler<>(this::sayHelloStream));
            handlers.put(SAY_HELLO_CLIENT_STREAM_METHOD.getMethodName(),
                new BiStreamMethodHandler<>(this::sayHelloClientStream));
            handlers.put(SAY_HELLO_SERVER_STREAM_METHOD.getMethodName(),
                new ServerStreamMethodHandler<>(this::sayHelloServerStream));
            return new StubInvoker<>(url, IGreeter.class, handlers);
        }

        @Override
        public HelloReply sayHello(HelloRequest request) {
            throw unimplementedMethodException(SAY_HELLO_METHOD);
        }

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            try {
                HelloReply response = sayHello(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Throwable t) {
                responseObserver.onError(t);
            }
        }

        @Override
        public StreamObserver<HelloRequest> sayHelloStream(StreamObserver<HelloReply> replyStream) {
            throw unimplementedMethodException(SAY_HELLO_STREAM_METHOD);
        }

        @Override
        public void sayHelloServerStream(HelloRequest request, StreamObserver<HelloReply> replyStream) {
            replyStream.onError(unimplementedMethodException(SAY_HELLO_SERVER_STREAM_METHOD));
        }

        @Override
        public StreamObserver<HelloRequest> sayHelloClientStream(StreamObserver<HelloReply> replyStream) {
            throw unimplementedMethodException(SAY_HELLO_CLIENT_STREAM_METHOD);
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
