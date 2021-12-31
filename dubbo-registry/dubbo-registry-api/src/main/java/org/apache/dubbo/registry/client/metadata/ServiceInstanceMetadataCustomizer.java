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

import org.apache.dubbo.common.infra.InfraAdapter;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;

/**
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
 *
 */
public class ServiceInstanceMetadataCustomizer implements ServiceInstanceCustomizer {

    @Override
    public void customize(ServiceInstance serviceInstance, ApplicationModel applicationModel) {
        MetadataInfo metadataInfo = ((DefaultServiceInstance)serviceInstance).getServiceMetadata();
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

        serviceInstance.getMetadata().putAll(extraParameters);;
        if (CollectionUtils.isNotEmptyMap(metadataInfo.getInstanceParams())) {
            serviceInstance.getMetadata().putAll(metadataInfo.getInstanceParams());
        }
    }
}
