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

import java.util.List;
import java.util.Set;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

<<<<<<< HEAD
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.apache.dubbo.event.EventDispatcher.getDefaultExtension;
=======
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_DELAY_NOTIFICATION_KEY;
>>>>>>> origin/3.2

/**
 * Defines the common operations of Service Discovery, extended and loaded by ServiceDiscoveryFactory
 */
public interface ServiceDiscovery extends RegistryService, Prioritized {

    void register() throws RuntimeException;

    void update() throws RuntimeException;

<<<<<<< HEAD
    /**
     * Destroy the {@link ServiceDiscovery}
     *
     * @throws Exception If met with error
     */
    void destroy() throws Exception;

    // ==================================================================================== //

    // =================================== Registration =================================== //

    /**
     * Registers an instance of {@link ServiceInstance}.
     *
     * @param serviceInstance an instance of {@link ServiceInstance} to be registered
     * @throws RuntimeException if failed
     */
    void register(ServiceInstance serviceInstance) throws RuntimeException;

    /**
     * Updates the registered {@link ServiceInstance}.
     *
     * @param serviceInstance the registered {@link ServiceInstance}
     * @throws RuntimeException if failed
     */
    void update(ServiceInstance serviceInstance) throws RuntimeException;

    /**
     * Unregisters an instance of {@link ServiceInstance}.
     *
     * @param serviceInstance an instance of {@link ServiceInstance} to be unregistered
     * @throws RuntimeException if failed
     */
    void unregister(ServiceInstance serviceInstance) throws RuntimeException;

    // ==================================================================================== //

    // ==================================== Discovery ===================================== //

    /**
     * Get the default size of pagination query
     *
     * @return the default value is 100
     */
    default int getDefaultPageSize() {
        return 100;
    }
=======
    void unregister() throws RuntimeException;
>>>>>>> origin/3.2

    /**
     * Gets all service names
     *
     * @return non-null read-only {@link Set}
     */
    Set<String> getServices();

<<<<<<< HEAD
    /**
     * Gets all {@link ServiceInstance service instances} by the specified service name.
     *
     * @param serviceName the service name
     * @return non-null {@link List}
     * @throws NullPointerException if <code>serviceName</code> is <code>null</code>
     */
    default List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {

        List<ServiceInstance> allInstances = new LinkedList<>();

        int offset = 0;

        int pageSize = getDefaultPageSize();

        Page<ServiceInstance> page = getInstances(serviceName, offset, pageSize);

        allInstances.addAll(page.getData());

        while (page.hasNext()) {
            offset += page.getDataSize();
            page = getInstances(serviceName, offset, pageSize);
            allInstances.addAll(page.getData());
        }
=======
    List<ServiceInstance> getInstances(String serviceName) throws NullPointerException;
>>>>>>> origin/3.2

    default void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
            throws NullPointerException, IllegalArgumentException {
    }

    /**
     * unsubscribe to instance change event.
     *
<<<<<<< HEAD
     * @param serviceName the service name
     * @param offset      the offset of request , the number "0" indicates first page
     * @param pageSize    the number of request, the {@link Integer#MAX_VALUE max value} indicates the range is unlimited
     * @return non-null {@link Page} object
     * @throws NullPointerException          if <code>serviceName</code> is <code>null</code>
     * @throws IllegalArgumentException      if <code>offset</code> or <code>pageSize</code> is negative number
     * @throws UnsupportedOperationException if not supported
=======
     * @param listener
     * @throws IllegalArgumentException
>>>>>>> origin/3.2
     */
    default void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
            throws IllegalArgumentException {
    }

<<<<<<< HEAD
    /**
     * Get the {@link Page pagination} of {@link ServiceInstance service instances} by the specified service name.
     * If <code>healthyOnly == true</code>, filter healthy instances only.
     *
     * @param serviceName the service name
     * @param offset      the offset of request , the number "0" indicates first page
     * @param pageSize    the number of request, the {@link Integer#MAX_VALUE max value} indicates the range is unlimited
     * @param healthyOnly if <code>true</code> , filter healthy instances only
     * @return non-null {@link Page} object
     * @throws NullPointerException          if <code>serviceName</code> is <code>null</code>
     * @throws IllegalArgumentException      if <code>offset</code> or <code>pageSize</code> is negative number
     * @throws UnsupportedOperationException if not supported
     */
    default Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize, boolean healthyOnly) throws
            NullPointerException, IllegalArgumentException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Current implementation does not support pagination query method.");
    }

    /**
     * batch-get all {@link ServiceInstance service instances} by the specified service names
     *
     * @param serviceNames the multiple service names
     * @param offset       the offset of request , the number "0" indicates first page
     * @param requestSize  the number of request, the {@link Integer#MAX_VALUE max value} indicates the range is unlimited
     * @return non-null read-only {@link Map} whose key is the service name and value is
     * the {@link Page pagination} of {@link ServiceInstance service instances}
     * @throws NullPointerException          if <code>serviceName</code> is <code>null</code>
     * @throws IllegalArgumentException      if <code>offset</code> or <code>requestSize</code> is negative number
     * @throws UnsupportedOperationException if not supported
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
     * Add an instance of {@link ServiceInstancesChangedListener} for specified service
     * <p>
     * Default, current method will be invoked by {@link ServiceDiscoveryRegistry#subscribe(URL, NotifyListener)
     * the ServiceDiscoveryRegistry on the subscription}, and it's mandatory to
     * {@link EventDispatcher#addEventListener(EventListener) add} the {@link ServiceInstancesChangedListener} argument
     * into {@link EventDispatcher} whether the subclass implements same approach or not, thus this method is used to
     * trigger or adapt the vendor's change notification mechanism typically, like Zookeeper Watcher,
     * Nacos EventListener. If the registry observes the change, It's suggested that the implementation could invoke
     * {@link #dispatchServiceInstancesChangedEvent(String)} method or variants
     *
     * @param listener an instance of {@link ServiceInstancesChangedListener}
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @see EventPublishingServiceDiscovery
     * @see EventDispatcher
     */
    default void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
            throws NullPointerException, IllegalArgumentException {
    }

    /**
     * unsubscribe to instances change event.
     * @param listener
     * @throws IllegalArgumentException
     */
    default void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
            throws IllegalArgumentException {
    }

    /**
     * Dispatch the {@link ServiceInstancesChangedEvent}
     *
     * @param serviceName the name of service whose service instances have been changed
     */
    default void dispatchServiceInstancesChangedEvent(String serviceName) {
        dispatchServiceInstancesChangedEvent(serviceName, getInstances(serviceName));
=======
    default ServiceInstancesChangedListener createListener(Set<String> serviceNames) {
        return new ServiceInstancesChangedListener(serviceNames, this);
    }

    ServiceInstance getLocalInstance();

    MetadataInfo getLocalMetadata();

    default MetadataInfo getLocalMetadata(String revision) {
        return getLocalMetadata();
>>>>>>> origin/3.2
    }

    MetadataInfo getRemoteMetadata(String revision);

<<<<<<< HEAD
    /**
     * Dispatch the {@link ServiceInstancesChangedEvent}
     *
     * @param serviceName      the name of service whose service instances have been changed
     * @param serviceInstances the service instances have been changed
     */
    default void dispatchServiceInstancesChangedEvent(String serviceName, List<ServiceInstance> serviceInstances) {
        dispatchServiceInstancesChangedEvent(new ServiceInstancesChangedEvent(serviceName, serviceInstances));
    }
=======
    MetadataInfo getRemoteMetadata(String revision, List<ServiceInstance> instances);
>>>>>>> origin/3.2

    /**
     * Destroy the {@link ServiceDiscovery}
     *
     * @throws Exception If met with error
     */
    void destroy() throws Exception;

    boolean isDestroy();

    default URL getUrl() {
        return null;
    }

    default long getDelay() {
        return getUrl().getParameter(REGISTRY_DELAY_NOTIFICATION_KEY, 5000);
    }

//    String getKey(URL exportedURL);

    default URL getUrl() {
        return null;
    }

    ServiceInstance getLocalInstance();

    /**
     * A human-readable description of the implementation
     *
     * @return The description.
     */

    @Override
    String toString();
}
