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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ReflectUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class StubMethodDescriptor implements MethodDescriptor, PackableMethod {
    private static final Logger logger = LoggerFactory.getLogger(StubMethodDescriptor.class);
    private final ServiceDescriptor serviceDescriptor;
    private final ConcurrentMap<String, Object> attributeMap = new ConcurrentHashMap<>();
    private final String methodName;
    private final String[] compatibleParamSignatures;
    private final Class<?>[] parameterClasses;
    private final Class<?> returnClass;
    private final Type[] returnTypes;
    private final String paramDesc;
    private final RpcType rpcType;
    private final Pack requestPack;
    private final Pack responsePack;
    private final UnPack requestUnpack;
    private final UnPack responseUnpack;

    public StubMethodDescriptor(String methodName,
        Class<?> requestClass,
        Class<?> responseClass,
        StubServiceDescriptor serviceDescriptor,
        RpcType rpcType,
        Pack requestPack,
        Pack responsePack,
        UnPack requestUnpack,
        UnPack responseUnpack) {
        this.methodName = methodName;
        this.serviceDescriptor = serviceDescriptor;
        this.rpcType = rpcType;
        this.requestPack = requestPack;
        this.responsePack = responsePack;
        this.responseUnpack = responseUnpack;
        this.requestUnpack = requestUnpack;
        this.parameterClasses = new Class<?>[]{requestClass};
        this.returnClass = responseClass;
        this.paramDesc = ReflectUtils.getDesc(parameterClasses);
        this.compatibleParamSignatures = Stream.of(parameterClasses).map(Class::getName).toArray(String[]::new);
        this.returnTypes = new Type[]{requestClass, requestClass};
        serviceDescriptor.addMethod(this);
    }


    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public String getParamDesc() {
        return paramDesc;
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
        return false;
    }

    @Override
    public Method getMethod() {
        return null;
    }

    @Override
    public void addAttribute(String key, Object value) {
        this.attributeMap.put(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        return this.attributeMap.get(key);
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
}
