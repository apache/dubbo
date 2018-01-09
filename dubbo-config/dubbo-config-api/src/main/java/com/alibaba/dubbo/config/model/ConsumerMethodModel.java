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
package com.alibaba.dubbo.config.model;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.config.ReferenceConfig;

import java.lang.reflect.Method;

public class ConsumerMethodModel {
    private final Method method;
    private final ReferenceConfig metadata;
//    private final boolean isCallBack;
//    private final boolean isFuture;
    private final String[] parameterTypes;
    private final Class<?>[] parameterClasses;
    private final Class<?> returnClass;
    private final String methodName;
    private final boolean generic;

    public ConsumerMethodModel(Method method, ReferenceConfig metadata) {
        this.method = method;
        this.parameterClasses = method.getParameterTypes();
        this.returnClass = method.getReturnType();
        this.parameterTypes = this.createParamSignature(parameterClasses);
        this.methodName = method.getName();
        this.metadata = metadata;
        this.generic = methodName.equals(Constants.$INVOKE) && parameterTypes != null && parameterTypes.length == 3;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getReturnClass() {
        return returnClass;
    }



    public String getMethodName() {
        return methodName;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public ReferenceConfig getMetadata() {
        return metadata;
    }

    private String[] createParamSignature(Class<?>[] args) {
        if (args == null || args.length == 0) {
            return new String[] {};
        }
        String[] paramSig = new String[args.length];
        for (int x = 0; x < args.length; x++) {
            paramSig[x] = args[x].getName();
        }
        return paramSig;
    }


    public boolean isGeneric() {
        return generic;
    }

    public Class<?>[] getParameterClasses() {
        return parameterClasses;
    }
}
