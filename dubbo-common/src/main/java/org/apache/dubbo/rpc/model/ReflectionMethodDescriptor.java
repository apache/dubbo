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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.ReflectUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static org.apache.dubbo.common.constants.CommonConstants.$INVOKE;
import static org.apache.dubbo.common.constants.CommonConstants.$INVOKE_ASYNC;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_REFLECTIVE_OPERATION_FAILED;

public class ReflectionMethodDescriptor implements MethodDescriptor {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ReflectionMethodDescriptor.class);

    private final ConcurrentMap<String, Object> attributeMap = new ConcurrentHashMap<>();
    public final String methodName;
    private final String[] compatibleParamSignatures;
    private final Class<?>[] parameterClasses;
    private final Class<?> returnClass;
    private final Type[] returnTypes;
    private final String paramDesc;
    private final Method method;
    private final boolean generic;
    private final RpcType rpcType;


    public ReflectionMethodDescriptor(Method method) {
        this.method = method;
        this.methodName = method.getName();
        this.parameterClasses = method.getParameterTypes();
        this.returnClass = method.getReturnType();
        Type[] returnTypesResult;
        try {
            returnTypesResult = ReflectUtils.getReturnTypes(method);
        } catch (Throwable throwable) {
            logger.error(COMMON_REFLECTIVE_OPERATION_FAILED, "", "",
                "fail to get return types. Method name: " + methodName + " Declaring class:" + method.getDeclaringClass()
                    .getName(), throwable);
            returnTypesResult = new Type[]{returnClass, returnClass};
        }
        this.returnTypes = returnTypesResult;
        this.paramDesc = ReflectUtils.getDesc(parameterClasses);
        this.compatibleParamSignatures = Stream.of(parameterClasses).map(Class::getName).toArray(String[]::new);
        this.generic = (methodName.equals($INVOKE) || methodName.equals($INVOKE_ASYNC)) && parameterClasses.length == 3;
        this.rpcType = determineRpcType();
    }

    private RpcType determineRpcType() {
        if (generic) {
            return RpcType.UNARY;
        }
        if (parameterClasses.length > 2) {
            return RpcType.UNARY;
        }
        if (parameterClasses.length == 1 && isStreamType(parameterClasses[0]) && isStreamType(returnClass)) {
            return RpcType.BI_STREAM;
        }
        if (parameterClasses.length == 2 && !isStreamType(parameterClasses[0]) && isStreamType(
            parameterClasses[1]) && returnClass.getName().equals(void.class.getName())) {
            return RpcType.SERVER_STREAM;
        }
        if (Arrays.stream(parameterClasses).anyMatch(this::isStreamType) || isStreamType(returnClass)) {
            throw new IllegalStateException(
                "Bad stream method signature. method(" + methodName + ":" + paramDesc + ")");
        }
        // Can not determine client stream because it has same signature with bi_stream
        return RpcType.UNARY;
    }

    private boolean isStreamType(Class<?> classType) {
        return StreamObserver.class.isAssignableFrom(classType);
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public String[] getCompatibleParamSignatures() {
        return compatibleParamSignatures;
    }

    @Override
    public Class<?>[] getParameterClasses() {
        return parameterClasses;
    }

    @Override
    public String getParamDesc() {
        return paramDesc;
    }

    @Override
    public Class<?> getReturnClass() {
        return returnClass;
    }

    @Override
    public Type[] getReturnTypes() {
        return returnTypes;
    }

    @Override
    public RpcType getRpcType() {
        return rpcType;
    }

    @Override
    public boolean isGeneric() {
        return generic;
    }

    public void addAttribute(String key, Object value) {
        this.attributeMap.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributeMap.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReflectionMethodDescriptor that = (ReflectionMethodDescriptor) o;
        return generic == that.generic && Objects.equals(method, that.method) && Objects.equals(paramDesc,
            that.paramDesc) && Arrays.equals(compatibleParamSignatures,
            that.compatibleParamSignatures) && Arrays.equals(parameterClasses, that.parameterClasses) && Objects.equals(
            returnClass, that.returnClass) && Arrays.equals(returnTypes, that.returnTypes) && Objects.equals(methodName,
            that.methodName) && Objects.equals(attributeMap, that.attributeMap);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(method, paramDesc, returnClass, methodName, generic, attributeMap);
        result = 31 * result + Arrays.hashCode(compatibleParamSignatures);
        result = 31 * result + Arrays.hashCode(parameterClasses);
        result = 31 * result + Arrays.hashCode(returnTypes);
        return result;
    }

}
