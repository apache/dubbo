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
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.DefaultFuture2;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;

public class ClientCallUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCallUtil.class);

    public static void call(ClientCall call, RequestMetadata metadata) {
        try {
            switch (metadata.method.getRpcType()) {
                case UNARY:
                    if (metadata.arguments.length == 2 && metadata.arguments[1] instanceof StreamObserver) {
                        streamCall(call, metadata);
                    } else {
                        unaryCall(call, metadata);
                    }
                    break;
                case CLIENT_STREAM:
                case SERVER_STREAM:
                case BI_STREAM:
                    streamCall(call, metadata);
                    break;
                default:
                    throw new IllegalStateException("Can not reach here");
            }
        } catch (Throwable t) {
            cancelByThrowable(call, t);
            final TriRpcStatus status = TriRpcStatus.INTERNAL.withCause(t)
                .withDescription("Call aborted cause client exception");
            DefaultFuture2.received(metadata.requestId, status, null);
        }
    }

    public static void streamCall(ClientCall call, RequestMetadata metadata) {
        AppResponse appResponse = new AppResponse();
        if (metadata.method.getRpcType() == MethodDescriptor.RpcType.SERVER_STREAM) {
            callServerStream(call, metadata);
        } else {
            callBiOrClientStream(call, metadata, appResponse);
        }
        DefaultFuture2.sent(metadata.requestId);
        DefaultFuture2.received(metadata.requestId, TriRpcStatus.OK, appResponse);
    }

    private static void callBiOrClientStream(ClientCall call, RequestMetadata metadata, AppResponse appResponse) {
        StreamObserver<Object> responseObserver = (StreamObserver<Object>) metadata.arguments[0];
        final StreamObserver<Object> requestObserver = streamCall(call, metadata, responseObserver);
        appResponse.setValue(requestObserver);
    }

    private static void callServerStream(ClientCall call, RequestMetadata metadata) {
        Object request = metadata.arguments[0];
        StreamObserver<Object> responseObserver = (StreamObserver<Object>) metadata.arguments[1];
        final StreamObserver<Object> requestObserver = streamCall(call, metadata, responseObserver);
        requestObserver.onNext(request);
        requestObserver.onCompleted();
    }

    public static StreamObserver<Object> streamCall(ClientCall call,
        RequestMetadata metadata,
        StreamObserver<Object> responseObserver) {
        if (responseObserver instanceof CancelableStreamObserver) {
            final CancellationContext context = new CancellationContext();
            ((CancelableStreamObserver<Object>) responseObserver).setCancellationContext(context);
            context.addListener(context1 -> call.cancel("Canceled by app", null));
        }
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(responseObserver,
            metadata.method.getRpcType() == MethodDescriptor.RpcType.UNARY,
            new ClientCallToObserverAdapter<>(call));
        return callDirectly(call, metadata, listener);
    }

    public static void unaryCall(ClientCall call, RequestMetadata metadata) {
        final UnaryClientCallListener listener = new UnaryClientCallListener(metadata.requestId, call);
        final StreamObserver<Object> requestObserver = callDirectly(call, metadata, listener);
        Object argument;
        if (metadata.packableMethod.singleArgument()) {
            argument = metadata.arguments[0];
        } else {
            argument = metadata.arguments;
        }
        requestObserver.onNext(argument);
        requestObserver.onCompleted();
        DefaultFuture2.sent(metadata.requestId);
    }

    public static StreamObserver<Object> callDirectly(ClientCall call,
        RequestMetadata metadata,
        ClientCall.StartListener responseListener) {
        final ClientCallToObserverAdapter<Object> requestObserver = new ClientCallToObserverAdapter<>(call);
        call.start(metadata, responseListener);
        return requestObserver;
    }

    static void cancelByThrowable(ClientCall call, Throwable t) {
        try {
            call.cancel("Canceled by error", t);
        } catch (Throwable t1) {
            LOGGER.error("Cancel triple request failed", t1);
        }
    }
}
