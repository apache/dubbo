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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ServiceModel {
    private final String serviceName;
    private final Class<?> serviceInterfaceClass;
    private final Map<String, Set<MethodModel>> methods = new HashMap<>();

    public ServiceModel (Class<?> interfaceClass) {
        this.serviceInterfaceClass = interfaceClass;
        this.serviceName = interfaceClass.getName();
        initMethods();
    }

    private void initMethods() {
        Method[] methodsToExport = null;
        methodsToExport = this.serviceInterfaceClass.getMethods();

        for (Method method : methodsToExport) {
            method.setAccessible(true);

            Set<MethodModel> methodModels = methods.computeIfAbsent(method.getName(), (k) ->new HashSet<>(1));
            methodModels.add(new MethodModel(method));
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public Class<?> getServiceInterfaceClass() {
        return serviceInterfaceClass;
    }

    public Set<MethodModel> getAllMethods () {
        Set<MethodModel> methodModels = new HashSet<>();
        methods.forEach((k, v) -> methodModels.addAll(v));
        return methodModels;
    }

    public Optional<MethodModel> getMethod (String methodName, String params) {
        return methods.get(methodName).stream()
                .filter((methodModel) -> methodModel.matchParams(params))
                .findFirst();
    }

    public Set<MethodModel> getMethods (String methodName) {
        return methods.get(methodName);
    }

}
