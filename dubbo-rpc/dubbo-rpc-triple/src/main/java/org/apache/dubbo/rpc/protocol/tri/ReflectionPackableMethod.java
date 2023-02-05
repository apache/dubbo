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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.remoting.utils.UrlUtils;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.PackableMethod;

import com.google.protobuf.Message;

import static org.apache.dubbo.common.constants.CommonConstants.$ECHO;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOBUF_MESSAGE_CLASS_NAME;
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
    private final Pack originalPack;
    private final UnPack originalUnpack;

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
            originalPack = new PbArrayPacker(singleArgument);
            responsePack = PB_PACK;
            requestUnpack = new PbUnpack<>(actualRequestTypes[0]);
            responseUnpack = new PbUnpack<>(actualResponseType);
            originalUnpack = new PbUnpack<>(actualResponseType);

        } else {
            final MultipleSerialization serialization = url.getOrDefaultFrameworkModel()
                .getExtensionLoader(MultipleSerialization.class)
                .getExtension(url.getParameter(Constants.MULTI_SERIALIZATION_KEY,
                    CommonConstants.DEFAULT_KEY));

            this.requestPack = new WrapRequestPack(serialization, url, serializeName, singleArgument);
            this.responsePack = new WrapResponsePack(serialization, url, actualResponseType);
            this.originalPack = new OriginalPack(serialization, url, serializeName, singleArgument);
            this.requestUnpack = new WrapRequestUnpack(serialization, url, actualRequestTypes);
            this.responseUnpack = new WrapResponseUnpack(serialization, url, actualResponseType);
            this.originalUnpack = new OriginalUnpack(serialization, url, actualResponseType);
        }
    }

    public static ReflectionPackableMethod init(MethodDescriptor methodDescriptor, URL url) {
        final String serializeName = UrlUtils.serializationOrDefault(url);
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
    public Pack getRequestPack(String contentType) {
        if(isNotOriginalSerializeType(contentType) ){
            return requestPack;
        }
        ((OriginalUnpack)originalPack).serialize = getSerializeType(contentType);
        return originalPack;
    }

    @Override
    public Pack getResponsePack(String contentType) {
        if(isNotOriginalSerializeType(contentType) ){
            return responsePack;
        }
        ((OriginalUnpack)originalPack).serialize = getSerializeType(contentType);
        return originalPack;
    }

    @Override
    public UnPack getResponseUnpack(String contentType) {
        if(isNotOriginalSerializeType(contentType) ){
            return responseUnpack;
        }
        ((OriginalUnpack)originalUnpack).serialize = getSerializeType(contentType);
        return originalUnpack;
    }

    @Override
    public UnPack getRequestUnpack(String contentType) {
        if(isNotOriginalSerializeType(contentType) ){
            return requestUnpack;
        }
        ((OriginalUnpack)originalUnpack).serialize = getSerializeType(contentType);
        return originalUnpack;
    }

    private boolean isNotOriginalSerializeType(String contentType) {

        return TripleConstant.CONTENT_PROTO.contains(contentType);
    }

    private String getSerializeType(String contentType) {
        // contentType：application/grpc、application/grpc+proto ...
        String[] contentTypes = contentType.split("\\+");
        return contentTypes[1];
    }

    private static class WrapResponsePack implements Pack {

        private final MultipleSerialization multipleSerialization;
        private final URL url;

        private final Class<?> actualResponseType;
        String serialize;

        private WrapResponsePack(MultipleSerialization multipleSerialization, URL url,
                                 Class<?> actualResponseType) {
            this.multipleSerialization = multipleSerialization;
            this.url = url;
            this.actualResponseType = actualResponseType;
        }

        @Override
        public byte[] pack(Object obj) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Class<?> clz;
            if (obj != null) {
                clz = obj.getClass();
            } else {
                clz = actualResponseType;
            }
            multipleSerialization.serialize(url, serialize, clz, obj, bos);
            return TripleCustomerProtocolWapper.TripleResponseWrapper.Builder.newBuilder()
                .setSerializeType(serialize)
                .setType(clz.getName())
                .setData(bos.toByteArray())
                .build()
                .toByteArray();
        }
    }

    private static class WrapResponseUnpack implements UnPack {

        private final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();

        private final MultipleSerialization serialization;
        private final URL url;

        private final Class<?> actualResponseType;

        private WrapResponseUnpack(MultipleSerialization serialization, URL url, Class<?> actualResponseType) {
            this.serialization = serialization;
            this.url = url;
            this.actualResponseType = actualResponseType;
        }

        @Override
        public Object unpack(byte[] data) throws IOException, ClassNotFoundException {
            TripleCustomerProtocolWapper.TripleResponseWrapper wrapper = TripleCustomerProtocolWapper.TripleResponseWrapper
                .parseFrom(data);
            final String serializeType = convertHessianFromWrapper(wrapper.getSerializeType());
            ByteArrayInputStream bais = new ByteArrayInputStream(wrapper.getData());
            Class<?> clz = getClassFromCache(wrapper.getType(), classCache, actualResponseType);
            return serialization.deserialize(url, serializeType, clz, bais);
        }
    }

    private static class WrapRequestPack implements Pack {

        private final String serialize;
        private final MultipleSerialization multipleSerialization;
        private final URL url;
        private final boolean singleArgument;

        private WrapRequestPack(MultipleSerialization multipleSerialization,
                                URL url,
                                String serialize,
                                boolean singleArgument) {
            this.url = url;
            this.serialize = convertHessianToWrapper(serialize);
            this.multipleSerialization = multipleSerialization;
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
            final TripleCustomerProtocolWapper.TripleRequestWrapper.Builder builder = TripleCustomerProtocolWapper.TripleRequestWrapper.Builder.newBuilder();
            builder.setSerializeType(serialize);
            for (Object argument : arguments) {
                builder.addArgTypes(argument.getClass().getName());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                multipleSerialization.serialize(url, serialize, argument.getClass(), argument, bos);
                builder.addArgs(bos.toByteArray());
            }
            return builder.build().toByteArray();
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

        private final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();

        private final MultipleSerialization serialization;
        private final URL url;

        private final Class<?>[] actualRequestTypes;

        private WrapRequestUnpack(MultipleSerialization serialization, URL url, Class<?>[] actualRequestTypes) {
            this.serialization = serialization;
            this.url = url;
            this.actualRequestTypes = actualRequestTypes;
        }

        @Override
        public Object unpack(byte[] data) throws IOException, ClassNotFoundException {
            TripleCustomerProtocolWapper.TripleRequestWrapper wrapper = TripleCustomerProtocolWapper.TripleRequestWrapper.parseFrom(
                data);
            Object[] ret = new Object[wrapper.getArgs().size()];
            ((WrapResponsePack) responsePack).serialize = wrapper.getSerializeType();
            for (int i = 0; i < wrapper.getArgs().size(); i++) {
                ByteArrayInputStream bais = new ByteArrayInputStream(
                    wrapper.getArgs().get(i));
                String className = wrapper.getArgTypes().get(i);
                Class<?> clz = getClassFromCache(className, classCache, actualRequestTypes[i]);
                ret[i] = serialization.deserialize(url, wrapper.getSerializeType(), clz, bais);
            }
            return ret;
        }


    }

    private static class OriginalPack implements Pack{

        private final MultipleSerialization multipleSerialization;
        private final URL url;
        private final boolean singleArgument;
        String serialize;

        private OriginalPack(MultipleSerialization multipleSerialization,
                             URL url,
                             String serialize,
                             boolean singleArgument) {
            this.url = url;
            this.multipleSerialization = multipleSerialization;
            this.singleArgument = singleArgument;
        }

        @Override
        public byte[] pack(Object obj) throws IOException {
            Object[] arguments = singleArgument ? new Object[]{obj} : (Object[]) obj;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            multipleSerialization.serialize(url, serialize, arguments.getClass(), arguments, bos);

            return  bos.toByteArray();
        }

    }

    private static class OriginalUnpack implements UnPack{
        private final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();


        private final MultipleSerialization serialization;
        private final URL url;
        private final Class<?> actualResponseType;

        String serialize;

        private OriginalUnpack(MultipleSerialization serialization,
                               URL url,
                               Class<?> actualResponseType) {
            this.serialization = serialization;
            this.url = url;
            this.actualResponseType=actualResponseType;
        }

        @Override
        public Object unpack(byte[] data) throws IOException, ClassNotFoundException {

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            Class<?> clz = getClassFromCache(Object[].class.getName(), classCache, actualResponseType);
            return serialization.deserialize(url, serialize,clz, bais);
        }
    }

    private static Class<?> getClassFromCache(String className, Map<String, Class<?>> classCache, Class<?> expectedClass) {
        if (expectedClass.getName().equals(className)) {
            return expectedClass;
        }

        Class<?> clz = classCache.get(className);
        if (clz == null) {
            try {
                clz = ClassUtils.forName(className);
            } catch (Throwable e) {
                // To catch IllegalStateException, LinkageError, ClassNotFoundException
                clz = expectedClass;
            }
            classCache.put(className, clz);
        }
        return clz;
    }

    /**
     * Convert hessian version from Dubbo's SPI version(hessian2) to wrapper API version
     * (hessian4)
     *
     * @param serializeType literal type
     * @return hessian4 if the param is hessian2, otherwise return the param
     */
    private static String convertHessianToWrapper(String serializeType) {
        if (TripleConstant.HESSIAN2.equals(serializeType)) {
            return TripleConstant.HESSIAN4;
        }
        return serializeType;
    }

}
