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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.infra.InfraAdapter;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataParamsFilter;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.registry.client.metadata.store.InMemoryWritableMetadataService;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;

/**
 * <p>Intercepting instance to load instance-level params from different sources before being registered to registry.</p>
 *
 * The sources can be:
 * <ul>
 *  <li>os environment</li>
 *  <li>vm options</li>
 * </ul>
 *
 * The keys left are determined by:
 * <ul>
 *  <li>all keys specified by sources above</li>
 *  <li>keys specified in param filters</li>
 * </ul>
 *
 *
 */
public class ServiceInstanceMetadataCustomizer implements ServiceInstanceCustomizer {

    @Override
    public void customize(ServiceInstance serviceInstance) {
        ApplicationModel applicationModel = serviceInstance.getApplicationModel();
        ExtensionLoader<MetadataParamsFilter> loader = applicationModel.getExtensionLoader(MetadataParamsFilter.class);

        InMemoryWritableMetadataService localMetadataService
                = (InMemoryWritableMetadataService) WritableMetadataService.getDefaultExtension(applicationModel);
        // pick the first interface metadata available.
        // FIXME, check the same key in different urls have the same value
        Map<String, MetadataInfo> metadataInfos = localMetadataService.getMetadataInfos();
        if (CollectionUtils.isEmptyMap(metadataInfos)) {
            return;
        }
        MetadataInfo metadataInfo = metadataInfos.values().iterator().next();
        if (metadataInfo == null || CollectionUtils.isEmptyMap(metadataInfo.getServices())) {
            return;
        }
        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServices().values().iterator().next();
        URL url = serviceInfo.getUrl();
        List<MetadataParamsFilter> paramsFilters = loader.getActivateExtension(url, "params-filter");
        Map<String, String> allParams = new HashMap<>(url.getParameters());

        // load instance params users want to load.
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

        if (CollectionUtils.isEmpty(paramsFilters)) {
            serviceInstance.getMetadata().putAll(extraParameters);
            return;
        }

        serviceInstance.getMetadata().putAll(extraParameters);;
        paramsFilters.forEach(filter -> {
            String[] included = filter.instanceParamsIncluded();
            if (ArrayUtils.isEmpty(included)) {
                /*
                 * Does not put any parameter in instance if not specified.
                 * It has no functional impact as long as params appear in service metadata.
                 */
            } else {
                for (String p : included) {
                    if (allParams.get(p) != null) {
                        serviceInstance.getMetadata().put(p, allParams.get(p));
                    }
                }
            }
        });
    }
}
