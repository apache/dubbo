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
package org.apache.dubbo.registry.support.cloud;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.FailbackRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.apache.dubbo.common.Constants.PROVIDER_SIDE;
import static org.apache.dubbo.common.Constants.SIDE_KEY;

/**
 * Dubbo Cloud-Native Service {@link Registry} abstraction
 *
 * @param <S> The subclass of {@link ServiceInstance}
 * @since 2.7.1
 */
public class CloudNativeRegistry<S extends ServiceInstance> extends FailbackRegistry {

    /**
     * The parameter name of {@link #allServicesLookupInterval}
     */
    public static final String ALL_SERVICES_LOOKUP_INTERVAL_PARAM_NAME = "dubbo.all.services.lookup.interval";

    /**
     * The parameter name of {@link #registeredServicesLookupInterval}
     */
    public static final String REGISTERED_SERVICES_LOOKUP_INTERVAL_PARAM_NAME = "dubbo.registered.services.lookup.interval";

    /**
     * The interval in second of lookup service names(only for Dubbo-OPS)
     */
    private final long allServicesLookupInterval;

    private final long registeredServicesLookupInterval;

    private final CloudServiceRegistry<S> cloudServiceRegistry;

    private final CloudServiceDiscovery<S> cloudServiceDiscovery;

    private final ServiceInstanceFactory<S> serviceInstanceFactory;

    private final ScheduledExecutorService servicesLookupScheduler;

    public CloudNativeRegistry(URL url,
                               CloudServiceRegistry<S> cloudServiceRegistry,
                               CloudServiceDiscovery<S> cloudServiceDiscovery,
                               ServiceInstanceFactory<S> serviceInstanceFactory,
                               ScheduledExecutorService servicesLookupScheduler) {
        super(url);
        this.allServicesLookupInterval = url.getParameter(ALL_SERVICES_LOOKUP_INTERVAL_PARAM_NAME, 30L);
        this.registeredServicesLookupInterval = url.getParameter(REGISTERED_SERVICES_LOOKUP_INTERVAL_PARAM_NAME, 300L);
        this.cloudServiceRegistry = cloudServiceRegistry;
        this.cloudServiceDiscovery = cloudServiceDiscovery;
        this.serviceInstanceFactory = serviceInstanceFactory;
        this.servicesLookupScheduler = servicesLookupScheduler;
    }

    protected boolean shouldRegister(S serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        String side = metadata.get(SIDE_KEY);
        return PROVIDER_SIDE.equals(side); // Only register the Provider.
    }

    @Override
    public void doRegister(URL url) {
        S serviceInstance = serviceInstanceFactory.create(url);
        if (shouldRegister(serviceInstance)) {
            cloudServiceRegistry.register(serviceInstance);
        }
    }

    @Override
    public void doUnregister(URL url) {
        S serviceInstance = serviceInstanceFactory.create(url);
        if (shouldRegister(serviceInstance)) {
            cloudServiceRegistry.deregister(serviceInstance);
        }
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        List<String> serviceNames = getServiceNames(url, listener);
        doSubscribe(url, listener, serviceNames);
        this.servicesLookupScheduler.scheduleAtFixedRate(() -> {
            doSubscribe(url, listener, serviceNames);
        }, registeredServicesLookupInterval, registeredServicesLookupInterval, TimeUnit.SECONDS);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        if (isAdminProtocol(url)) {
            shutdownServiceNamesLookup();
        }
    }

    private void shutdownServiceNamesLookup() {
        if (servicesLookupScheduler != null) {
            servicesLookupScheduler.shutdown();
        }
    }

    /**
     * Get all service names
     *
     * @return non-null {@link List}
     */
    protected List<String> getAllServiceNames() {
        return cloudServiceDiscovery.getServices();
    }

    private void doSubscribe(final URL url, final NotifyListener listener, final List<String> serviceNames) {
        for (String serviceName : serviceNames) {
            List<S> serviceInstances = cloudServiceDiscovery.getServiceInstances(serviceName);
            notifySubscriber(url, listener, serviceInstances);
        }
    }

    /**
     * Get the service names from the specified {@link URL url}
     *
     * @param url      {@link URL}
     * @param listener {@link NotifyListener}
     * @return non-null
     */
    private List<String> getServiceNames(URL url, NotifyListener listener) {
        if (isAdminProtocol(url)) {
            initAllServicesLookupScheduler(url, listener);
            return getServiceNamesForOps(url);
        } else {
            return singletonList(serviceInstanceFactory.createServiceName(url));
        }
    }

    /**
     * Get the service names for Dubbo OPS
     *
     * @param url {@link URL}
     * @return non-null
     */
    private List<String> getServiceNamesForOps(URL url) {
        List<String> serviceNames = getAllServiceNames();
        filterServiceNames(serviceNames);
        return serviceNames;
    }


    private boolean isAdminProtocol(URL url) {
        return Constants.ADMIN_PROTOCOL.equals(url.getProtocol());
    }

    private <T> void filter(Collection<T> collection, Predicate<T> predicate) {
        Iterator<T> iterator = collection.iterator();
        while (iterator.hasNext()) {
            T data = iterator.next();
            if (!predicate.test(data)) { // remove if not accept
                iterator.remove();
            }
        }
    }

    private void initAllServicesLookupScheduler(final URL url, final NotifyListener listener) {
        servicesLookupScheduler.scheduleAtFixedRate(() -> {
            List<String> serviceNames = getAllServiceNames();
            filterServiceNames(serviceNames);
            doSubscribe(url, listener, serviceNames);
        }, allServicesLookupInterval, allServicesLookupInterval, TimeUnit.SECONDS);
    }

    private void filterServiceNames(List<String> serviceNames) {
        filter(serviceNames, cloudServiceDiscovery::supports);
    }

    private void doSubscribe(final URL url, final NotifyListener listener, final Set<String> serviceNames) {
        Collection<S> serviceInstances = serviceNames.stream()
                .map(cloudServiceDiscovery::getServiceInstances)
                .flatMap(v -> v.stream())
                .collect(Collectors.toList());
        notifySubscriber(url, listener, serviceInstances);
    }

    /**
     * Notify the Healthy {@link S service instance} to subscriber.
     *
     * @param url              {@link URL}
     * @param listener         {@link NotifyListener}
     * @param serviceInstances all {@link S registrations}
     */
    private void notifySubscriber(URL url, NotifyListener listener, Collection<S> serviceInstances) {
        Set<S> healthyServiceInstances = new LinkedHashSet<S>(serviceInstances);
        // Healthy Instances
        filterHealthyInstances(healthyServiceInstances);
        List<URL> urls = buildURLs(url, healthyServiceInstances);
        this.notify(url, listener, urls);
    }

    private void filterHealthyInstances(Collection<S> serviceInstances) {
        filter(serviceInstances, cloudServiceRegistry::isHealthy);
    }

    private List<URL> buildURLs(URL consumerURL, Collection<S> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            return Collections.emptyList();
        }
        List<URL> urls = new LinkedList<URL>();
        for (S serviceInstance : serviceInstances) {
            URL url = buildURL(serviceInstance);
            if (UrlUtils.isMatch(consumerURL, url)) {
                urls.add(url);
            }
        }
        return urls;
    }

    private URL buildURL(S serviceInstance) {
        URL url = new URL(serviceInstance.getMetadata().get(Constants.PROTOCOL_KEY),
                serviceInstance.getHost(), serviceInstance.getPort(),
                serviceInstance.getMetadata());
        return url;
    }

    @Override
    public boolean isAvailable() {
        return cloudServiceRegistry.isAvailable();
    }
}
