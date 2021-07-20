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
    private final RpcType rpcType;

    private final ConcurrentMap<String, Object> attributeMap = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(MethodDescriptor.class);

    public MethodDescriptor(Method method) {
        this.method = method;
        this.methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 1 && isStreamType(parameterTypes[0])) {
            this.parameterClasses = new Class<?>[]{
                    (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0]};
            this.returnClass = (Class<?>) ((ParameterizedType) method.getGenericParameterTypes()[0])
                    .getActualTypeArguments()[0];
            if (needWrap()) {
                rpcType = RpcType.STREAM_WRAP;
            } else {
                rpcType = RpcType.STREAM_UNWRAP;
            }
        } else {
            this.parameterClasses = method.getParameterTypes();
            this.returnClass = method.getReturnType();
            if (needWrap()) {
                rpcType = RpcType.UNARY_WRAP;
            } else {
                rpcType = RpcType.UNARY_UNWRAP;
            }
        }
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
        return StreamObserver.class.isAssignableFrom(clz);
    }

    public boolean isStream() {
        return rpcType.equals(RpcType.STREAM_WRAP) || rpcType.equals(RpcType.STREAM_UNWRAP);
    }

    public boolean isUnary() {
        return rpcType.equals(RpcType.UNARY_WRAP) || rpcType.equals(RpcType.UNARY_UNWRAP);
    }

    public boolean isNeedWrap() {
        return rpcType.equals(RpcType.UNARY_WRAP) || rpcType.equals(RpcType.STREAM_WRAP);
    }

    private boolean needWrap() {
        if (CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(methodName)) {
            return true;
        } else if ($ECHO.equals(methodName)) {
            return true;
        } else {
            if (parameterClasses.length != 1 || parameterClasses[0] == null) {
                return true;
            }

            Class<?> clazz = parameterClasses[0];
            while (clazz != Object.class && clazz != null) {
                Class<?>[] interfaces = clazz.getInterfaces();
                if (interfaces.length > 0) {
                    for (Class<?> clazzInterface : interfaces) {
                        if (PROTOBUF_MESSAGE_CLASS_NAME.equalsIgnoreCase(clazzInterface.getName())) {
                            return false;
                        }
                    }
                }

                clazz = clazz.getSuperclass();
            }

            return true;
        }
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
        UNARY_WRAP,
        UNARY_UNWRAP,
        STREAM_WRAP,
        STREAM_UNWRAP;
    }

}
