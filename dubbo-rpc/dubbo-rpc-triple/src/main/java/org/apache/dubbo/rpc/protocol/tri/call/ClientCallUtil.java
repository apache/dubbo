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
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.StreamMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.DefaultFuture2;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.observer.WrapperRequestObserver;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;

import java.util.List;

public class ClientCallUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCallUtil.class);

    public static void call(ClientCall call,
                            long requestId,
                            Object[] arguments,
                            MethodDescriptor methodDescriptor,
                            GenericPack genericPack,
                            List<String> argumentTypes,
                            GenericUnpack genericUnpack) {
        if (methodDescriptor instanceof StreamMethodDescriptor) {
            streamCall(call, requestId, arguments, (StreamMethodDescriptor) methodDescriptor, genericPack, argumentTypes, genericUnpack);
            return;
        }
        // unary call
        Object argument;
        if (methodDescriptor.isNeedWrap()) {
            argument = arguments;
        } else {
            argument = arguments[0];
        }
        unaryCall(call, argument, requestId, methodDescriptor, genericPack, argumentTypes, genericUnpack);
    }

    public static void streamCall(ClientCall call,
                                  long requestId,
                                  Object[] arguments,
                                  StreamMethodDescriptor methodDescriptor,
                                  GenericPack genericPack, List<String> argumentTypes,
                                  GenericUnpack genericUnpack) {
        AppResponse appResponse = new AppResponse();
        if (methodDescriptor.isServerStream()) {
            Object request = arguments[0];
            StreamObserver<Object> responseObserver = (StreamObserver<Object>) arguments[1];
            final StreamObserver<Object> requestObserver = streamCall(call, responseObserver, methodDescriptor, genericPack, argumentTypes, genericUnpack);
            requestObserver.onNext(request);
            requestObserver.onCompleted();
        } else {
            StreamObserver<Object> responseObserver = (StreamObserver<Object>) arguments[0];
            final StreamObserver<Object> requestObserver = streamCall(call, responseObserver, methodDescriptor, genericPack, argumentTypes, genericUnpack);
            appResponse.setValue(requestObserver);
        }
        // for timeout
        DefaultFuture2.sent(requestId);
        final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.OK);
        DefaultFuture2.received(requestId, status, appResponse);
    }

    public static StreamObserver<Object> streamCall(ClientCall call,
                                                    StreamObserver<Object> responseObserver,
                                                    StreamMethodDescriptor methodDescriptor,
                                                    GenericPack genericPack, List<String> argumentTypes,
                                                    GenericUnpack genericUnpack) {
        if (responseObserver instanceof CancelableStreamObserver) {
            final CancellationContext context = new CancellationContext();
            ((CancelableStreamObserver<Object>) responseObserver).setCancellationContext(context);
            context.addListener(context1 -> call.cancel("Canceled by app", null));
        }
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(responseObserver);
        return call(call, methodDescriptor, listener, genericPack, argumentTypes, genericUnpack);
    }

    public static void unaryCall(ClientCall call, Object request, long requestId,
                                 MethodDescriptor methodDescriptor,
                                 GenericPack genericPack, List<String> argumentTypes,
                                 GenericUnpack genericUnpack) {
        final UnaryCallListener listener = new UnaryCallListener(requestId);
        final StreamObserver<Object> requestObserver = call(call, methodDescriptor, listener, genericPack, argumentTypes, genericUnpack);
        try {
            requestObserver.onNext(request);
            requestObserver.onCompleted();
        } catch (Throwable t) {
            cancelByThrowable(call, t);
        }
    }

    public static StreamObserver<Object> call(ClientCall call, MethodDescriptor methodDescriptor,
                                              ClientCall.Listener responseListener,
                                              GenericPack genericPack, List<String> argumentTypes,
                                              GenericUnpack genericUnpack) {

        if (methodDescriptor.isNeedWrap()) {
            return wrapperCall(call, responseListener, genericPack, argumentTypes, genericUnpack);
        }
        return call(call, responseListener);
    }

    public static StreamObserver<Object> wrapperCall(ClientCall call, ClientCall.Listener responseListener,
                                                     GenericPack genericPack, List<String> argumentTypes,
                                                     GenericUnpack genericUnpack) {
        final StreamObserver<Object> requestObserver = WrapperRequestObserver.wrap(new ClientCallToObserverAdapter<>(call), argumentTypes, genericPack);
        final ClientCall.Listener wrapResponseListener = WrapResponseCallListener.wrap(responseListener, genericUnpack);
        call.start(wrapResponseListener);
        return requestObserver;
    }

    public static StreamObserver<Object> call(ClientCall call, ClientCall.Listener responseListener) {
        final ClientCallToObserverAdapter<Object> requestObserver = new ClientCallToObserverAdapter<>(call);
        call.start(responseListener);
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
