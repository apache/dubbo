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

import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryChangeListener;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.event.EventDispatcher.getDefaultExtension;

/**
 * In-Memory {@link ServiceDiscovery} implementation
 *
 * @since 2.7.2
 */
public class InMemoryServiceDiscovery implements ServiceDiscovery {

    private final EventDispatcher dispatcher = getDefaultExtension();

    private Map<String, List<ServiceInstance>> repository = new HashMap<>();

    @Override
    public Set<String> getServices() {
        return repository.keySet();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return repository.computeIfAbsent(serviceName, s -> new LinkedList<>());
    }

    public InMemoryServiceDiscovery addServiceInstance(ServiceInstance serviceInstance) {
        String serviceName = serviceInstance.getServiceName();
        List<ServiceInstance> serviceInstances = repository.computeIfAbsent(serviceName, s -> new LinkedList<>());
        if (!serviceInstances.contains(serviceInstance)) {
            serviceInstances.add(serviceInstance);
        }
        return this;
    }

    public String toString() {
        return "InMemoryServiceDiscovery";
    }

    @Override
    public void addServiceDiscoveryChangeListener(String serviceName, ServiceDiscoveryChangeListener listener) throws NullPointerException, IllegalArgumentException {
        dispatcher.addEventListener(listener);
    }

}
