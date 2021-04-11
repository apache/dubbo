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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.DefaultPage;
import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.event.EventDispatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;

/**
 * In-Memory {@link ServiceDiscovery} implementation
 *
 * @since 2.7.5
 */
public class InMemoryServiceDiscovery extends AbstractServiceDiscovery {

    private final EventDispatcher dispatcher = EventDispatcher.getDefaultExtension();

    private Map<String, List<ServiceInstance>> repository = new HashMap<>();

    private URL registryURL;

    @Override
    public Set<String> getServices() {
        return repository.keySet();
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize, boolean healthyOnly) {
        List<ServiceInstance> instances = new ArrayList<>(repository.computeIfAbsent(serviceName, s -> new LinkedList<>()));
        int totalSize = instances.size();
        List<ServiceInstance> data = emptyList();
        if (offset < totalSize) {
            int toIndex = offset + pageSize > totalSize - 1 ? totalSize : offset + pageSize;
            data = instances.subList(offset, toIndex);
        }
        if (healthyOnly) {
            Iterator<ServiceInstance> iterator = data.iterator();
            while (iterator.hasNext()) {
                ServiceInstance instance = iterator.next();
                if (!instance.isHealthy()) {
                    iterator.remove();
                }
            }
        }
        return new DefaultPage<>(offset, pageSize, data, totalSize);
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    public String toString() {
        return "InMemoryServiceDiscovery";
    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) {
        String serviceName = serviceInstance.getServiceName();
        List<ServiceInstance> serviceInstances = repository.computeIfAbsent(serviceName, s -> new LinkedList<>());
        if (!serviceInstances.contains(serviceInstance)) {
            serviceInstances.add(serviceInstance);
        }
    }

    @Override
    public void doUpdate(ServiceInstance serviceInstance) {
        unregister(serviceInstance);
        register(serviceInstance);
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        String serviceName = serviceInstance.getServiceName();
        List<ServiceInstance> serviceInstances = repository.computeIfAbsent(serviceName, s -> new LinkedList<>());
        serviceInstances.remove(serviceInstance);
    }

    @Override
    public void initialize(URL registryURL) throws Exception {
        this.registryURL = registryURL;
    }

    @Override
    public void destroy() {
    }
}
