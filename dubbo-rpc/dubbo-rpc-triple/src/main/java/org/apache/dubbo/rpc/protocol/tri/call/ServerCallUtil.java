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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.StreamMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.observer.WrapperResponseObserver;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;

public class ServerCallUtil {

    public static ServerCall.Listener startCall(ServerCall call,
                                                RpcInvocation invocation,
                                                MethodDescriptor methodDescriptor,
                                                GenericUnpack genericUnpack,
                                                Invoker<?> invoker) {
        ServerCall.Listener listener;
        CancellationContext cancellationContext = RpcContext.getCancellationContext();
        ServerCallToObserverAdapter<Object> responseObserver;
        if (methodDescriptor.isNeedWrap()) {
            responseObserver = new WrapperResponseObserver<>(call, cancellationContext, invocation.getReturnType().getName(),
                genericUnpack.serialization, invoker.getUrl());
        } else {
            responseObserver = new ServerCallToObserverAdapter<>(call, cancellationContext);
        }
        if (methodDescriptor instanceof StreamMethodDescriptor) {
            if (((StreamMethodDescriptor) methodDescriptor).isServerStream()) {
                listener = new ServerStreamServerCallListener(call, invocation, invoker, responseObserver);
            } else {
                listener = new BiStreamServerCallListener(call, invocation, invoker, responseObserver);
            }
            call.requestN(1);
        } else {
            listener = new UnaryServerCallListener(call, invocation, invoker, responseObserver);
            call.requestN(2);
        }
        if (methodDescriptor.isNeedWrap()) {
            listener = new WrapRequestServerCallListener(call, listener, genericUnpack);
        }
        return listener;
    }
}
