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

import org.apache.dubbo.common.utils.DefaultPage;
import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryChangeListener;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static org.apache.dubbo.common.utils.CollectionUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.CollectionUtils.sort;

/**
 * The serviceDiscoveries of {@link ServiceDiscovery}
 *
 * @since 2.7.2
 */
public class CompositeServiceDiscovery implements ServiceDiscovery {

    private List<ServiceDiscovery> serviceDiscoveries = new LinkedList<>();

    public CompositeServiceDiscovery(ServiceDiscovery... serviceDiscoveries) {
        this(asList(serviceDiscoveries));
    }

    public CompositeServiceDiscovery(Collection<ServiceDiscovery> serviceDiscoveries) {
        addServiceDiscoveries(serviceDiscoveries);
    }

    protected void addServiceDiscoveries(Collection<ServiceDiscovery> serviceDiscoveries) {
        this.serviceDiscoveries.addAll(serviceDiscoveries);
        sort(this.serviceDiscoveries);
    }

    @Override
    public Set<String> getServices() {
        Set<String> allServiceNames = new TreeSet<>();
        for (ServiceDiscovery serviceDiscovery : serviceDiscoveries) {
            Set<String> serviceNames = serviceDiscovery.getServices();
            if (isNotEmpty(serviceNames)) {
                allServiceNames.addAll(serviceNames);
            }
        }
        return unmodifiableSet(allServiceNames);
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        List<ServiceInstance> serviceInstances = new LinkedList<>();
        for (ServiceDiscovery serviceDiscovery : serviceDiscoveries) {
            List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
            if (isNotEmpty(instances)) {
                serviceInstances.addAll(instances);
            }
        }
        return serviceInstances;
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int requestSize, boolean healthyOnly)
            throws NullPointerException, IllegalArgumentException {
        DefaultPage<ServiceInstance> page = new DefaultPage<>(offset, requestSize);

        int totalElements = 0;
        List<ServiceInstance> serviceInstances = new LinkedList<>();

        for (ServiceDiscovery serviceDiscovery : serviceDiscoveries) {
            Page<ServiceInstance> p = serviceDiscovery.getInstances(serviceName, offset, requestSize, healthyOnly);
            totalElements += p.getTotalSize();
            if (p.hasData()) {
                serviceInstances.addAll(p.getData());
            }
        }

        page.setTotalSize(totalElements);
        page.setData(serviceInstances);
        return page;
    }

    @Override
    public String toString() {
        return format("%s [composite : %s]", this.getClass().getSimpleName(), valueOf(serviceDiscoveries));
    }

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void addServiceDiscoveryChangeListener(String serviceName,
                                                  ServiceDiscoveryChangeListener listener) throws NullPointerException, IllegalArgumentException {
        serviceDiscoveries.forEach(serviceDiscovery -> serviceDiscovery.addServiceDiscoveryChangeListener(serviceName, listener));
    }
}
