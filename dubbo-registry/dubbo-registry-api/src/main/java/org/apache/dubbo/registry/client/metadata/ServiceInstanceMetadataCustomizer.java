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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.infra.InfraAdapter;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataParamsFilter;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;

/**
 *
 */
public class ServiceInstanceMetadataCustomizer implements ServiceInstanceCustomizer {

    /**
     * 获取环境变量或者jvm启动参数信息  存入ServiceInstance对应的元数据
     * @param serviceInstance {@link ServiceInstance the service instance}
     */
    @Override
    public void customize(ServiceInstance serviceInstance) {

        /**
         * 获取MetadataParamsFilter对应的实现集合
         */
        ExtensionLoader<MetadataParamsFilter> loader = ExtensionLoader.getExtensionLoader(MetadataParamsFilter.class);
        Set<MetadataParamsFilter> paramsFilters = loader.getSupportedExtensionInstances();

        /**？？？？？
         * 获取InMemoryWritableMetadataService
         */
        WritableMetadataService localMetadataService = WritableMetadataService.getDefaultExtension();
        // pick the first interface metadata available.
        // FIXME, check the same key in different urls has the same value
        /**
         * 获取本地缓存得元数据信息（第一个）
         */
        MetadataInfo metadataInfo = localMetadataService.getMetadataInfos().values().iterator().next();
        /**
         * 获取元数据信息中第一个服务信息
         */
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServices().values().iterator().next();
        /**
         * 获取服务对应url中包含得parameters
         */
        Map<String, String> allParams = new HashMap<>(serviceInfo.getUrl().getParameters());

        // load instance params users want to load.
        // TODO, duplicate logic with that in ApplicationConfig
        /**
         * InfraAdapter对应得实现EnvironmentAdapter
         * 环境变量信息和jvm启动参数
         */
        Set<InfraAdapter> adapters = ExtensionLoader.getExtensionLoader(InfraAdapter.class).getSupportedExtensionInstances();
        if (CollectionUtils.isNotEmpty(adapters)) {
            Map<String, String> inputParameters = new HashMap<>();
            /**
             * 缓存application名称
             */
            inputParameters.put(APPLICATION_KEY, ApplicationModel.getName());
            for (InfraAdapter adapter : adapters) {
                /**
                 * 获取环境变量  如果存在则加入extraParameters
                 */
                Map<String, String> extraParameters = adapter.getExtraAttributes(inputParameters);
                if (CollectionUtils.isNotEmptyMap(extraParameters)) {
                    extraParameters.forEach(allParams::putIfAbsent);
                }
            }
        }

        /**
         * paramsFilters为空  填充服务对应的元数据信息
         */
        if (CollectionUtils.isEmpty(paramsFilters)) {
            serviceInstance.getMetadata().putAll(allParams);
            return;
        }

        paramsFilters.forEach(filter -> {
            String[] included = filter.instanceParamsIncluded();
            if (included == null) {
                serviceInstance.getMetadata().putAll(allParams);
            } else {
                for (String p : included) {
                    /**
                     * 根据included过滤allParams中的数据   加入到serviceInstance对应的元数据中
                     */
                    if (allParams.get(p) != null) {
                        serviceInstance.getMetadata().put(p, allParams.get(p));
                    }
                }
            }
        });
    }
}
