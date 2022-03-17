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
import java.util.function.Function;

public class GreeterStub implements IGreeter {
    private static final String SERVICE_NAME = "org.apache.dubbo.sample.tri.IGreeter";
    private static final String SERVICE_VERSION = "1.0.0";
    private static final String SERVICE_GROUP = "";
    private static final ServiceDescriptor serviceDescriptor = new ServiceDescriptor(SERVICE_NAME, IGreeter.class);
    private static final Map<String, StubMethodDescriptor> NAME_2_METHOD_DESCRIPTORS = new HashMap<>();
    private static final StubMethodDescriptor SAY_HELLO_METHOD = new StubMethodDescriptor("sayHello",
            HelloRequest.class, HelloReply.class, serviceDescriptor,
            StubMethodDescriptor.RpcType.UNARY, obj -> ((Message) obj).toByteArray(),
            obj -> ((Message) obj).toByteArray(),
            HelloRequest::parseFrom,
            HelloReply::parseFrom, "/"+SERVICE_NAME + "/sayHello");

    static {
        NAME_2_METHOD_DESCRIPTORS.put(SAY_HELLO_METHOD.getMethodName(), SAY_HELLO_METHOD);
    }

    //    private static final ServiceDescriptor serviceDescriptor;

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
    public StreamObserver<HelloRequest> sayHello(StreamObserver<HelloReply> responseObserver) {
        return null;
    }

    private static StubMethodDescriptor getMethod(String method) {
        return NAME_2_METHOD_DESCRIPTORS.get(method);
    }

    public static abstract class IGreeterImplBase implements IGreeter, ServerService {

        @Override
        @SuppressWarnings("all")
        public Invoker<IGreeter> getInvoker(URL url) {
            PathResolver pathResovler = url.getOrDefaultFrameworkModel()
                                           .getExtensionLoader(PathResolver.class)
                                           .getDefaultExtension();
            pathResovler.addNativeStub(getSayHelloMethod().fullMethodName);
            Map<String, Function<Object[], Object>> handlers = new HashMap<>();
            handlers.put(getSayHelloMethod().getMethodName(), objects -> sayHello((HelloRequest) objects[0]));
            return new StubInvoker<>(this, url, IGreeter.class, handlers);
        }

        @Override
        public HelloReply sayHello(HelloRequest request) {
            throw StubCallUtil.unimplementedMethodException(SAY_HELLO_METHOD);
        }

        @Override
        public StreamObserver<HelloRequest> sayHello(StreamObserver<HelloReply> responseObserver) {
            return StubCallUtil.callMethod(getSayHelloMethod(), responseObserver, this::sayHello);
        }

    }
}
