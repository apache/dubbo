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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ServiceConfigBase;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ProviderModel is about published services
 */
public class ProviderModel extends ServiceModel {
    private final List<RegisterStatedURL> urls;
    private final Map<String, List<ProviderMethodModel>> methods = new HashMap<>();

    public ProviderModel(String serviceKey,
                         Object serviceInstance,
                         ServiceDescriptor serviceModel,
                         ServiceConfigBase<?> serviceConfig) {
        super(serviceInstance, serviceKey, serviceModel, serviceConfig);
        if (null == serviceInstance) {
            throw new IllegalArgumentException("Service[" + serviceKey + "]Target is NULL.");
        }

        this.urls = new ArrayList<>(1);
    }

    public ProviderModel(String serviceKey,
                         Object serviceInstance,
                         ServiceDescriptor serviceModel,
                         ServiceConfigBase<?> serviceConfig,
                         ServiceMetadata serviceMetadata) {
        super(serviceInstance, serviceKey, serviceModel, serviceConfig, serviceMetadata);
        if (null == serviceInstance) {
            throw new IllegalArgumentException("Service[" + serviceKey + "]Target is NULL.");
        }

        initMethod(serviceModel.getServiceInterfaceClass());
        this.urls = new ArrayList<>(1);
    }

    public ProviderModel(String serviceKey,
                         Object serviceInstance,
                         ServiceDescriptor serviceModel,
                         ServiceConfigBase<?> serviceConfig,
                         ModuleModel moduleModel,
                         ServiceMetadata serviceMetadata) {
        super(serviceInstance, serviceKey, serviceModel, serviceConfig, moduleModel, serviceMetadata);
        if (null == serviceInstance) {
            throw new IllegalArgumentException("Service[" + serviceKey + "]Target is NULL.");
        }

        initMethod(serviceModel.getServiceInterfaceClass());
        this.urls = new ArrayList<>(1);
    }

    public Object getServiceInstance() {
        return getProxyObject();
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

            List<ProviderMethodModel> methodModels = methods.computeIfAbsent(method.getName(), k -> new ArrayList<>());
            methodModels.add(new ProviderMethodModel(method));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ProviderModel that = (ProviderModel) o;
        return Objects.equals(urls, that.urls) && Objects.equals(methods, that.methods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), urls, methods);
    }
}
