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

package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.proxy.InvocationUtil;

import java.lang.reflect.Type;

public class StubInvocationUtil {

    public static <T, R> R unaryCall(Invoker<?> invoker, MethodDescriptor methodDescriptor,
        T request) {
        return (R) call(invoker, methodDescriptor, new Object[]{request});
    }

    public static <T, R> void unaryCall(Invoker<?> invoker, MethodDescriptor method, T request,
        StreamObserver<R> responseObserver) {
        try {
            Object res = unaryCall(invoker, method, request);
            responseObserver.onNext((R) res);
        } catch (Exception e) {
            responseObserver.onError(e);
        }
        responseObserver.onCompleted();
    }

    public static <T, R> StreamObserver<T> biOrClientStreamCall(Invoker<?> invoker,
        MethodDescriptor method, StreamObserver<R> responseObserver) {
        return (StreamObserver<T>) call(invoker, method, new Object[]{responseObserver});
    }

    public static <T, R> void serverStreamCall(Invoker<?> invoker, MethodDescriptor method,
        T request, StreamObserver<R> responseObserver) {
        call(invoker, method, new Object[]{request, responseObserver});
    }

    private static Object call(Invoker<?> invoker, MethodDescriptor methodDescriptor,
        Object[] arguments) {
        //Due to the design of ServiceDescriptor, the MethodDescriptor with the same name will override each other in ServiceDescriptor.
        //The sync and async version of MethodDescriptor share the same method name, and usually the sync version will replace another one
        //(Because it's declared after the async version).
        //So this RpcInvocation always uses MethodDescriptor of sync version to initialize.
        RpcInvocation rpcInvocation = new RpcInvocation(invoker.getUrl().getServiceModel(),
            methodDescriptor.getMethodName(), invoker.getInterface().getName(),
            invoker.getUrl().getProtocolServiceKey(), methodDescriptor.getParameterClasses(),
            arguments);

        //When there are multiple MethodDescriptors with the same method name, the return type will be wrong
        rpcInvocation.setReturnType(methodDescriptor.getReturnClass());

        Type[] returnTypes = methodDescriptor.getReturnTypes();
        //If this MethodDescriptor from an async method, its return class should be CompletableFuture.
        //If this MethodDescriptor from a sync method, its return class is real return type.
        //The definition of returnTypes:
        //returnTypes[0], the concrete type.
        //returnTypes[1], the parameterized type.
        returnTypes[0] = methodDescriptor.getReturnClass();
        rpcInvocation.setReturnTypes(returnTypes);

        try {
            return InvocationUtil.invoke(invoker, rpcInvocation);
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw TriRpcStatus.INTERNAL
                    .withCause(e)
                    .asException();
            }
        }
    }
}
