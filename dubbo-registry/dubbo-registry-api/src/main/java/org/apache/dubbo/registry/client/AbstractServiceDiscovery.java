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

import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public abstract class AbstractServiceDiscovery implements ServiceDiscovery {

    protected ServiceInstance serviceInstance;

    private final Set<ServiceInstancesChangedListener> listeners = new ConcurrentHashSet<>();

    @Override
    public ServiceInstance getLocalInstance() {
        return serviceInstance;
    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {
        this.serviceInstance = serviceInstance;
        if (ServiceInstanceMetadataUtils.getExportedServicesRevision(serviceInstance) == null) {
            ServiceInstanceMetadataUtils.calInstanceRevision(this, serviceInstance);
        }
    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {
        this.serviceInstance = serviceInstance;
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        listeners.add(listener);
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        listeners.remove(listener);
    }

    public void dispatchServiceInstancesChangedEvent(String serviceName) {
        dispatchServiceInstancesChangedEvent(serviceName, getInstances(serviceName));
    }

    public void dispatchServiceInstancesChangedEvent(String serviceName, String... otherServiceNames) {
        dispatchServiceInstancesChangedEvent(serviceName, getInstances(serviceName));
        if (otherServiceNames != null) {
            Stream.of(otherServiceNames)
                    .filter(StringUtils::isNotEmpty)
                    .forEach(this::dispatchServiceInstancesChangedEvent);
        }
    }

    public void dispatchServiceInstancesChangedEvent(String serviceName, List<ServiceInstance> serviceInstances) {
        dispatchServiceInstancesChangedEvent(new ServiceInstancesChangedEvent(serviceName, serviceInstances));
    }

    public void dispatchServiceInstancesChangedEvent(ServiceInstancesChangedEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }
}
