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
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.ReferenceConfigBase;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * This model is bound to your reference's configuration, for example, group, version or method level configuration.
 */
public class ConsumerModel {
    private String serviceKey;
    private final ServiceDescriptor serviceModel;
    private final ReferenceConfigBase<?> referenceConfig;
    private final Set<String> apps = new TreeSet<>();

    private Object proxyObject;

    private final Map<String, AsyncMethodInfo> methodConfigs;

    /**
     * This constructor create an instance of ConsumerModel and passed objects should not be null.
     * If service name, service instance, proxy object,methods should not be null. If these are null
     * then this constructor will throw {@link IllegalArgumentException}
     *
     * @param serviceKey  Name of the service.
     * @param proxyObject Proxy object.
     */
    public ConsumerModel(String serviceKey,
                         Object proxyObject,
                         ServiceDescriptor serviceModel,
                         ReferenceConfigBase<?> referenceConfig,
                         Map<String, AsyncMethodInfo> methodConfigs) {

        Assert.notEmptyString(serviceKey, "Service name can't be null or blank");

        this.serviceKey = serviceKey;
        this.proxyObject = proxyObject;
        this.serviceModel = serviceModel;
        this.referenceConfig = referenceConfig;
        this.methodConfigs = methodConfigs == null ? new HashMap<>() : methodConfigs;
    }

    /**
     * Return the proxy object used by called while creating instance of ConsumerModel
     *
     * @return
     */
    public Object getProxyObject() {
        return proxyObject;
    }

    public void setProxyObject(Object proxyObject) {
        this.proxyObject = proxyObject;
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

    public String getServiceKey() {
        return serviceKey;
    }

    public AsyncMethodInfo getMethodConfig(String methodName) {
        return methodConfigs.get(methodName);
    }

    public ServiceDescriptor getServiceModel() {
        return serviceModel;
    }

    public ReferenceConfigBase getReferenceConfig() {
        return referenceConfig;
    }

    public Set<String> getApps() {
        return apps;
    }

    public AsyncMethodInfo getAsyncInfo(String methodName) {
        return methodConfigs.get(methodName);
    }

    /* *************** Start, metadata compatible **************** */

    private ServiceMetadata serviceMetadata;
    private Map<Method, ConsumerMethodModel> methodModels = new HashMap<>();

    public ConsumerModel(String serviceKey,
                         Object proxyObject,
                         ServiceDescriptor serviceModel,
                         ReferenceConfigBase<?> referenceConfig,
                         ServiceMetadata metadata,
                         Map<String, AsyncMethodInfo> methodConfigs) {

        this(serviceKey, proxyObject, serviceModel, referenceConfig, methodConfigs);
        this.serviceMetadata = metadata;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
        if (serviceMetadata != null) {
            serviceMetadata.setServiceKey(serviceKey);
            serviceMetadata.setGroup(BaseServiceMetadata.groupFromServiceKey(serviceKey));
        }
    }

    public void initMethodModels() {
        Class<?>[] interfaceList;
        if (proxyObject == null) {
            Class<?> serviceInterfaceClass = referenceConfig.getServiceInterfaceClass();
            if (serviceInterfaceClass != null) {
                interfaceList = new Class[]{serviceInterfaceClass};
            } else {
                interfaceList = new Class[0];
            }
        } else {
            interfaceList = proxyObject.getClass().getInterfaces();
        }
        for (Class<?> interfaceClass : interfaceList) {
            for (Method method : interfaceClass.getMethods()) {
                methodModels.put(method, new ConsumerMethodModel(method));
            }
        }
    }

    public ClassLoader getClassLoader() {
        Class<?> serviceType = serviceMetadata.getServiceType();
        return serviceType != null ? serviceType.getClassLoader() : ClassUtils.getClassLoader();
    }

    /**
     * @return serviceMetadata
     */
    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }

    /**
     * Return method model for the given method on consumer side
     *
     * @param method method object
     * @return method model
     */
    public ConsumerMethodModel getMethodModel(Method method) {
        return methodModels.get(method);
    }

    /**
     * Return method model for the given method on consumer side
     *
     * @param method method object
     * @return method model
     */
    public ConsumerMethodModel getMethodModel(String method) {
        Optional<Map.Entry<Method, ConsumerMethodModel>> consumerMethodModelEntry = methodModels.entrySet().stream().filter(entry -> entry.getKey().getName().equals(method)).findFirst();
        return consumerMethodModelEntry.map(Map.Entry::getValue).orElse(null);
    }

    /**
     * @param method   methodName
     * @param argsType method arguments type
     * @return
     */
    public ConsumerMethodModel getMethodModel(String method, String[] argsType) {
        Optional<ConsumerMethodModel> consumerMethodModel = methodModels.entrySet().stream()
            .filter(entry -> entry.getKey().getName().equals(method))
            .map(Map.Entry::getValue).filter(methodModel -> Arrays.equals(argsType, methodModel.getParameterTypes()))
            .findFirst();
        return consumerMethodModel.orElse(null);
    }

    /**
     * Return all method models for the current service
     *
     * @return method model list
     */
    public List<ConsumerMethodModel> getAllMethodModels() {
        return new ArrayList<>(methodModels.values());
    }

    public String getServiceName() {
        return this.serviceMetadata.getServiceKey();
    }


}
