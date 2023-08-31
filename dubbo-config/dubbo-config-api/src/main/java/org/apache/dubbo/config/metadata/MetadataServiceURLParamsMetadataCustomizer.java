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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.common.BaseServiceMetadata;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.registry.client.metadata.SpringCloudMetadataServiceURLBuilder;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getMetadataServiceParameter;

/**
 * Used to interact with non-dubbo systems, also see {@link SpringCloudMetadataServiceURLBuilder}
 */
public class MetadataServiceURLParamsMetadataCustomizer implements ServiceInstanceCustomizer {

    @Override
    public void customize(ServiceInstance serviceInstance, ApplicationModel applicationModel) {

        Map<String, String> metadata = serviceInstance.getMetadata();

        String propertyName = resolveMetadataPropertyName(serviceInstance);
        String propertyValue = resolveMetadataPropertyValue(applicationModel);

        if (!isBlank(propertyName) && !isBlank(propertyValue)) {
            metadata.put(propertyName, propertyValue);
        }
    }

    private String resolveMetadataPropertyName(ServiceInstance serviceInstance) {
        return METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME;
    }

    private String resolveMetadataPropertyValue(ApplicationModel applicationModel) {
        ModuleServiceRepository serviceRepository = applicationModel.getInternalModule().getServiceRepository();
        String key = BaseServiceMetadata.buildServiceKey(MetadataService.class.getName(), applicationModel.getApplicationName(), MetadataService.VERSION);
        ProviderModel providerModel = serviceRepository.lookupExportedService(key);
        String metadataValue = "";
        if (providerModel != null) {
            List<URL> metadataURLs = providerModel.getServiceUrls();
            if (CollectionUtils.isNotEmpty(metadataURLs)) {
                metadataValue = getMetadataServiceParameter(metadataURLs.get(0));
            }
        }
        return metadataValue;
    }
}
