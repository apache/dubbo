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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServiceRepository {

    public static final String NAME = "repository";

    private AtomicBoolean inited = new AtomicBoolean(false);
    private ApplicationModel applicationModel;

    public ServiceRepository(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        initialize();
    }

    private void initialize() {
        if (inited.compareAndSet(false, true)) {
            Set<BuiltinServiceDetector> builtinServices
                = applicationModel.getExtensionLoader(BuiltinServiceDetector.class).getSupportedExtensionInstances();
            if (CollectionUtils.isNotEmpty(builtinServices)) {
                for (BuiltinServiceDetector service : builtinServices) {
                    applicationModel.getInternalModule().getServiceRepository().registerService(service.getService());
                }
            }
        }
    }

    public void destroy() {
        //TODO destroy application service repository
    }

    public Collection<ConsumerModel> allConsumerModels() {
        // aggregate from sub modules
        List<ConsumerModel> allConsumerModels = new ArrayList<>();
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            allConsumerModels.addAll(moduleModel.getServiceRepository().getReferredServices());
        }
        return allConsumerModels;
    }

    public Collection<ProviderModel> allProviderModels() {
        // aggregate from sub modules
        List<ProviderModel> allProviderModels = new ArrayList<>();
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            allProviderModels.addAll(moduleModel.getServiceRepository().getExportedServices());
        }
        return allProviderModels;
    }

    public ServiceDescriptor lookupService(String interfaceName) {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            ServiceDescriptor serviceDescriptor = moduleModel.getServiceRepository().lookupService(interfaceName);
            if (serviceDescriptor != null) {
                return serviceDescriptor;
            }
        }
        return null;
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

    public ProviderModel lookupExportedService(String serviceKey) {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            ProviderModel providerModel = moduleModel.getServiceRepository().lookupExportedService(serviceKey);
            if (providerModel != null) {
                return providerModel;
            }
        }
        return null;
    }

    public ConsumerModel lookupReferredService(String serviceKey) {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            ConsumerModel consumerModel = moduleModel.getServiceRepository().lookupReferredService(serviceKey);
            if (consumerModel != null) {
                return consumerModel;
            }
        }
        return null;
    }

}
