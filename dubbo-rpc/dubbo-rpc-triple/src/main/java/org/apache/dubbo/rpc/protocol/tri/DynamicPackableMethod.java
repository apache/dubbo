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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.PackableMethod;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

import static org.apache.dubbo.common.constants.CommonConstants.$ECHO;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOBUF_MESSAGE_CLASS_NAME;
import static org.apache.dubbo.remoting.Constants.DEFAULT_REMOTING_SERIALIZATION;
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.rpc.protocol.tri.TripleProtocol.METHOD_ATTR_PACK;

public class DynamicPackableMethod implements PackableMethod {

    private static final String GRPC_ASYNC_RETURN_CLASS = "com.google.common.util.concurrent.ListenableFuture";
    private static final String TRI_ASYNC_RETURN_CLASS = "java.util.concurrent.CompletableFuture";
    private static final String REACTOR_RETURN_CLASS = "reactor.core.publisher.Mono";
    private static final String RX_RETURN_CLASS = "io.reactivex.Single";
    private static final String GRPC_STREAM_CLASS = "io.grpc.stub.StreamObserver";
    private static final Pack PB_PACK = o -> ((Message) o).toByteArray();

    private Class<?> requestType;
    private Class<?> responseType;
    private final Pack requestPack;
    private final Pack responsePack;
    private final UnPack requestUnpack;
    private final UnPack responseUnpack;
    private final boolean singleArgument;

    public static DynamicPackableMethod init(MethodDescriptor methodDescriptor, URL url) {
        final String serializeName = url.getParameter(SERIALIZATION_KEY, DEFAULT_REMOTING_SERIALIZATION);
        Object stored = methodDescriptor.getAttribute(METHOD_ATTR_PACK);
        if (stored != null) {
            return (DynamicPackableMethod) stored;
        }
        DynamicPackableMethod dynamicPackableMethod = new DynamicPackableMethod(methodDescriptor, url, serializeName);
        methodDescriptor.addAttribute(METHOD_ATTR_PACK, dynamicPackableMethod);
        return dynamicPackableMethod;
    }

    @Override
    public boolean singleArgument() {
        return singleArgument;
    }

    public DynamicPackableMethod(MethodDescriptor methodDescriptor, URL url, String serializeName) {
        if (!needWrap(methodDescriptor)) {
            requestPack = PB_PACK;
            responsePack = PB_PACK;
            requestUnpack = new PbUnpack<>(requestType);
            responseUnpack = new PbUnpack<>(responseType);
            singleArgument = true;
        } else {
            final MultipleSerialization serialization = url.getOrDefaultFrameworkModel()
                .getExtensionLoader(MultipleSerialization.class)
                .getExtension(url.getParameter(Constants.MULTI_SERIALIZATION_KEY, CommonConstants.DEFAULT_KEY));
            this.requestPack = new WrapRequestPack(serialization, url, serializeName,
                methodDescriptor.getCompatibleParamSignatures());
            this.responsePack = new WrapResponsePack(serialization, url, methodDescriptor.getReturnClass().getName());
            this.requestUnpack = new WrapRequestUnpack(serialization, url);
            this.responseUnpack = new WrapResonseUnpack(serialization, url);
            this.singleArgument = false;
        }
    }

    @Override
    public Pack getRequestPack() {
        return requestPack;
    }

    @Override
    public Pack getResponsePack() {
        return responsePack;
    }

    @Override
    public UnPack getResponseUnpack() {
        return responseUnpack;
    }

    @Override
    public UnPack getRequestUnpack() {
        return requestUnpack;
    }


    private boolean isStreamType(Class<?> classType) {
        return StreamObserver.class.isAssignableFrom(classType);
    }

    /**
     * Determine if the request and response instance should be wrapped in Protobuf wrapper object
     *
     * @return true if the request and response object is not generated by protobuf
     */
    protected boolean needWrap(MethodDescriptor methodDescriptor) {
        // generic call must be wrapped
        if (methodDescriptor.isGeneric()) {
            return true;
        }
        // echo must be wrapped
        if ($ECHO.equals(methodDescriptor.getMethodName())) {
            return true;
        }
        if (canNotHandlerClass(methodDescriptor.getReturnClass()) || Arrays.stream(
            methodDescriptor.getParameterClasses()).anyMatch(this::canNotHandlerClass)) {
            throw new IllegalStateException(
                "Bad stream method signature. method(" + methodDescriptor.getMethodName() + ":" + methodDescriptor.getParamDesc() + ")");
        }

        int protobufParameterCount = 0;
        int javaParameterCount = 0;
        int streamParamterCount = 0;
        for (Class<?> parameterClass : methodDescriptor.getParameterClasses()) {
            if (methodDescriptor.getRpcType() != MethodDescriptor.RpcType.UNARY && isStreamType(parameterClass)) {
                streamParamterCount++;
                if (streamParamterCount > 1) {
                    // more than one stream param
                    throw new IllegalStateException(
                        "method params error: more than one Stream params. method=" + methodDescriptor.getMethodName());
                }
            }
            if (isProtobufClass(parameterClass)) {
                protobufParameterCount++;
            } else {
                javaParameterCount++;
            }
            if (protobufParameterCount > 0 && javaParameterCount > 0) {
                throw new IllegalStateException("Bad method type, can not mix protobuf and normal pojo");
            }
        }
        // protobuf only support one param
        if (protobufParameterCount >= 2) {
            throw new IllegalStateException(
                "method params error: more than one protobuf params. method=" + methodDescriptor.getMethodName());
        }

        boolean returnClassProtobuf = isProtobufClass(methodDescriptor.getReturnClass());
        if (protobufParameterCount > 0 && returnClassProtobuf) {
            return false;
        }

        if (protobufParameterCount <= 0 && !returnClassProtobuf) {
            return true;
        }
        // bidirectional-stream: StreamObserver<Request> foo(StreamObserver<Response>)
        if (methodDescriptor.getParameterClasses().length == 1 && isStreamType(
            methodDescriptor.getParameterClasses()[0])) {
            this.requestType = (Class<?>) ((ParameterizedType) methodDescriptor.getMethod()
                .getGenericReturnType()).getActualTypeArguments()[0];
            this.responseType = (Class<?>) ((ParameterizedType) methodDescriptor.getMethod()
                .getGenericParameterTypes()[0]).getActualTypeArguments()[0];
            // server-stream: void foo(Request, StreamObserver<Response>)
        } else {
            this.requestType = methodDescriptor.getMethod().getParameterTypes()[0];
            this.responseType = (Class<?>) ((ParameterizedType) methodDescriptor.getMethod()
                .getGenericParameterTypes()[1]).getActualTypeArguments()[0];
        }
        if (isProtobufClass(requestType) && isProtobufClass(responseType)) {
            return false;
        } else if (!isProtobufClass(requestType) && !isProtobufClass(responseType)) {
            return true;
        }

        // java param should be wrapped
        return javaParameterCount > 0;
    }


    private boolean isMono(Class<?> clz) {
        return REACTOR_RETURN_CLASS.equalsIgnoreCase(clz.getName());
    }

    private boolean isRx(Class<?> clz) {
        return RX_RETURN_CLASS.equalsIgnoreCase(clz.getName());
    }

    public boolean isProtobufClass(Class<?> clazz) {
        while (clazz != Object.class && clazz != null) {
            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length > 0) {
                for (Class<?> clazzInterface : interfaces) {
                    if (PROTOBUF_MESSAGE_CLASS_NAME.equalsIgnoreCase(clazzInterface.getName())) {
                        return true;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    private boolean canNotHandlerClass(Class<?> type) {
        return isMono(type) || isProtobufClass(type) || isGrpcStreamType(type) || isFuture(type) || isRx(type);
    }

    private boolean isGrpcStreamType(Class<?> classType) {
        return GRPC_STREAM_CLASS.equalsIgnoreCase(classType.getName());
    }

    private boolean isFuture(Class<?> type) {
        return TRI_ASYNC_RETURN_CLASS.equalsIgnoreCase(type.getName()) || GRPC_ASYNC_RETURN_CLASS.equals(
            type.getName());
    }

    private static class WrapResponsePack implements Pack {
        private final MultipleSerialization multipleSerialization;
        private final URL url;
        private final String returnType;
        String serialize;

        private WrapResponsePack(MultipleSerialization multipleSerialization, URL url, String returnType) {
            this.multipleSerialization = multipleSerialization;
            this.url = url;
            this.returnType = returnType;
        }

        @Override
        public byte[] pack(Object obj) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            multipleSerialization.serialize(url, serialize, null, obj, bos);
            return TripleWrapper.TripleResponseWrapper.newBuilder()
                .setSerializeType(serialize)
                .setType(returnType)
                .setData(ByteString.copyFrom(bos.toByteArray()))
                .build()
                .toByteArray();
        }
    }

    private class WrapRequestUnpack implements UnPack {
        private final MultipleSerialization serialization;
        private final URL url;

        private WrapRequestUnpack(MultipleSerialization serialization, URL url) {
            this.serialization = serialization;
            this.url = url;
        }

        @Override
        public Object unpack(byte[] data) throws IOException, ClassNotFoundException {
            TripleWrapper.TripleRequestWrapper wrapper = TripleWrapper.TripleRequestWrapper.parseFrom(data);
            Object[] ret = new Object[wrapper.getArgsCount()];
            final String serializeType = convertHessianFromWrapper(wrapper.getSerializeType());
            ((WrapResponsePack) responsePack).serialize = serializeType;
            for (int i = 0; i < wrapper.getArgsList().size(); i++) {
                ByteArrayInputStream bais = new ByteArrayInputStream(wrapper.getArgs(i).toByteArray());
                ret[i] = serialization.deserialize(url, serializeType, wrapper.getArgTypes(i), bais);
            }
            return ret;
        }
    }

    private static class WrapResonseUnpack implements UnPack {
        private final MultipleSerialization serialization;
        private final URL url;

        private WrapResonseUnpack(MultipleSerialization serialization, URL url) {
            this.serialization = serialization;
            this.url = url;
        }

        @Override
        public Object unpack(byte[] data) throws IOException, ClassNotFoundException {
            TripleWrapper.TripleResponseWrapper wrapper = TripleWrapper.TripleResponseWrapper.parseFrom(data);
            final String serializeType = convertHessianFromWrapper(wrapper.getSerializeType());
            ByteArrayInputStream bais = new ByteArrayInputStream(wrapper.getData().toByteArray());
            return serialization.deserialize(url, serializeType, wrapper.getType(), bais);
        }
    }

    private static class WrapRequestPack implements Pack {
        private final String serialize;
        private final MultipleSerialization multipleSerialization;
        private final String[] argumentsType;
        private final URL url;

        private WrapRequestPack(MultipleSerialization multipleSerialization,
            URL url,
            String serialize,
            String[] argumentsType) {
            this.url = url;
            this.serialize = convertHessianToWrapper(serialize);
            this.multipleSerialization = multipleSerialization;
            this.argumentsType = argumentsType;
        }

        @Override
        public byte[] pack(Object obj) throws IOException {
            Object[] arguments;
            if (obj instanceof Object[]) {
                arguments = (Object[]) obj;
            } else {
                throw new IllegalArgumentException("Wrap request's arguments must be an object array");
            }
            final TripleWrapper.TripleRequestWrapper.Builder builder = TripleWrapper.TripleRequestWrapper.newBuilder()
                .setSerializeType(serialize);
            for (String type : argumentsType) {
                builder.addArgTypes(type);
            }
            for (Object argument : arguments) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                multipleSerialization.serialize(url, serialize, null, argument, bos);
                builder.addArgs(ByteString.copyFrom(bos.toByteArray()));
            }
            return builder.build().toByteArray();
        }

        /**
         * Convert hessian version from Dubbo's SPI version(hessian2) to wrapper API version (hessian4)
         *
         * @param serializeType literal type
         * @return hessian4 if the param is hessian2, otherwise return the param
         */
        private String convertHessianToWrapper(String serializeType) {
            if (TripleConstant.HESSIAN2.equals(serializeType)) {
                return TripleConstant.HESSIAN4;
            }
            return serializeType;
        }

    }

    private static String convertHessianFromWrapper(String serializeType) {
        if (TripleConstant.HESSIAN4.equals(serializeType)) {
            return TripleConstant.HESSIAN2;
        }
        return serializeType;
    }
}
