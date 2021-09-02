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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.BaseServiceMetadata.interfaceFromServiceKey;
import static org.apache.dubbo.common.BaseServiceMetadata.versionFromServiceKey;

/**
 * Service repository for framework
 */
public class FrameworkServiceRepository {
    private FrameworkModel frameworkModel;

    private static final Logger logger = LoggerFactory.getLogger(FrameworkServiceRepository.class);

    // useful to find a provider model quickly with serviceInterfaceName:version
    private ConcurrentMap<String, ProviderModel> providersWithoutGroup = new ConcurrentHashMap<>();

    // useful to find a url quickly with serviceInterfaceName:version
    private ConcurrentMap<String, Set<URL>> providerUrlsWithoutGroup = new ConcurrentHashMap<>();

    public FrameworkServiceRepository(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    public void registerProvider(ProviderModel providerModel) {
        String key = keyWithoutGroup(providerModel.getServiceKey());
        ProviderModel previous = providersWithoutGroup.putIfAbsent(key, providerModel);
        if (previous != null && previous != providerModel) {
            // TODO process register duplicate provider
            //throw new IllegalStateException("Register duplicate provider for key: " + key);
            logger.error("Register duplicate provider for key: " + key);
        }
    }

    public void unregisterProvider(ProviderModel providerModel) {
        String key = keyWithoutGroup(providerModel.getServiceKey());
        providersWithoutGroup.remove(key);
        providerUrlsWithoutGroup.remove(key);
    }

    public ProviderModel lookupExportedServiceWithoutGroup(String key) {
        return providersWithoutGroup.get(key);
    }

    public void registerProviderUrl(URL url) {
        providerUrlsWithoutGroup.computeIfAbsent(keyWithoutGroup(url.getServiceKey()), (k) -> new ConcurrentHashSet<>()).add(url);
    }

    public Set<URL> lookupRegisteredProviderUrlsWithoutGroup(String key) {
        return providerUrlsWithoutGroup.get(key);
    }

    public ServiceDescriptor lookupService(String interfaceName) {
        for (ApplicationModel applicationModel : frameworkModel.getApplicationModels()) {
            ServiceDescriptor serviceDescriptor = applicationModel.getApplicationServiceRepository().lookupService(interfaceName);
            if (serviceDescriptor != null) {
                return serviceDescriptor;
            }
        }
        return null;
    }

    private static String keyWithoutGroup(String serviceKey) {
        String interfaceName = interfaceFromServiceKey(serviceKey);
        String version = versionFromServiceKey(serviceKey);
        if (StringUtils.isEmpty(version)) {
            return interfaceName;
        }
        return interfaceName + ":" + version;
    }

}
