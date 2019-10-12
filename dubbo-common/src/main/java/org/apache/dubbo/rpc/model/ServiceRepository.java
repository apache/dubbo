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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SPI("default")
public interface ServiceRepository {

    void registerService(Class<?> interfaceClazz);

    void registerConsumer(String serviceKey, Object proxyObject, ServiceModel serviceModel, Map<String, Object> attributes);

    void registerProvider(String serviceKey, Object serviceInstance, ServiceModel serviceModel);

    List<ServiceModel> getAllServices();

    ServiceModel lookupService(String interfaceName);

    MethodModel lookupMethod(String interfaceName, String methodName);

    List<ProviderModel> getExportedServices();

    ProviderModel lookupExportedService(String serviceKey);

    List<ConsumerModel> getReferredServices();

    ConsumerModel lookupReferredService(String serviceKey);

    static ExtensionLoader<ServiceRepository> loader = ExtensionLoader.getExtensionLoader(ServiceRepository.class);

    static void init(String name) {
        if (StringUtils.isNotEmpty(name)) {
            loader.getExtension(name);
        }
    }

    static ServiceRepository getLoadedInstance() {
        Set<Object> instances = loader.getLoadedExtensionInstances();
        if (CollectionUtils.isNotEmpty(instances)) {
            return (ServiceRepository) instances.iterator().next();
        } else {
            return loader.getDefaultExtension();
        }
    }
}
