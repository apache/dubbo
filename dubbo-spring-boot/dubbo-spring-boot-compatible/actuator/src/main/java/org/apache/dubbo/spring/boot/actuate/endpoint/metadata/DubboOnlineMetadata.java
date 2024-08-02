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
package org.apache.dubbo.spring.boot.actuate.endpoint.metadata;

import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ProviderModel.RegisterStatedURL;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Dubbo Online
 *
 * @since 3.3.0
 */
@Component
public class DubboOnlineMetadata extends AbstractDubboMetadata {

    @Autowired
    public ApplicationModel applicationModel;

    public Map<String, Object> baseOnline(
            String servicePattern, Predicate<RegisterStatedURL> filterCondition, String status) {
        Map<String, Object> onlineInfo = new LinkedHashMap<>();
        Collection<ProviderModel> providerModelList =
                applicationModel.getApplicationServiceRepository().allProviderModels();
        for (ProviderModel providerModel : providerModelList) {
            ServiceMetadata metadata = providerModel.getServiceMetadata();
            if (metadata.getServiceKey().matches(servicePattern)
                    || metadata.getDisplayServiceKey().matches(servicePattern)) {
                List<RegisterStatedURL> statedUrls = providerModel.getStatedUrl();
                for (ProviderModel.RegisterStatedURL statedUrl : statedUrls) {
                    if (filterCondition.test(statedUrl)) {
                        doExport(statedUrl);
                        onlineInfo.put(metadata.getDisplayServiceKey(), status);
                    }
                }
            }
        }
        return onlineInfo;
    }

    public Map<String, Object> online(String servicePattern) {
        return baseOnline(servicePattern, statedUrl -> !statedUrl.isRegistered(), "online");
    }

    public Map<String, Object> onlineApp(String servicePattern) {
        return baseOnline(
                servicePattern,
                statedUrl -> !statedUrl.isRegistered() && UrlUtils.isServiceDiscoveryURL(statedUrl.getRegistryUrl()),
                "onlineApplication");
    }

    public Map<String, Object> onlineInterface(String servicePattern) {
        return baseOnline(
                servicePattern,
                statedUrl -> !statedUrl.isRegistered() && !UrlUtils.isServiceDiscoveryURL(statedUrl.getRegistryUrl()),
                "onlineInterface");
    }

    protected void doExport(ProviderModel.RegisterStatedURL statedURL) {
        RegistryFactory registryFactory = statedURL
                .getRegistryUrl()
                .getOrDefaultApplicationModel()
                .getExtensionLoader(RegistryFactory.class)
                .getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(statedURL.getRegistryUrl());
        registry.register(statedURL.getProviderUrl());
        statedURL.setRegistered(true);
    }
}
