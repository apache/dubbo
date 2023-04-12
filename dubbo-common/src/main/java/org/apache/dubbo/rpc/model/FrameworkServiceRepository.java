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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.dubbo.common.BaseServiceMetadata.interfaceFromServiceKey;
import static org.apache.dubbo.common.BaseServiceMetadata.versionFromServiceKey;

/**
 * Service repository for framework
 */
public class FrameworkServiceRepository {

    private final FrameworkModel frameworkModel;

    // useful to find a provider model quickly with group/serviceInterfaceName:version
    private final ConcurrentMap<String, ProviderModel> providers = new ConcurrentHashMap<>();

    // useful to find a provider model quickly with serviceInterfaceName:version
    private final ConcurrentMap<String, List<ProviderModel>> providersWithoutGroup = new ConcurrentHashMap<>();

    public FrameworkServiceRepository(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    public void registerProvider(ProviderModel providerModel) {
        String key = providerModel.getServiceKey();
        ProviderModel previous = providers.putIfAbsent(key, providerModel);
        if (previous != null && previous != providerModel) {
            // TODO callback service multi instances
            // throw new IllegalStateException("Register duplicate provider for key: " + key);
        }
        String keyWithoutGroup = keyWithoutGroup(key);
        ConcurrentHashMapUtils.computeIfAbsent(providersWithoutGroup, keyWithoutGroup, (k) -> new CopyOnWriteArrayList<>()).add(providerModel);
    }

    public void unregisterProvider(ProviderModel providerModel) {
        providers.remove(providerModel.getServiceKey());
        String keyWithoutGroup = keyWithoutGroup(providerModel.getServiceKey());
        providersWithoutGroup.remove(keyWithoutGroup);
    }

    public ProviderModel lookupExportedServiceWithoutGroup(String key) {
        if (providersWithoutGroup.containsKey(key)) {
            List<ProviderModel> providerModels = providersWithoutGroup.get(key);
            return providerModels.size() > 0 ? providerModels.get(0) : null;
        } else {
            return null;
        }
    }

    public List<ProviderModel> lookupExportedServicesWithoutGroup(String key) {
        return providersWithoutGroup.get(key);
    }

    public ProviderModel lookupExportedService(String serviceKey) {
        return providers.get(serviceKey);
    }

    public List<ProviderModel> allProviderModels() {
        return Collections.unmodifiableList(new ArrayList<>(providers.values()));
    }

    public List<ConsumerModel> allConsumerModels() {
        List<ConsumerModel> consumerModels = new LinkedList<>();
        frameworkModel.getApplicationModels().forEach(applicationModel ->
            consumerModels.addAll(applicationModel.getApplicationServiceRepository().allConsumerModels()));
        return Collections.unmodifiableList(consumerModels);
    }

    private static String keyWithoutGroup(String serviceKey) {
        String interfaceName = interfaceFromServiceKey(serviceKey);
        String version = versionFromServiceKey(serviceKey);
        if (StringUtils.isEmpty(version)) {
            return interfaceName;
        }
        return interfaceName + CommonConstants.GROUP_CHAR_SEPARATOR + version;
    }

}
