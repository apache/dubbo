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
import java.util.Iterator;
import java.util.stream.Stream;

import static org.apache.dubbo.common.constants.CommonConstants.$ECHO;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOBUF_MESSAGE_CLASS_NAME;
import static org.apache.dubbo.remoting.Constants.DEFAULT_REMOTING_SERIALIZATION;
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.rpc.protocol.tri.TripleProtocol.METHOD_ATTR_PACK;

public class ReflectionPackableMethod implements PackableMethod {

    private static final String GRPC_ASYNC_RETURN_CLASS = "com.google.common.util.concurrent.ListenableFuture";
    private static final String TRI_ASYNC_RETURN_CLASS = "java.util.concurrent.CompletableFuture";
    private static final String REACTOR_RETURN_CLASS = "reactor.core.publisher.Mono";
    private static final String RX_RETURN_CLASS = "io.reactivex.Single";
    private static final String GRPC_STREAM_CLASS = "io.grpc.stub.StreamObserver";
    private static final Pack PB_PACK = o -> ((Message) o).toByteArray();

    private final Pack requestPack;
    private final Pack responsePack;
    private final UnPack requestUnpack;
    private final UnPack responseUnpack;

    public ReflectionPackableMethod(MethodDescriptor method, URL url, String serializeName) {
        Class<?>[] actualRequestTypes;
        Class<?> actualResponseType;
        switch (method.getRpcType()) {
            case CLIENT_STREAM:
            case BI_STREAM:
                actualRequestTypes = new Class<?>[]{
                    (Class<?>) ((ParameterizedType) method.getMethod()
                        .getGenericReturnType()).getActualTypeArguments()[0]};
                actualResponseType = (Class<?>) ((ParameterizedType) method.getMethod()
                    .getGenericParameterTypes()[0]).getActualTypeArguments()[0];
                break;
            case SERVER_STREAM:
                actualRequestTypes = method.getMethod().getParameterTypes();
                actualResponseType = (Class<?>) ((ParameterizedType) method.getMethod()
                    .getGenericParameterTypes()[1]).getActualTypeArguments()[0];
                break;
            case UNARY:
                actualRequestTypes = method.getParameterClasses();
                actualResponseType = method.getReturnClass();
                break;
            default:
                throw new IllegalStateException("Can not reach here");
        }

        boolean singleArgument = method.getRpcType() != MethodDescriptor.RpcType.UNARY;
        if (!needWrap(method, actualRequestTypes, actualResponseType)) {
            requestPack = new PbArrayPacker(singleArgument);
            responsePack = PB_PACK;
            requestUnpack = new PbUnpack<>(actualRequestTypes[0]);
            responseUnpack = new PbUnpack<>(actualResponseType);
        } else {
            final MultipleSerialization serialization = url.getOrDefaultFrameworkModel()
                .getExtensionLoader(MultipleSerialization.class)
                .getExtension(url.getParameter(Constants.MULTI_SERIALIZATION_KEY,
                    CommonConstants.DEFAULT_KEY));
            String[] paramSigns = Stream.of(actualRequestTypes).map(Class::getName)
                .toArray(String[]::new);
            this.requestPack = new WrapRequestPack(serialization, url, serializeName, paramSigns,
                singleArgument);
            this.responsePack = new WrapResponsePack(serialization, url,
                actualResponseType.getName());
            this.requestUnpack = new WrapRequestUnpack(serialization, url);
            this.responseUnpack = new WrapResponseUnpack(serialization, url);
        }
    }

    public static ReflectionPackableMethod init(MethodDescriptor methodDescriptor, URL url) {
        final String serializeName = url.getParameter(SERIALIZATION_KEY,
            DEFAULT_REMOTING_SERIALIZATION);
        Object stored = methodDescriptor.getAttribute(METHOD_ATTR_PACK);
        if (stored != null) {
            return (ReflectionPackableMethod) stored;
        }
        ReflectionPackableMethod reflectionPackableMethod = new ReflectionPackableMethod(
            methodDescriptor, url, serializeName);
        methodDescriptor.addAttribute(METHOD_ATTR_PACK, reflectionPackableMethod);
        return reflectionPackableMethod;
    }

    static boolean isStreamType(Class<?> type) {
        return StreamObserver.class.isAssignableFrom(type) || GRPC_STREAM_CLASS.equalsIgnoreCase(
            type.getName());
    }

    /**
     * Determine if the request and response instance should be wrapped in Protobuf wrapper object
     *
     * @return true if the request and response object is not generated by protobuf
     */
    static boolean needWrap(MethodDescriptor methodDescriptor, Class<?>[] parameterClasses,
        Class<?> returnClass) {
        String methodName = methodDescriptor.getMethodName();
        // generic call must be wrapped
        if (CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(
            methodName)) {
            return true;
        }
        // echo must be wrapped
        if ($ECHO.equals(methodName)) {
            return true;
        }
        boolean returnClassProtobuf = isProtobufClass(returnClass);
        // Response foo()
        if (parameterClasses.length == 0) {
            return !returnClassProtobuf;
        }
        int protobufParameterCount = 0;
        int javaParameterCount = 0;
        int streamParameterCount = 0;
        boolean secondParameterStream = false;
        // count normal and protobuf param
        for (int i = 0; i < parameterClasses.length; i++) {
            Class<?> parameterClass = parameterClasses[i];
            if (isProtobufClass(parameterClass)) {
                protobufParameterCount++;
            } else {
                if (isStreamType(parameterClass)) {
                    if (i == 1) {
                        secondParameterStream = true;
                    }
                    streamParameterCount++;
                } else {
                    javaParameterCount++;
                }
            }
        }
        // more than one stream param
        if (streamParameterCount > 1) {
            throw new IllegalStateException(
                "method params error: more than one Stream params. method=" + methodName);
        }
        // protobuf only support one param
        if (protobufParameterCount >= 2) {
            throw new IllegalStateException(
                "method params error: more than one protobuf params. method=" + methodName);
        }
        // server stream support one normal param and one stream param
        if (streamParameterCount == 1) {
            if (javaParameterCount + protobufParameterCount > 1) {
                throw new IllegalStateException(
                    "method params error: server stream does not support more than one normal param."
                        + " method=" + methodName);
            }
            // server stream: void foo(Request, StreamObserver<Response>)
            if (!secondParameterStream) {
                throw new IllegalStateException(
                    "method params error: server stream's second param must be StreamObserver."
                        + " method=" + methodName);
            }
        }
        if (methodDescriptor.getRpcType() != MethodDescriptor.RpcType.UNARY) {
            if (MethodDescriptor.RpcType.SERVER_STREAM == methodDescriptor.getRpcType()) {
                if (!secondParameterStream) {
                    throw new IllegalStateException(
                        "method params error:server stream's second param must be StreamObserver."
                            + " method=" + methodName);
                }
            }
            // param type must be consistent
            if (returnClassProtobuf) {
                if (javaParameterCount > 0) {
                    throw new IllegalStateException(
                        "method params error: both normal and protobuf param found. method="
                            + methodName);
                }
            } else {
                if (protobufParameterCount > 0) {
                    throw new IllegalStateException("method params error method=" + methodName);
                }
            }
        } else {
            if (streamParameterCount > 0) {
                throw new IllegalStateException(
                    "method params error: unary method should not contain any StreamObserver."
                        + " method=" + methodName);
            }
            if (protobufParameterCount > 0 && returnClassProtobuf) {
                return false;
            }
            // handler reactor or rxjava only consider gen by proto
            if (isMono(returnClass) || isRx(returnClass)) {
                return false;
            }
            if (protobufParameterCount <= 0 && !returnClassProtobuf) {
                return true;
            }
            // handle grpc stub only consider gen by proto
            if (GRPC_ASYNC_RETURN_CLASS.equalsIgnoreCase(returnClass.getName())
                && protobufParameterCount == 1) {
                return false;
            }
            // handle dubbo generated method
            if (TRI_ASYNC_RETURN_CLASS.equalsIgnoreCase(returnClass.getName())) {
                Class<?> actualReturnClass = (Class<?>) ((ParameterizedType) methodDescriptor.getMethod()
                    .getGenericReturnType()).getActualTypeArguments()[0];
                boolean actualReturnClassProtobuf = isProtobufClass(actualReturnClass);
                if (actualReturnClassProtobuf && protobufParameterCount == 1) {
                    return false;
                }
                if (!actualReturnClassProtobuf && protobufParameterCount == 0) {
                    return true;
                }
            }
            // todo remove this in future
            boolean ignore = checkNeedIgnore(returnClass);
            if (ignore) {
                return protobufParameterCount != 1;
            }
            throw new IllegalStateException("method params error method=" + methodName);
        }
        // java param should be wrapped
        return javaParameterCount > 0;
    }

    /**
     * fixme will produce error on grpc. but is harmless so ignore now
     */
    static boolean checkNeedIgnore(Class<?> returnClass) {
        return Iterator.class.isAssignableFrom(returnClass);
    }

    static boolean isMono(Class<?> clz) {
        return REACTOR_RETURN_CLASS.equalsIgnoreCase(clz.getName());
    }

    static boolean isRx(Class<?> clz) {
        return RX_RETURN_CLASS.equalsIgnoreCase(clz.getName());
    }

    static boolean isProtobufClass(Class<?> clazz) {
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

    private static String convertHessianFromWrapper(String serializeType) {
        if (TripleConstant.HESSIAN4.equals(serializeType)) {
            return TripleConstant.HESSIAN2;
        }
        return serializeType;
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

    private static class WrapResponsePack implements Pack {

        private final MultipleSerialization multipleSerialization;
        private final URL url;
        private final String returnType;
        String serialize;

        private WrapResponsePack(MultipleSerialization multipleSerialization, URL url,
            String returnType) {
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

    private static class WrapResponseUnpack implements UnPack {

        private final MultipleSerialization serialization;
        private final URL url;

        private WrapResponseUnpack(MultipleSerialization serialization, URL url) {
            this.serialization = serialization;
            this.url = url;
        }

        @Override
        public Object unpack(byte[] data) throws IOException, ClassNotFoundException {
            TripleWrapper.TripleResponseWrapper wrapper = TripleWrapper.TripleResponseWrapper.parseFrom(
                data);
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
        private final boolean singleArgument;

        private WrapRequestPack(MultipleSerialization multipleSerialization,
            URL url,
            String serialize,
            String[] argumentsType,
            boolean singleArgument) {
            this.url = url;
            this.serialize = convertHessianToWrapper(serialize);
            this.multipleSerialization = multipleSerialization;
            this.argumentsType = argumentsType;
            this.singleArgument = singleArgument;
        }

        @Override
        public byte[] pack(Object obj) throws IOException {
            Object[] arguments;
            if (singleArgument) {
                arguments = new Object[]{obj};
            } else {
                arguments = (Object[]) obj;
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
         * Convert hessian version from Dubbo's SPI version(hessian2) to wrapper API version
         * (hessian4)
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

    private static class PbArrayPacker implements Pack {

        private final boolean singleArgument;

        private PbArrayPacker(boolean singleArgument) {
            this.singleArgument = singleArgument;
        }

        @Override
        public byte[] pack(Object obj) throws IOException {
            if (!singleArgument) {
                obj = ((Object[]) obj)[0];
            }
            return PB_PACK.pack(obj);
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
            TripleWrapper.TripleRequestWrapper wrapper = TripleWrapper.TripleRequestWrapper.parseFrom(
                data);
            Object[] ret = new Object[wrapper.getArgsCount()];
            final String serializeType = convertHessianFromWrapper(wrapper.getSerializeType());
            ((WrapResponsePack) responsePack).serialize = serializeType;
            for (int i = 0; i < wrapper.getArgsList().size(); i++) {
                ByteArrayInputStream bais = new ByteArrayInputStream(
                    wrapper.getArgs(i).toByteArray());
                ret[i] = serialization.deserialize(url, serializeType, wrapper.getArgTypes(i),
                    bais);
            }
            return ret;
        }
    }
}
