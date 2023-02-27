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
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.ServiceConfigBase;

import java.util.Objects;
import java.util.Set;

public class ServiceModel {
    private String serviceKey;
    private Object proxyObject;
    private Runnable destroyRunner;
    private ClassLoader classLoader;

    private final ClassLoader interfaceClassLoader;

    private final ModuleModel moduleModel;
    private final ServiceDescriptor serviceModel;

    private AbstractInterfaceConfig config;

    private final ServiceMetadata serviceMetadata;

    public ServiceModel(Object proxyObject, String serviceKey, ServiceDescriptor serviceModel, ModuleModel moduleModel, ClassLoader interfaceClassLoader) {
        this(proxyObject, serviceKey, serviceModel, moduleModel, null, interfaceClassLoader);
    }

    public ServiceModel(Object proxyObject, String serviceKey, ServiceDescriptor serviceModel, ModuleModel moduleModel, ServiceMetadata serviceMetadata,
                        ClassLoader interfaceClassLoader) {
        this.proxyObject = proxyObject;
        this.serviceKey = serviceKey;
        this.serviceModel = serviceModel;
        this.moduleModel = ScopeModelUtil.getModuleModel(moduleModel);
        this.serviceMetadata = serviceMetadata;
        this.interfaceClassLoader = interfaceClassLoader;
        if (serviceMetadata != null) {
            serviceMetadata.setServiceModel(this);
        }
        if (interfaceClassLoader != null) {
            this.classLoader = interfaceClassLoader;
        }
        if (this.classLoader == null) {
            this.classLoader = Thread.currentThread().getContextClassLoader();
        }
    }

    @Deprecated
    public AbstractInterfaceConfig getConfig() {
        return config;
    }

    @Deprecated
    public void setConfig(AbstractInterfaceConfig config) {
        this.config = config;
    }

    /**
     * ServiceModel should be decoupled from AbstractInterfaceConfig and removed in a future version
     * @return
     */
    @Deprecated
    public ReferenceConfigBase<?> getReferenceConfig() {
        if (config == null) {
            return null;
        }
        if (config instanceof ReferenceConfigBase) {
            return (ReferenceConfigBase<?>) config;
        } else {
            throw new IllegalArgumentException("Current ServiceModel is not a ConsumerModel");
        }
    }

    /**
     * ServiceModel should be decoupled from AbstractInterfaceConfig and removed in a future version
     * @return
     */
    @Deprecated
    public ServiceConfigBase<?> getServiceConfig() {
        if (config == null) {
            return null;
        }
        if (config instanceof ServiceConfigBase) {
            return (ServiceConfigBase<?>) config;
        } else {
            throw new IllegalArgumentException("Current ServiceModel is not a ProviderModel");
        }
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

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
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

    public Runnable getDestroyRunner() {
        return destroyRunner;
    }

    public void setDestroyRunner(Runnable destroyRunner) {
        this.destroyRunner = destroyRunner;
    }

    public ClassLoader getInterfaceClassLoader() {
        return interfaceClassLoader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceModel that = (ServiceModel) o;
        return Objects.equals(serviceKey, that.serviceKey) && Objects.equals(proxyObject, that.proxyObject) && Objects.equals(destroyRunner, that.destroyRunner) && Objects.equals(classLoader, that.classLoader) && Objects.equals(interfaceClassLoader, that.interfaceClassLoader) && Objects.equals(moduleModel, that.moduleModel) && Objects.equals(serviceModel, that.serviceModel) && Objects.equals(serviceMetadata, that.serviceMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceKey, proxyObject, destroyRunner, classLoader, interfaceClassLoader, moduleModel, serviceModel, serviceMetadata);
    }
}
