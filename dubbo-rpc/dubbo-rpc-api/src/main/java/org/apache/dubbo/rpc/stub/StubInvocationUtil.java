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
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.proxy.InvocationUtil;

public class StubInvocationUtil {

    public static <T, R> R unaryCall(Invoker<?> invoker, StubMethodDescriptor methodDescriptor, T request) {
        RpcInvocation rpcInvocation = new RpcInvocation(invoker.getUrl().getServiceModel(),
            methodDescriptor.getMethodName(), invoker.getInterface().getName(),
            invoker.getUrl().getProtocolServiceKey(), methodDescriptor.getParameterClasses(), new Object[]{request});
        try {
            return (R) InvocationUtil.invoke(invoker, rpcInvocation);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    public static <T, R> void unaryCall(Invoker<?> invoker,
        MethodDescriptor method,
        T request,
        StreamObserver<R> responseObserver) {

    }

    public static <T, R> StreamObserver<T> biOrClientStreamCall(Invoker<?> invoker,
        MethodDescriptor method,
        StreamObserver<R> responseObserver) {
        RpcInvocation rpcInvocation = new RpcInvocation(invoker.getUrl().getServiceModel(), method.getMethodName(),
            invoker.getInterface().getName(), invoker.getUrl().getProtocolServiceKey(), method.getParameterClasses(),
            new Object[]{responseObserver});
        try {
            return (StreamObserver<T>) InvocationUtil.invoke(invoker, rpcInvocation);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static <T, R> void serverStreamCall(Invoker<?> invoker,
        MethodDescriptor method,
        T request,
        StreamObserver<R> responseObserver) {
        RpcInvocation rpcInvocation = new RpcInvocation(invoker.getUrl().getServiceModel(), method.getMethodName(),
            invoker.getInterface().getName(), invoker.getUrl().getProtocolServiceKey(), method.getParameterClasses(),
            new Object[]{request, responseObserver});
        try {
            InvocationUtil.invoke(invoker, rpcInvocation);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
