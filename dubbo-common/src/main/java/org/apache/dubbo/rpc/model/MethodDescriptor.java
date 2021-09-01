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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.ReflectUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static org.apache.dubbo.common.constants.CommonConstants.$ECHO;
import static org.apache.dubbo.common.constants.CommonConstants.$INVOKE;
import static org.apache.dubbo.common.constants.CommonConstants.$INVOKE_ASYNC;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOBUF_MESSAGE_CLASS_NAME;

/**
 *
 */
public class MethodDescriptor {

    private static final String GRPC_ASYNC_RETURN_CLASS = "com.google.common.util.concurrent.ListenableFuture";
    private static final String TRI_ASYNC_RETURN_CLASS = "java.util.concurrent.CompletableFuture";
    private static final String GRPC_STREAM_CLASS = "io.grpc.stub.StreamObserver";

    private static final Logger logger = LoggerFactory.getLogger(MethodDescriptor.class);
    private final Method method;
    //    private final boolean isCallBack;
    //    private final boolean isFuture;
    private final String paramDesc;
    // duplicate filed as paramDesc, but with different format.
    private final String[] compatibleParamSignatures;
    private final Class<?>[] parameterClasses;
    private final Class<?> returnClass;
    private final Type[] returnTypes;
    private final String methodName;
    private final boolean generic;
    private final boolean wrap;
    private final RpcType rpcType;
    private final ConcurrentMap<String, Object> attributeMap = new ConcurrentHashMap<>();

    public MethodDescriptor(Method method) {
        this.method = method;
        this.methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 1 && isStreamType(parameterTypes[0])) {
            this.parameterClasses = new Class<?>[]{
                (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0]};
            this.returnClass = (Class<?>) ((ParameterizedType) method.getGenericParameterTypes()[0])
                .getActualTypeArguments()[0];
            this.rpcType = RpcType.BIDIRECTIONAL_STREAM;
        } else if (parameterTypes.length == 2 && method.getReturnType().equals(Void.TYPE)
            && !isStreamType(parameterTypes[0]) && isStreamType(parameterTypes[1])) {
            this.parameterClasses = method.getParameterTypes();
            this.returnClass =
                (Class<?>) ((ParameterizedType) method.getGenericParameterTypes()[1]).getActualTypeArguments()[0];
            this.rpcType = RpcType.SERVER_STREAM;
        } else {
            this.parameterClasses = method.getParameterTypes();
            this.returnClass = method.getReturnType();
            this.rpcType = RpcType.UNARY;
        }
        this.wrap = needWrap();
        Type[] returnTypesResult;
        try {
            returnTypesResult = ReflectUtils.getReturnTypes(method);
        } catch (Throwable throwable) {
            logger.error("fail to get return types", throwable);
            returnTypesResult = new Type[]{returnClass, returnClass};
        }

        this.returnTypes = returnTypesResult;
        this.paramDesc = ReflectUtils.getDesc(parameterClasses);
        this.compatibleParamSignatures = Stream.of(parameterClasses)
            .map(Class::getName)
            .toArray(String[]::new);
        this.generic = (methodName.equals($INVOKE) || methodName.equals($INVOKE_ASYNC)) && parameterClasses.length == 3;
    }

    private static boolean isStreamType(Class<?> clz) {
        return StreamObserver.class.isAssignableFrom(clz) || GRPC_STREAM_CLASS.equalsIgnoreCase(clz.getName());
    }

    public boolean isStream() {
        return rpcType.equals(RpcType.SERVER_STREAM) || rpcType.equals(RpcType.BIDIRECTIONAL_STREAM) || rpcType.equals(RpcType.CLIENT_STREAM);
    }

    public boolean isUnary() {
        return rpcType.equals(RpcType.UNARY);
    }

    public boolean isNeedWrap() {
        return wrap;
    }

    public RpcType getRpcType() {
        return rpcType;
    }

    private boolean needWrap() {
        if (CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(methodName)) {
            return true;
        }
        if ($ECHO.equals(methodName)) {
            return true;
        }
        boolean returnClassProtobuf = isProtobufClass(returnClass);
        if (parameterClasses.length == 0) {
            return !returnClassProtobuf;
        }
        int protobufParameterCount = 0;
        int javaParameterCount = 0;
        int streamParameterCount = 0;
        boolean secondParameterStream = false;
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
        if (streamParameterCount > 1) {
            throw new IllegalStateException("method params error method=" + methodName);
        }
        if (protobufParameterCount >= 2) {
            throw new IllegalStateException("method params error method=" + methodName);
        }
        if (streamParameterCount == 1) {
            if (javaParameterCount + protobufParameterCount > 1) {
                throw new IllegalStateException("method params error method=" + methodName);
            }
            if (!secondParameterStream) {
                throw new IllegalStateException("method params error method=" + methodName);
            }
        }
        if (isStream()) {
            if (RpcType.SERVER_STREAM == rpcType) {
                if (!secondParameterStream) {
                    throw new IllegalStateException("method params error method=" + methodName);
                }
            }
            if (returnClassProtobuf) {
                if (javaParameterCount > 0) {
                    throw new IllegalStateException("method params error method=" + methodName);
                }
            } else {
                if (protobufParameterCount > 0) {
                    throw new IllegalStateException("method params error method=" + methodName);
                }
            }
        } else {
            if (streamParameterCount > 0) {
                throw new IllegalStateException("method params error method=" + methodName);
            }
            if (protobufParameterCount > 0 && returnClassProtobuf) {
                return false;
            }
            if (protobufParameterCount <= 0 && !returnClassProtobuf) {
                return true;
            }
            if (GRPC_ASYNC_RETURN_CLASS.equalsIgnoreCase(returnClass.getName()) && protobufParameterCount == 1) {
                return false;
            }
            if (TRI_ASYNC_RETURN_CLASS.equalsIgnoreCase(returnClass.getName())) {
                Class<?> actualReturnClass =
                    (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
                boolean actualReturnClassProtobuf = isProtobufClass(actualReturnClass);
                if (actualReturnClassProtobuf && protobufParameterCount == 1) {
                    return false;
                }
                if (!actualReturnClassProtobuf && protobufParameterCount == 0) {
                    return true;
                }
            }
            throw new IllegalStateException("method params error method=" + methodName);
        }
        return javaParameterCount > 0;
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

    public boolean matchParams(String params) {
        return paramDesc.equalsIgnoreCase(params);
    }

    public Method getMethod() {
        return method;
    }

    public String getParamDesc() {
        return paramDesc;
    }

    public String[] getCompatibleParamSignatures() {
        return compatibleParamSignatures;
    }

    public Class<?>[] getParameterClasses() {
        return parameterClasses;
    }

    public Class<?> getReturnClass() {
        return returnClass;
    }

    public Type[] getReturnTypes() {
        return returnTypes;
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean isGeneric() {
        return generic;
    }

    public void addAttribute(String key, Object value) {
        this.attributeMap.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributeMap.get(key);
    }

    public enum RpcType {
        UNARY, SERVER_STREAM, CLIENT_STREAM, BIDIRECTIONAL_STREAM
    }

}
