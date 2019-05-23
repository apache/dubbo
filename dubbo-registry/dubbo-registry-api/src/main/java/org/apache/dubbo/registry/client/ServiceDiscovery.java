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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Integer.compare;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;

/**
 * The common operations of Service Discovery
 *
 * @since 2.7.2
 */
public interface ServiceDiscovery extends Comparable<ServiceDiscovery> {

    /**
     * A human-readable description of the implementation
     *
     * @return The description.
     */
    String toString();

    /**
     * Gets all service names
     *
     * @return non-null read-only {@link Set}
     */
    Set<String> getServices();

    /**
     * Gets all {@link ServiceInstance service instances} by the specified service name.
     *
     * @param serviceName the service name
     * @return non-null {@link List}
     * @throws NullPointerException if <code>serviceName</code> is <code>null</code> is <code>null</code>
     */
    List<ServiceInstance> getInstances(String serviceName) throws NullPointerException;

    /**
     * Gets the total size of {@link #getInstances(String)} instances}
     *
     * @param serviceName the service name
     * @return
     * @throws NullPointerException if <code>serviceName</code> is <code>null</code> is <code>null</code>
     */
    default int getTotalSizeInstances(String serviceName) throws NullPointerException {
        return getInstances(serviceName).size();
    }

    /**
     * Gets the {@link Page pagination} of {@link ServiceInstance service instances} by the specified service name.
     * It's equal to {@link #getInstances(String, int, int, boolean)} with <code>healthyOnly == true</code>
     *
     * @param serviceName the service name
     * @param offset      the offset of request , the number "0" indicates first page
     * @param requestSize the number of request, the {@link Integer#MAX_VALUE max value} indicates the range is unlimited
     * @return non-null {@link Page} object
     * @throws NullPointerException     if <code>serviceName</code> is <code>null</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>offset</code> or <code>requestSize</code> is negative number
     */
    default Page<ServiceInstance> getInstances(String serviceName, int offset, int requestSize) throws NullPointerException,
            IllegalArgumentException {
        return getInstances(serviceName, offset, requestSize, false);
    }

    /**
     * Get the {@link Page pagination} of {@link ServiceInstance service instances} by the specified service name.
     * If <code>healthyOnly == true</code>, filter healthy instances only.
     *
     * @param serviceName the service name
     * @param offset      the offset of request , the number "0" indicates first page
     * @param requestSize the number of request, the {@link Integer#MAX_VALUE max value} indicates the range is unlimited
     * @param healthyOnly if <code>true</code> , filter healthy instances only
     * @return non-null {@link Page} object
     * @throws NullPointerException     if <code>serviceName</code> is <code>null</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>offset</code> or <code>requestSize</code> is negative number
     */
    default Page<ServiceInstance> getInstances(String serviceName, int offset, int requestSize, boolean healthyOnly) throws
            NullPointerException, IllegalArgumentException {

        List<ServiceInstance> serviceInstances = getInstances(serviceName);

        DefaultPage<ServiceInstance> page = new DefaultPage(offset, requestSize);

        int totalElements = getTotalSizeInstances(serviceName);

        boolean hasMore = totalElements > offset + requestSize;

        int fromIndex = offset < totalElements ? offset : -1;

        int endIndex = hasMore ? offset + requestSize : totalElements;

        List<ServiceInstance> data = fromIndex < 0 ? emptyList() :
                new ArrayList<>(serviceInstances.subList(fromIndex, endIndex));

        Iterator<ServiceInstance> iterator = data.iterator();

        while (iterator.hasNext()) {
            ServiceInstance serviceInstance = iterator.next();
            if (!serviceInstance.isEnabled()) { // remove disabled instance
                iterator.remove();
                continue;
            }

            if (healthyOnly) {
                if (!serviceInstance.isHealthy()) {  // remove unhealthy instance
                    iterator.remove();
                    continue;
                }
            }
        }

        page.setData(data);
        page.setTotalSize(totalElements);

        return page;
    }

    /**
     * batch-get all {@link ServiceInstance service instances} by the specified service names
     *
     * @param serviceNames the multiple service names
     * @param offset       the offset of request , the number "0" indicates first page
     * @param requestSize  the number of request, the {@link Integer#MAX_VALUE max value} indicates the range is unlimited
     * @return non-null read-only {@link Map} whose key is the service name and value is
     * the {@link Page pagination} of {@link ServiceInstance service instances}
     * @throws NullPointerException     if <code>serviceName</code> is <code>null</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>offset</code> or <code>requestSize</code> is negative number
     */
    default Map<String, Page<ServiceInstance>> getInstances(Iterable<String> serviceNames, int offset, int requestSize) throws
            NullPointerException, IllegalArgumentException {
        Map<String, Page<ServiceInstance>> instances = new LinkedHashMap<>();
        for (String serviceName : serviceNames) {
            instances.put(serviceName, getInstances(serviceName, offset, requestSize));
        }
        return unmodifiableMap(instances);
    }

    /**
     * The priority of current {@link ServiceDiscovery}
     *
     * @return The {@link Integer#MIN_VALUE minimum integer} indicates the highest priority, in contrast，
     * the lowest priority is {@link Integer#MAX_VALUE the maximum integer}
     */
    default int getPriority() {
        return Integer.MAX_VALUE;
    }

    /**
     * Compares its priority
     *
     * @param that {@link ServiceDiscovery}
     * @return
     */
    @Override
    default int compareTo(ServiceDiscovery that) {
        return compare(this.getPriority(), that.getPriority());
    }

    /**
     * Add an instance of {@link ServiceDiscoveryChangeListener} for specified service
     *
     * @param serviceName the service name
     * @param listener    an instance of {@link ServiceDiscoveryChangeListener}
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    void addServiceDiscoveryChangeListener(String serviceName, ServiceDiscoveryChangeListener listener)
            throws NullPointerException, IllegalArgumentException;
}
