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

import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dubbo Shutdown
 *
 * @since 2.7.0
 */
@Component
public class DubboShutdownMetadata extends AbstractDubboMetadata {

    @Autowired
    private ApplicationModel applicationModel;

    public Map<String, Object> shutdown() throws Exception {

        Map<String, Object> shutdownCountData = new LinkedHashMap<>();

        // registries
        RegistryManager registryManager = applicationModel.getBeanFactory().getBean(RegistryManager.class);

        int registriesCount = registryManager.getRegistries().size();

        // protocols
        int protocolsCount = getProtocolConfigsBeanMap().size();

        shutdownCountData.put("registries", registriesCount);
        shutdownCountData.put("protocols", protocolsCount);

        // Service Beans
        Map<String, ServiceBean> serviceBeansMap = getServiceBeansMap();
        if (!serviceBeansMap.isEmpty()) {
            for (ServiceBean serviceBean : serviceBeansMap.values()) {
                serviceBean.destroy();
            }
        }
        shutdownCountData.put("services", serviceBeansMap.size());

        // Reference Beans
        Collection<ReferenceConfigBase<?>> references = applicationModel.getDefaultModule().getConfigManager().getReferences();
        for (ReferenceConfigBase<?> reference : references) {
            reference.destroy();
        }
        shutdownCountData.put("references", references.size());

        // Set Result to complete
        Map<String, Object> shutdownData = new TreeMap<>();
        shutdownData.put("shutdown.count", shutdownCountData);


        return shutdownData;
    }

}
