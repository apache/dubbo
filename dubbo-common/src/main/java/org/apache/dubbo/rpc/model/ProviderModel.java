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

import org.apache.dubbo.common.BaseServiceMetadata;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ServiceConfigBase;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ProviderModel is about published services
 */
public class ProviderModel {
    private String serviceKey;
    private final Object serviceInstance;
    private final ServiceDescriptor serviceModel;
    private final ServiceConfigBase<?> serviceConfig;
    private final List<RegisterStatedURL> urls;

    public ProviderModel(String serviceKey,
                         Object serviceInstance,
                         ServiceDescriptor serviceModel,
                         ServiceConfigBase<?> serviceConfig) {
        if (null == serviceInstance) {
            throw new IllegalArgumentException("Service[" + serviceKey + "]Target is NULL.");
        }

        this.serviceKey = serviceKey;
        this.serviceInstance = serviceInstance;
        this.serviceModel = serviceModel;
        this.serviceConfig = serviceConfig;
        this.urls = new ArrayList<>(1);
    }

    public String getServiceKey() {
        return serviceKey;
    }


    public Class<?> getServiceInterfaceClass() {
        return serviceModel.getServiceInterfaceClass();
    }

    public Object getServiceInstance() {
        return serviceInstance;
    }

    public Set<MethodDescriptor> getAllMethods() {
        return serviceModel.getAllMethods();
    }

    public ServiceDescriptor getServiceModel() {
        return serviceModel;
    }

    public ServiceConfigBase getServiceConfig() {
        return serviceConfig;
    }

    public List<RegisterStatedURL> getStatedUrl() {
        return urls;
    }

    public void addStatedUrl(RegisterStatedURL url) {
        this.urls.add(url);
    }

    public static class RegisterStatedURL {
        private volatile URL registryUrl;
        private volatile URL providerUrl;
        private volatile boolean registered;

        public RegisterStatedURL(URL providerUrl,
                                 URL registryUrl,
                                 boolean registered) {
            this.providerUrl = providerUrl;
            this.registered = registered;
            this.registryUrl = registryUrl;
        }

        public URL getProviderUrl() {
            return providerUrl;
        }

        public void setProviderUrl(URL providerUrl) {
            this.providerUrl = providerUrl;
        }

        public boolean isRegistered() {
            return registered;
        }

        public void setRegistered(boolean registered) {
            this.registered = registered;
        }

        public URL getRegistryUrl() {
            return registryUrl;
        }

        public void setRegistryUrl(URL registryUrl) {
            this.registryUrl = registryUrl;
        }
    }

    /* *************** Start, metadata compatible **************** */

    private ServiceMetadata serviceMetadata;
    private final Map<String, List<ProviderMethodModel>> methods = new HashMap<String, List<ProviderMethodModel>>();

    public ProviderModel(String serviceKey,
                         Object serviceInstance,
                         ServiceDescriptor serviceModel,
                         ServiceConfigBase<?> serviceConfig,
                         ServiceMetadata serviceMetadata) {
        this(serviceKey, serviceInstance, serviceModel, serviceConfig);

        this.serviceMetadata = serviceMetadata;
        initMethod(serviceModel.getServiceInterfaceClass());
    }


    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
        if (serviceMetadata != null) {
            serviceMetadata.setServiceKey(serviceKey);
            serviceMetadata.setGroup(BaseServiceMetadata.groupFromServiceKey(serviceKey));
        }
    }

    public String getServiceName() {
        return this.serviceMetadata.getServiceKey();
    }

    public List<ProviderMethodModel> getAllMethodModels() {
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

    public List<ProviderMethodModel> getMethodModelList(String methodName) {
        List<ProviderMethodModel> resultList = methods.get(methodName);
        return resultList == null ? Collections.emptyList() : resultList;
    }

    private void initMethod(Class<?> serviceInterfaceClass) {
        Method[] methodsToExport;
        methodsToExport = serviceInterfaceClass.getMethods();

        for (Method method : methodsToExport) {
            method.setAccessible(true);

            List<ProviderMethodModel> methodModels = methods.get(method.getName());
            if (methodModels == null) {
                methodModels = new ArrayList<ProviderMethodModel>();
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
