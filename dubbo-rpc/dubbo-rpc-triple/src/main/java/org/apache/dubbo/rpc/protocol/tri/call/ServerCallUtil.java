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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;

public class ServerCallUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCallUtil.class);

    public static ServerCall.Listener startCall(ServerCall call,
        RpcInvocation invocation,
        MethodDescriptor methodDescriptor,
        Invoker<?> invoker) {
        CancellationContext cancellationContext = RpcContext.getCancellationContext();
        ServerCallToObserverAdapter<Object> responseObserver = new ServerCallToObserverAdapter<>(call,
            cancellationContext);
        return startCall(call, methodDescriptor, invocation, invoker, responseObserver);
    }

    public static ServerCall.Listener startCall(ServerCall call,
        MethodDescriptor methodDescriptor,
        RpcInvocation invocation,
        Invoker<?> invoker,
        ServerCallToObserverAdapter<Object> responseObserver) {
        try {
            ServerCall.Listener listener;
            switch (methodDescriptor.getRpcType()) {
                case UNARY:
                    listener = new UnaryServerCallListener(invocation, invoker, responseObserver);
                    call.requestN(2);
                    break;
                case SERVER_STREAM:
                    listener = new ServerStreamServerCallListener(invocation, invoker, responseObserver);
                    call.requestN(2);
                    break;
                case BI_STREAM:
                case CLIENT_STREAM:
                    listener = new BiStreamServerCallListener(invocation, invoker, responseObserver);
                    call.requestN(1);
                    break;
                default:
                    throw new IllegalStateException("Can not reach here");
            }
            return listener;
        } catch (Throwable t) {
            LOGGER.error("Create triple stream failed", t);
            responseObserver.onError(TriRpcStatus.INTERNAL.withDescription("Create stream failed")
                .withCause(t)
                .asException());
        }
        return null;
    }
}

