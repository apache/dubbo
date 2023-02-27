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

<<<<<<< HEAD
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.infra.InfraAdapter;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataParamsFilter;
import org.apache.dubbo.metadata.WritableMetadataService;
=======
import org.apache.dubbo.common.infra.InfraAdapter;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MetadataInfo;
>>>>>>> origin/3.2
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;

<<<<<<< HEAD
=======
import java.util.Collections;
>>>>>>> origin/3.2
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;

/**
<<<<<<< HEAD
=======
 * <p>Intercepting instance to load instance-level params from different sources before being registered into registry.</p>
 *
 * The sources can be one or both of the following:
 * <ul>
 *  <li>os environment</li>
 *  <li>vm options</li>
 * </ul>
 *
 * So, finally, the keys left in order will be:
 * <ul>
 *  <li>all keys specified by sources above</li>
 *  <li>keys found in metadata info</li>
 * </ul>
 *
>>>>>>> origin/3.2
 *
 */
public class ServiceInstanceMetadataCustomizer implements ServiceInstanceCustomizer {

    @Override
<<<<<<< HEAD
    public void customize(ServiceInstance serviceInstance) {

        ExtensionLoader<MetadataParamsFilter> loader = ExtensionLoader.getExtensionLoader(MetadataParamsFilter.class);
        Set<MetadataParamsFilter> paramsFilters = loader.getSupportedExtensionInstances();

        WritableMetadataService localMetadataService = WritableMetadataService.getDefaultExtension();
        // pick the first interface metadata available.
        // FIXME, check the same key in different urls has the same value
        MetadataInfo metadataInfo = localMetadataService.getMetadataInfos().values().iterator().next();
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServices().values().iterator().next();
        Map<String, String> allParams = new HashMap<>(serviceInfo.getUrl().getParameters());

        // load instance params users want to load.
        // TODO, duplicate logic with that in ApplicationConfig
        Set<InfraAdapter> adapters = ExtensionLoader.getExtensionLoader(InfraAdapter.class).getSupportedExtensionInstances();
        if (CollectionUtils.isNotEmpty(adapters)) {
            Map<String, String> inputParameters = new HashMap<>();
            inputParameters.put(APPLICATION_KEY, ApplicationModel.getName());
            for (InfraAdapter adapter : adapters) {
                Map<String, String> extraParameters = adapter.getExtraAttributes(inputParameters);
                if (CollectionUtils.isNotEmptyMap(extraParameters)) {
                    extraParameters.forEach(allParams::putIfAbsent);
                }
            }
        }

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
                    if (allParams.get(p) != null) {
                        serviceInstance.getMetadata().put(p, allParams.get(p));
                    }
                }
            }
        });
=======
    public void customize(ServiceInstance serviceInstance, ApplicationModel applicationModel) {
        MetadataInfo metadataInfo = serviceInstance.getServiceMetadata();
        if (metadataInfo == null || CollectionUtils.isEmptyMap(metadataInfo.getServices())) {
            return;
        }

        // try to load instance params that do not appear in service urls
        // TODO, duplicate snippet in ApplicationConfig
        Map<String, String> extraParameters = Collections.emptyMap();
        Set<InfraAdapter> adapters = applicationModel.getExtensionLoader(InfraAdapter.class).getSupportedExtensionInstances();
        if (CollectionUtils.isNotEmpty(adapters)) {
            Map<String, String> inputParameters = new HashMap<>();
            inputParameters.put(APPLICATION_KEY, applicationModel.getApplicationName());
            for (InfraAdapter adapter : adapters) {
                extraParameters = adapter.getExtraAttributes(inputParameters);
            }
        }

        serviceInstance.getMetadata().putAll(extraParameters);
        if (CollectionUtils.isNotEmptyMap(metadataInfo.getInstanceParams())) {
            serviceInstance.getMetadata().putAll(metadataInfo.getInstanceParams());
        }
>>>>>>> origin/3.2
    }
}
