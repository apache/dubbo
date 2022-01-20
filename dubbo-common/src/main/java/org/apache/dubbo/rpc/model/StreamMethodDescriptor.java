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

package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.stream.StreamObserver;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

public class StreamMethodDescriptor extends MethodDescriptor {


    private static final String GRPC_ASYNC_RETURN_CLASS = "com.google.common.util.concurrent.ListenableFuture";
    private static final String TRI_ASYNC_RETURN_CLASS = "java.util.concurrent.CompletableFuture";
    private static final String REACTOR_RETURN_CLASS = "reactor.core.publisher.Mono";
    private static final String RX_RETURN_CLASS = "io.reactivex.Single";
    private static final String GRPC_STREAM_CLASS = "io.grpc.stub.StreamObserver";

    public final Class<?> requestType;
    public final Class<?> responseType;
    public final StreamType streamType;

    public StreamMethodDescriptor(Method method) {
        super(method);
        Class<?>[] parameterTypes = method.getParameterTypes();
        // bidirectional-stream: StreamObserver<Request> foo(StreamObserver<Response>)
        if (parameterTypes.length == 1 && isStreamType(parameterTypes[0])) {
            this.requestType =
                (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
            this.responseType = (Class<?>) ((ParameterizedType) method.getGenericParameterTypes()[0])
                .getActualTypeArguments()[0];
            this.streamType = StreamType.BI_DIRECTIONAL;
            // server-stream: void foo(Request, StreamObserver<Response>)
        } else {
            this.requestType = method.getParameterTypes()[0];
            this.responseType =
                (Class<?>) ((ParameterizedType) method.getGenericParameterTypes()[1]).getActualTypeArguments()[0];
            this.streamType = StreamType.SERVER;
        }
    }

    public static boolean isStreamMethod(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        return (parameterTypes.length == 1 && isStreamType(parameterTypes[0])) ||
            (parameterTypes.length == 2 && method.getReturnType().equals(Void.TYPE)
                && !isStreamType(parameterTypes[0]) && isStreamType(parameterTypes[1]));
    }

    private static boolean isStreamType(Class<?> clz) {
        return StreamObserver.class.isAssignableFrom(clz) || GRPC_STREAM_CLASS.equalsIgnoreCase(clz.getName());
    }

    @Override
    protected boolean needWrap() {
        if (isProtobufClass(requestType) && isProtobufClass(responseType)) {
            return false;
        } else if (!isProtobufClass(requestType) && !isProtobufClass(responseType)) {
            return true;
        }
        throw new IllegalStateException("method params error method=" + methodName);
    }

    public int responseObserverIndex() {
        if (streamType == StreamType.SERVER) {
            return 1;
        } else {
            return 0;
        }
    }

    public enum StreamType {
        SERVER, CLIENT, BI_DIRECTIONAL
    }
}
