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
import org.apache.dubbo.rpc.model.StreamMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.DefaultFuture2;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.RpcStatus;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.observer.WrapperRequestObserver;

public class ClientCallUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCallUtil.class);

    public static void call(ClientCall call, RequestMetadata metadata) {
        if (metadata.method instanceof StreamMethodDescriptor) {
            streamCall(call, metadata);
        } else {
            unaryCall(call, metadata);
        }
    }

    public static void streamCall(ClientCall call,
                                  RequestMetadata metadata) {
        AppResponse appResponse = new AppResponse();
        StreamMethodDescriptor methodDescriptor = (StreamMethodDescriptor) metadata.method;
        if (methodDescriptor.isServerStream()) {
            callServerStream(call, metadata);
        } else {
            callBiOrClientStream(call, metadata, appResponse);
        }
        // for timeout
        DefaultFuture2.sent(metadata.requestId);
        final RpcStatus status = RpcStatus.OK;
        DefaultFuture2.received(metadata.requestId, status, appResponse);
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
                                                    StreamObserver<Object> responseObserver
    ) {
        if (responseObserver instanceof CancelableStreamObserver) {
            final CancellationContext context = new CancellationContext();
            ((CancelableStreamObserver<Object>) responseObserver).setCancellationContext(context);
            context.addListener(context1 -> call.cancel("Canceled by app", null));
        }
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(responseObserver);
        return call(call, metadata, listener);
    }

    public static void unaryCall(ClientCall call, RequestMetadata metadata) {
        final UnaryCallListener listener = new UnaryCallListener(metadata.requestId);
        final StreamObserver<Object> requestObserver = call(call, metadata, listener);
        try {
            Object argument;
            if (metadata.method.isNeedWrap()) {
                argument = metadata.arguments;
            } else {
                argument = metadata.arguments[0];
            }
            requestObserver.onNext(argument);
            requestObserver.onCompleted();
        } catch (Throwable t) {
            cancelByThrowable(call, t);
        }
    }

    public static StreamObserver<Object> call(ClientCall call, RequestMetadata metadata,
                                              ClientCall.Listener responseListener) {

        if (metadata.method.isNeedWrap()) {
            return wrapperCall(call, metadata, responseListener);
        }
        return callDirectly(call, metadata, responseListener);
    }

    public static StreamObserver<Object> wrapperCall(ClientCall call, RequestMetadata metadata,
                                                     ClientCall.Listener responseListener) {
        final StreamObserver<Object> requestObserver = WrapperRequestObserver.wrap(new ClientCallToObserverAdapter<>(call),
                metadata.argumentTypes, metadata.genericPack);
        final ClientCall.Listener wrapResponseListener = WrapResponseCallListener.wrap(responseListener, metadata.genericUnpack);
        call.start(metadata, wrapResponseListener);
        return requestObserver;
    }

    public static StreamObserver<Object> callDirectly(ClientCall call, RequestMetadata metadata, ClientCall.Listener responseListener) {
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
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else {
            throw (Error) t;
        }
    }
}
