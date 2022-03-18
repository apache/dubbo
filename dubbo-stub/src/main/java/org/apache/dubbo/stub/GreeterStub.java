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

package org.apache.dubbo.stub;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.ServerService;
import org.apache.dubbo.rpc.StubInvoker;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.sample.tri.HelloReply;
import org.apache.dubbo.sample.tri.HelloRequest;
import org.apache.dubbo.sample.tri.IGreeter;

import com.google.protobuf.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class GreeterStub implements IGreeter {
    private static final String SERVICE_NAME = "org.apache.dubbo.sample.tri.IGreeter";
    private static final String SERVICE_VERSION = "1.0.0";
    private static final String SERVICE_GROUP = "";
    private static final ServiceDescriptor serviceDescriptor = new ServiceDescriptor(SERVICE_NAME, IGreeter.class);
    private static final Map<String, StubMethodDescriptor> NAME_2_METHOD_DESCRIPTORS = new HashMap<>();
    private static final StubMethodDescriptor SAY_HELLO_METHOD = new StubMethodDescriptor("sayHello",
        HelloRequest.class, HelloReply.class, serviceDescriptor, StubMethodDescriptor.RpcType.UNARY,
        obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom,
        HelloReply::parseFrom, "/" + SERVICE_NAME + "/sayHello");
    private static final StubMethodDescriptor SAY_HELLO_STREAM_METHOD = new StubMethodDescriptor("sayHelloStream",
        HelloRequest.class, HelloReply.class, serviceDescriptor, StubMethodDescriptor.RpcType.BI_STREAM,
        obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom,
        HelloReply::parseFrom, "/" + SERVICE_NAME + "/sayHelloStream");

    private static final StubMethodDescriptor SAY_HELLO_CLIENT_STREAM_METHOD = new StubMethodDescriptor(
        "sayHelloClientStream", HelloRequest.class, HelloReply.class, serviceDescriptor,
        StubMethodDescriptor.RpcType.CLIENT_STREAM, obj -> ((Message) obj).toByteArray(),
        obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom, HelloReply::parseFrom,
        "/" + SERVICE_NAME + "/sayHelloClientStream");

    private static final StubMethodDescriptor SAY_HELLO_SERVER_STREAM_METHOD = new StubMethodDescriptor(
        "sayHelloServerStream", HelloRequest.class, HelloReply.class, serviceDescriptor,
        StubMethodDescriptor.RpcType.SERVER_STREAM, obj -> ((Message) obj).toByteArray(),
        obj -> ((Message) obj).toByteArray(), HelloRequest::parseFrom, HelloReply::parseFrom,
        "/" + SERVICE_NAME + "/sayHelloClientStream");

    static {
        NAME_2_METHOD_DESCRIPTORS.put(SAY_HELLO_METHOD.getMethodName(), SAY_HELLO_METHOD);
        NAME_2_METHOD_DESCRIPTORS.put(SAY_HELLO_STREAM_METHOD.getMethodName(), SAY_HELLO_STREAM_METHOD);
        NAME_2_METHOD_DESCRIPTORS.put(SAY_HELLO_SERVER_STREAM_METHOD.getMethodName(), SAY_HELLO_SERVER_STREAM_METHOD);
        NAME_2_METHOD_DESCRIPTORS.put(SAY_HELLO_CLIENT_STREAM_METHOD.getMethodName(), SAY_HELLO_CLIENT_STREAM_METHOD);
    }

    //    private static final ServiceDescriptor serviceDescriptor;

    public static StubMethodDescriptor getSayHelloStreamMethod() {
        return SAY_HELLO_STREAM_METHOD;
    }

    public static StubMethodDescriptor getSayHelloMethod() {
//        if (SAY_HELLO_METHOD != null) {
        return SAY_HELLO_METHOD;
//        }
//        synchronized (GreeterStub.class) {
//            if (SAY_HELLO_METHOD != null) {
//                return SAY_HELLO_METHOD;
//            }
//            SAY_HELLO_METHOD = new StubMethodDescriptor("greet", HelloRequest.class, HelloReply.class, StubMethodDescriptor.RpcType.UNARY);
//        }
//        return SAY_HELLO_METHOD;
    }

//    public static ServiceDescriptor getServiceDescriptor() {
//        if (serviceDescriptor != null) {
//            return serviceDescriptor;
//        }
//        synchronized (GreeterStub.class) {
//            if (serviceDescriptor != null) {
//                return serviceDescriptor;
//            } else {
//                final ModuleModel model = ApplicationModel.defaultModel().getDefaultModule();
//                serviceDescriptor = model.getServiceRepository().registerService(GreeterStub.class);
//            }
//        }
//        return serviceDescriptor;
//    }

    @Override
    public HelloReply sayHello(HelloRequest request) {
        return null;
    }

//    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
//        asyncCall(connection, getSayHelloMethod(), request, responseObserver);
//    }

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
    }

    @Override
    public void sayHelloServerStream(HelloRequest request, StreamObserver<HelloReply> replyStream) {

    }

    @Override
    public StreamObserver<HelloRequest> sayHelloClientStream(StreamObserver<HelloReply> replyStream) {
        return null;
    }

    @Override
    public StreamObserver<HelloRequest> sayHelloStream(StreamObserver<HelloReply> replyStream) {
        return null;
    }

    private static StubMethodDescriptor getMethod(String method) {
        return NAME_2_METHOD_DESCRIPTORS.get(method);
    }

    public static abstract class IGreeterImplBase implements IGreeter, ServerService {

        @Override
        @SuppressWarnings("all")
        public final Invoker<IGreeter> getInvoker(URL url) {
            PathResolver pathResovler = url.getOrDefaultFrameworkModel()
                .getExtensionLoader(PathResolver.class)
                .getDefaultExtension();
            pathResovler.addNativeStub(getSayHelloMethod().fullMethodName);
            Map<String, Function<Object[], CompletableFuture<?>>> handlers = new HashMap<>();
            BiConsumer<HelloRequest, StreamObserver<HelloReply>> sayHelloMethodConsumer = this::sayHello;
            handlers.put(getSayHelloMethod().getMethodName(),
                objects -> StubCallUtil.callUnaryMethod((HelloRequest) objects[0], sayHelloMethodConsumer));
            handlers.put(getSayHelloStreamMethod().getMethodName(),
                objects -> StubCallUtil.callStreamMethod((StreamObserver<HelloReply>) objects[0],
                    this::sayHelloStream));
            handlers.put(SAY_HELLO_CLIENT_STREAM_METHOD.getMethodName(),
                objects -> StubCallUtil.callStreamMethod((StreamObserver<HelloReply>) objects[0],
                    this::sayHelloClientStream));
            handlers.put(SAY_HELLO_SERVER_STREAM_METHOD.getMethodName(),
                objects -> StubCallUtil.callServerStreamMethod((HelloRequest) objects[0],
                    (StreamObserver<HelloReply>) objects[1], this::sayHelloServerStream));
            return new StubInvoker<IGreeter>(url, IGreeter.class, handlers);
        }

        @Override
        public HelloReply sayHello(HelloRequest request) {
            throw StubCallUtil.unimplementedMethodException(SAY_HELLO_METHOD);
        }

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            StubCallUtil.callUnaryMethod(request, responseObserver, this::sayHello);
        }

        @Override
        public StreamObserver<HelloRequest> sayHelloStream(StreamObserver<HelloReply> replyStream) {
            throw StubCallUtil.unimplementedMethodException(SAY_HELLO_STREAM_METHOD);
        }

        @Override
        public void sayHelloServerStream(HelloRequest request, StreamObserver<HelloReply> replyStream) {
            replyStream.onError(StubCallUtil.unimplementedMethodException(SAY_HELLO_SERVER_STREAM_METHOD));
        }

        @Override
        public StreamObserver<HelloRequest> sayHelloClientStream(StreamObserver<HelloReply> replyStream) {
            throw StubCallUtil.unimplementedMethodException(SAY_HELLO_CLIENT_STREAM_METHOD);
        }

        @Override
        public final ServiceDescriptor getServiceDescriptor() {
            return serviceDescriptor;
        }
    }
}
