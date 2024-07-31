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

import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Dubbo online
 *
 * @since 3.3.0
 */
@Component
public class DubboOfflineMetadata extends AbstractDubboMetadata {

    @Autowired
    public ApplicationModel applicationModel;

    public Map<String, Object> baseOffline(
            String servicePattern, Predicate<ProviderModel.RegisterStatedURL> filterCondition, String status) {
        Map<String, Object> offlineInfo = new LinkedHashMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(
                Math.min(Runtime.getRuntime().availableProcessors(), 4),
                new NamedThreadFactory("Actuator-Dubbo-Offline"));
        try {
            List<CompletableFuture<Void>> futures = new LinkedList<>();
            Collection<ProviderModel> providerModelList =
                    applicationModel.getApplicationServiceRepository().allProviderModels();
            for (ProviderModel providerModel : providerModelList) {
                ServiceMetadata metadata = providerModel.getServiceMetadata();
                if (metadata.getServiceKey().matches(servicePattern)
                        || metadata.getDisplayServiceKey().matches(servicePattern)) {
                    List<ProviderModel.RegisterStatedURL> statedUrls = providerModel.getStatedUrl();
                    for (ProviderModel.RegisterStatedURL statedUrl : statedUrls) {
                        if (filterCondition.test(statedUrl)) {
                            futures.add(CompletableFuture.runAsync(
                                    () -> {
                                        doUnexport(statedUrl);
                                        offlineInfo.put(metadata.getDisplayServiceKey(), status);
                                    },
                                    executorService));
                        }
                    }
                }
            }
            for (CompletableFuture<Void> future : futures) {
                future.get();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
        return offlineInfo;
    }

    public Map<String, Object> offline(String servicePattern) {
        return baseOffline(servicePattern, ProviderModel.RegisterStatedURL::isRegistered, "offline");
    }

    public Map<String, Object> offlineApp(String servicePattern) {
        return baseOffline(
                servicePattern,
                statedUrl -> statedUrl.isRegistered() && UrlUtils.isServiceDiscoveryURL(statedUrl.getRegistryUrl()),
                "offlineApplication");
    }

    public Map<String, Object> offlineInterface(String servicePattern) {
        return baseOffline(
                servicePattern,
                statedUrl -> statedUrl.isRegistered() && !UrlUtils.isServiceDiscoveryURL(statedUrl.getRegistryUrl()),
                "offlineInterface");
    }

    protected void doUnexport(ProviderModel.RegisterStatedURL statedURL) {
        RegistryFactory registryFactory = statedURL
                .getRegistryUrl()
                .getOrDefaultApplicationModel()
                .getExtensionLoader(RegistryFactory.class)
                .getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(statedURL.getRegistryUrl());
        registry.unregister(statedURL.getProviderUrl());
        statedURL.setRegistered(false);
    }
}
