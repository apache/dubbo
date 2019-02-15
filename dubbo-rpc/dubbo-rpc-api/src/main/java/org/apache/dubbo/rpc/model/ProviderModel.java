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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProviderModel which is about published services
 */
public class ProviderModel {
    private final Object serviceInstance;
    private final ServiceMetadata serviceMetadata;
    private final Map<String, List<ProviderMethodModel>> methods = new HashMap<String, List<ProviderMethodModel>>();

    public ProviderModel(String serviceName, String group, String version, Object serviceInstance, Class<?> serviceInterfaceClass) {
        if (null == serviceInstance) {
            throw new IllegalArgumentException("Service[" + serviceName + "]Target is NULL.");
        }

        this.serviceInstance = serviceInstance;
        this.serviceMetadata = new ServiceMetadata(serviceName, group, version, serviceInterfaceClass);
        initMethod(serviceInterfaceClass);
    }


    public String getServiceName() {
        return this.serviceMetadata.getServiceKey();
    }

    public Class<?> getServiceInterfaceClass() {
        return this.serviceMetadata.getServiceType();
    }

    public Object getServiceInstance() {
        return serviceInstance;
    }

    public List<ProviderMethodModel> getAllMethods() {
        List<ProviderMethodModel> result = new ArrayList<ProviderMethodModel>();
        for (List<ProviderMethodModel> models : methods.values()) {
            result.addAll(models);
        }
        return result;
    }

    public ProviderMethodModel getMethodModel(String methodName, String[] argTypes) {
        List<ProviderMethodModel> methodModels = methods.get(methodName);
        if (methodModels != null) {
            for (ProviderMethodModel methodModel : methodModels) {
                if (Arrays.equals(argTypes, methodModel.getMethodArgTypes())) {
                    return methodModel;
                }
            }
        }
        return null;
    }

    private void initMethod(Class<?> serviceInterfaceClass) {
        Method[] methodsToExport = null;
        methodsToExport = serviceInterfaceClass.getMethods();

        for (Method method : methodsToExport) {
            method.setAccessible(true);

            List<ProviderMethodModel> methodModels = methods.get(method.getName());
            if (methodModels == null) {
                methodModels = new ArrayList<ProviderMethodModel>(1);
                methods.put(method.getName(), methodModels);
            }
            methodModels.add(new ProviderMethodModel(method));
        }
    }

    /**
     * @return serviceMetadata
     */
    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }
}
