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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.ServiceConfigBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service repository for module
 */
public class ModuleServiceRepository {

    private final ModuleModel moduleModel;

    /**
     * services
     */
    private ConcurrentMap<String, List<ServiceDescriptor>> services = new ConcurrentHashMap<>();

    /**
     * consumers ( key - group/interface:version value - consumerModel list)
     */
    private ConcurrentMap<String, List<ConsumerModel>> consumers = new ConcurrentHashMap<>();

    /**
     * providers
     */
    private final ConcurrentMap<String, ProviderModel> providers = new ConcurrentHashMap<>();
    private final FrameworkServiceRepository frameworkServiceRepository;

    public ModuleServiceRepository(ModuleModel moduleModel) {
        this.moduleModel = moduleModel;
        frameworkServiceRepository = ScopeModelUtil.getFrameworkModel(moduleModel).getServiceRepository();
    }

    public ModuleModel getModuleModel() {
        return moduleModel;
    }

    /**
     * @deprecated Replaced to {@link ModuleServiceRepository#registerConsumer(ConsumerModel)}
     */
    @Deprecated
    public void registerConsumer(String serviceKey,
                                 ServiceDescriptor serviceDescriptor,
                                 ReferenceConfigBase<?> rc,
                                 Object proxy,
                                 ServiceMetadata serviceMetadata) {
        ConsumerModel consumerModel = new ConsumerModel(serviceMetadata.getServiceKey(), proxy, serviceDescriptor, rc,
            serviceMetadata, null);
        this.registerConsumer(consumerModel);
    }

    public void registerConsumer(ConsumerModel consumerModel) {
        consumers.computeIfAbsent(consumerModel.getServiceKey(), (serviceKey) -> new CopyOnWriteArrayList<>()).add(consumerModel);
    }

    /**
     * @deprecated Replaced to {@link ModuleServiceRepository#registerProvider(ProviderModel)}
     */
    @Deprecated
    public void registerProvider(String serviceKey,
                                 Object serviceInstance,
                                 ServiceDescriptor serviceModel,
                                 ServiceConfigBase<?> serviceConfig,
                                 ServiceMetadata serviceMetadata) {
        ProviderModel providerModel = new ProviderModel(serviceKey, serviceInstance, serviceModel,
            serviceConfig, serviceMetadata);
        this.registerProvider(providerModel);
    }

    public void registerProvider(ProviderModel providerModel) {
        providers.putIfAbsent(providerModel.getServiceKey(), providerModel);
        frameworkServiceRepository.registerProvider(providerModel);
    }

    public ServiceDescriptor registerService(Class<?> interfaceClazz) {
        ServiceDescriptor serviceDescriptor = new ServiceDescriptor(interfaceClazz);
        List<ServiceDescriptor> serviceDescriptors = services.computeIfAbsent(interfaceClazz.getName(),
            k -> new CopyOnWriteArrayList<>());
        synchronized (serviceDescriptors) {
            Optional<ServiceDescriptor> previous = serviceDescriptors.stream()
                .filter(s -> s.getServiceInterfaceClass().equals(interfaceClazz)).findFirst();
            if (previous.isPresent()) {
                return previous.get();
            } else {
                serviceDescriptors.add(serviceDescriptor);
                return serviceDescriptor;
            }
        }
    }

    /**
     * See {@link #registerService(Class)}
     * <p>
     * we assume:
     * 1. services with different interfaces are not allowed to have the same path.
     * 2. services share the same interface but has different group/version can share the same path.
     * 3. path's default value is the name of the interface.
     *
     * @param path
     * @param interfaceClass
     * @return
     */
    public ServiceDescriptor registerService(String path, Class<?> interfaceClass) {
        ServiceDescriptor serviceDescriptor = registerService(interfaceClass);
        // if path is different with interface name, add extra path mapping
        if (!interfaceClass.getName().equals(path)) {
            List<ServiceDescriptor> serviceDescriptors = services.computeIfAbsent(path,
                _k -> new CopyOnWriteArrayList<>());
            synchronized (serviceDescriptors) {
                Optional<ServiceDescriptor> previous = serviceDescriptors.stream()
                    .filter(s -> s.getServiceInterfaceClass().equals(serviceDescriptor.getServiceInterfaceClass())).findFirst();
                if (previous.isPresent()) {
                    return previous.get();
                } else {
                    serviceDescriptors.add(serviceDescriptor);
                    return serviceDescriptor;
                }
            }
        }
        return serviceDescriptor;
    }

    @Deprecated
    public void reRegisterProvider(String newServiceKey, String serviceKey) {
        ProviderModel providerModel = this.providers.get(serviceKey);
        frameworkServiceRepository.unregisterProvider(providerModel);
        providerModel.setServiceKey(newServiceKey);
        this.providers.putIfAbsent(newServiceKey, providerModel);
        frameworkServiceRepository.registerProvider(providerModel);
        this.providers.remove(serviceKey);
    }

    @Deprecated
    public void reRegisterConsumer(String newServiceKey, String serviceKey) {
        List<ConsumerModel> consumerModel = this.consumers.get(serviceKey);
        consumerModel.forEach(c -> c.setServiceKey(newServiceKey));
        this.consumers.computeIfAbsent(newServiceKey, (k) -> new CopyOnWriteArrayList<>()).addAll(consumerModel);
        this.consumers.remove(serviceKey);
    }

    public void unregisterService(Class<?> interfaceClazz) {
        // TODO remove
        unregisterService(interfaceClazz.getName());
    }

    public void unregisterService(String path) {
        services.remove(path);
    }

    public void unregisterProvider(ProviderModel providerModel) {
        frameworkServiceRepository.unregisterProvider(providerModel);
        providers.remove(providerModel.getServiceKey());
    }

    public void unregisterConsumer(ConsumerModel consumerModel) {
        consumers.get(consumerModel.getServiceKey()).remove(consumerModel);
    }

    public List<ServiceDescriptor> getAllServices() {
        List<ServiceDescriptor> serviceDescriptors = services.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        return Collections.unmodifiableList(serviceDescriptors);
    }

    public ServiceDescriptor getService(String serviceName) {
        // TODO, may need to distinguish service by class loader.
        List<ServiceDescriptor> serviceDescriptors = services.get(serviceName);
        if (CollectionUtils.isEmpty(serviceDescriptors)) {
            return null;
        }
        return serviceDescriptors.get(0);
    }

    public ServiceDescriptor lookupService(String interfaceName) {
        if (services.containsKey(interfaceName)) {
            List<ServiceDescriptor> serviceDescriptors = services.get(interfaceName);
            return serviceDescriptors.size() > 0 ? serviceDescriptors.get(0) : null;
        } else {
            return null;
        }
    }

    public MethodDescriptor lookupMethod(String interfaceName, String methodName) {
        ServiceDescriptor serviceDescriptor = lookupService(interfaceName);
        if (serviceDescriptor == null) {
            return null;
        }

        List<MethodDescriptor> methods = serviceDescriptor.getMethods(methodName);
        if (CollectionUtils.isEmpty(methods)) {
            return null;
        }
        return methods.iterator().next();
    }

    public List<ProviderModel> getExportedServices() {
        return Collections.unmodifiableList(new ArrayList<>(providers.values()));
    }

    public ProviderModel lookupExportedService(String serviceKey) {
        return providers.get(serviceKey);
    }

    public List<ConsumerModel> getReferredServices() {
        List<ConsumerModel> consumerModels = consumers.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        return Collections.unmodifiableList(consumerModels);
    }

    /**
     * @deprecated Replaced to {@link ModuleServiceRepository#lookupReferredServices(String)}
     */
    @Deprecated
    public ConsumerModel lookupReferredService(String serviceKey) {
        if (consumers.containsKey(serviceKey)) {
            List<ConsumerModel> consumerModels = consumers.get(serviceKey);
            return consumerModels.size() > 0 ? consumerModels.get(0) : null;
        } else {
            return null;
        }
    }

    public List<ConsumerModel> lookupReferredServices(String serviceKey) {
        return consumers.get(serviceKey);
    }

    public void destroy() {
        for (ProviderModel providerModel : providers.values()) {
            frameworkServiceRepository.unregisterProvider(providerModel);
        }
        providers.clear();
        consumers.clear();
        services.clear();
    }
}
