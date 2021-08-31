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
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.ServiceConfigBase;

import java.util.Set;

public class ServiceModel {
    private String serviceKey;
    private Object proxyObject;
    private final ModuleModel moduleModel;
    private final ServiceDescriptor serviceModel;
    private final AbstractInterfaceConfig config;

    private ServiceMetadata serviceMetadata;

    public ServiceModel(Object proxyObject, String serviceKey, ServiceDescriptor serviceModel, AbstractInterfaceConfig config) {
        this(proxyObject, serviceKey, serviceModel, config, null);
    }

    public ServiceModel(Object proxyObject, String serviceKey, ServiceDescriptor serviceModel, AbstractInterfaceConfig config, ServiceMetadata serviceMetadata) {
        this.proxyObject = proxyObject;
        this.serviceKey = serviceKey;
        this.serviceModel = serviceModel;
        this.moduleModel = ScopeModelUtil.getModuleModel(config != null ? config.getScopeModel() : null);
        this.config = config;
        this.serviceMetadata = serviceMetadata;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void setProxyObject(Object proxyObject) {
        this.proxyObject = proxyObject;
    }

    public Object getProxyObject() {
        return proxyObject;
    }

    public ServiceDescriptor getServiceModel() {
        return serviceModel;
    }

    public ClassLoader getClassLoader() {
        Class<?> serviceType = serviceMetadata.getServiceType();
        return serviceType != null ? serviceType.getClassLoader() : ClassUtils.getClassLoader();
    }

    /**
     * Return all method models for the current service
     *
     * @return method model list
     */
    public Set<MethodDescriptor> getAllMethods() {
        return serviceModel.getAllMethods();
    }

    public Class<?> getServiceInterfaceClass() {
        return serviceModel.getServiceInterfaceClass();
    }

    public AbstractInterfaceConfig getConfig() {
        return config;
    }

    public ReferenceConfigBase<?> getReferenceConfig() {
        if (config instanceof ReferenceConfigBase) {
            return (ReferenceConfigBase<?>) config;
        } else {
            throw new IllegalArgumentException("Current ServiceModel is not a ConsumerModel");
        }
    }

    public ServiceConfigBase<?> getServiceConfig() {
        if (config instanceof ServiceConfigBase) {
            return (ServiceConfigBase<?>) config;
        } else {
            throw new IllegalArgumentException("Current ServiceModel is not a ProviderModel");
        }
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

    /**
     * @return serviceMetadata
     */
    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }

    public ModuleModel getModuleModel() {
        return moduleModel;
    }
}
