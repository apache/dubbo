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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ReflectedMethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.DefaultFuture2;
import org.apache.dubbo.rpc.protocol.tri.call.ServerCallUtil;

import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.IGreeter;
import io.netty.channel.ChannelFuture;

import java.util.concurrent.Executors;

public class GreeterStub implements IGreeter {
    private static final String SERVICE_NAME = "org.apache.dubbo.sample.tri.PbGreeter";
    private static final String SERVICE_VERSION = "1.0.0";
    private static final String SERVICE_GROUP = "";

    private static final ServiceDescriptor serviceDescriptor;
    private static final StubMethodDescriptor sayHelloMethod=new StubMethodDescriptor();
    final Connection connection;

    protected GreeterStub(Connection connection) {
        this.connection = connection;
    }

    public static GreeterStub newStub(Connection connection) {
        return new GreeterStub(connection);
    }

    static <ReqT, RespT> void asyncCall(Connection connection, StubMethodDescriptor method, ReqT request, StreamObserver<RespT> responseObserver) {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setArguments(new Object[]{request, responseObserver});
        invocation.setObjectAttachment(CommonConstants.PATH_KEY, SERVICE_NAME);
        invocation.put(CommonConstants.TIMEOUT_KEY, 3000);
        invocation.setServiceName(SERVICE_NAME);
        invocation.setMethodName(method.getMethodName());
        invocation.setParameterTypes(method.getRealParameterClasses());
        invocation.setReturnType(method.getRealReturnClass());
        // TODO remove this
        invocation.put("method_descriptor", method);
        invocation.setInvoker(new Invoker<Object>() {
            @Override
            public Class<Object> getInterface() {
                return null;
            }

            @Override
            public Result invoke(Invocation invocation) throws RpcException {
                return null;
            }

            @Override
            public URL getUrl() {
                return connection.getUrl();
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public void destroy() {

            }
        });
        Request frameReq = new Request();
        frameReq.setData(invocation);
        final DefaultFuture2 df = DefaultFuture2.newFuture(connection, frameReq, 3000, Executors.newSingleThreadExecutor());
        try {
            final ChannelFuture future = connection.write(frameReq);
            future.addListener(f1 -> {
                if (f1.isSuccess()) {
                    DefaultFuture2.sent(frameReq);
                } else {
                    Response response = new Response(frameReq.getId());
                    response.setErrorMessage(future.cause().getMessage());
                    DefaultFuture2.received(connection, response);
                    f1.cause().printStackTrace();
                }
            });
        } catch (RemotingException e) {
            e.printStackTrace();
        }
        df.thenApply(response -> (AppResponse) response)
            .thenAccept(response -> {
                if (response.hasException()) {
                    responseObserver.onError(response.getException());
                } else {
                    responseObserver.onNext((RespT) response.getValue());
                    responseObserver.onCompleted();
                }
            });
    }

    public static ReflectedMethodDescriptor getSayHelloMethod() {
        if (sayHelloMethod != null) {
            return sayHelloMethod;
        }
        synchronized (GreeterStub.class) {
            if (sayHelloMethod != null) {
                return sayHelloMethod;
            }
            sayHelloMethod = new ReflectedMethodDescriptor("greet", HelloRequest.class, HelloReply.class, ReflectedMethodDescriptor.RpcType.UNARY);
        }
        return sayHelloMethod;
    }

    public static ServiceDescriptor getServiceDescriptor() {
        if (serviceDescriptor != null) {
            return serviceDescriptor;
        }
        synchronized (GreeterStub.class) {
            if (serviceDescriptor != null) {
                return serviceDescriptor;
            } else {
                final ModuleModel model = ApplicationModel.defaultModel().getDefaultModule();
                serviceDescriptor = model.getServiceRepository().registerService(GreeterStub.class);
            }
        }
        return serviceDescriptor;
    }

    @Override
    public HelloReply sayHello(HelloRequest request) {
        return null;
    }

    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        asyncCall(connection, getSayHelloMethod(), request, responseObserver);
    }

    @Override
    public StreamObserver<HelloRequest> sayHello(StreamObserver<HelloReply> responseObserver) {
        return null;
    }

    public static abstract class IGreeterImplBase implements IGreeter {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            ServerCallUtil.callUnimplementedMethod(sayHelloMethod, responseObserver);
        }
    }
}
