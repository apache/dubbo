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
package org.apache.dubbo.metadata;


import org.apache.dubbo.common.URL;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.utils.CollectionUtils.isNotEmpty;

/**
 * The composite implementation of {@link ServiceNameMapping}
 *
 * @see ParameterizedServiceNameMapping
 * @see PropertiesFileServiceNameMapping
 * @see DynamicConfigurationServiceNameMapping
 * @since 2.7.8
 */
public class CompositeServiceNameMapping implements ServiceNameMapping {

    private volatile List<ServiceNameMapping> serviceNameMappings;

    private List<ServiceNameMapping> getServiceNameMappings() {
        if (this.serviceNameMappings == null) {
            synchronized (this) {
                if (this.serviceNameMappings == null) {
                    Set<ServiceNameMapping> serviceNameMappings = loadAllServiceNameMappings();

                    removeSelf(serviceNameMappings);

                    this.serviceNameMappings = new LinkedList<>(serviceNameMappings);
                }
            }
        }
        return this.serviceNameMappings;
    }

    private Set<ServiceNameMapping> loadAllServiceNameMappings() {
        return getExtensionLoader(ServiceNameMapping.class).getSupportedExtensionInstances();
    }

    private void removeSelf(Set<ServiceNameMapping> serviceNameMappings) {
        Iterator<ServiceNameMapping> iterator = serviceNameMappings.iterator();
        while (iterator.hasNext()) {
            ServiceNameMapping serviceNameMapping = iterator.next();
            if (this.getClass().equals(serviceNameMapping.getClass())) {
                iterator.remove(); // Remove self
            }
        }
    }

    @Override
    public void map(URL exportedURL) {
        List<ServiceNameMapping> serviceNameMappings = getServiceNameMappings();
        serviceNameMappings.forEach(serviceNameMapping -> serviceNameMapping.map(exportedURL));
    }

    @Override
    public Set<String> get(URL subscribedURL) {
        List<ServiceNameMapping> serviceNameMappings = getServiceNameMappings();
        Set<String> serviceNames = null;
        for (ServiceNameMapping serviceNameMapping : serviceNameMappings) {
            serviceNames = serviceNameMapping.get(subscribedURL);
            if (isNotEmpty(serviceNames)) {
                break;
            }
        }
        return serviceNames == null ? emptySet() : unmodifiableSet(serviceNames);
    }

    @Override
    public int getPriority() {
        return MIN_PRIORITY;
    }
}
