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
package org.apache.dubbo.registry.multiple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

public class MultipleServiceDiscovery implements ServiceDiscovery {
    public static final String REGISTRY_PREFIX_KEY = "child.";
    private static final String REGISTRY_TYPE = "registry-type";
    private static final String SERVICE = "service";
    private final Map<String, ServiceDiscovery> serviceDiscoveries = new ConcurrentHashMap<>();
    private URL registryURL;
    private String applicationName;
    private volatile boolean isDestroy;

    public MultipleServiceDiscovery(URL registryURL) {
        this.registryURL = registryURL;
        this.applicationName = registryURL.getApplication();

        Map<String, String> parameters = registryURL.getParameters();
        for (String key : parameters.keySet()) {
            if (key.startsWith(REGISTRY_PREFIX_KEY)) {
                URL url = URL.valueOf(registryURL.getParameter(key)).addParameter(CommonConstants.APPLICATION_KEY, applicationName)
                    .addParameter(REGISTRY_TYPE, SERVICE);
                ServiceDiscovery serviceDiscovery = ServiceDiscoveryFactory.getExtension(url).getServiceDiscovery(url);
                serviceDiscoveries.put(key, serviceDiscovery);
            }
        }
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    @Override
    public void destroy() throws Exception {
        this.isDestroy = true;
        for (ServiceDiscovery serviceDiscovery : serviceDiscoveries.values()) {
            serviceDiscovery.destroy();
        }
    }

    @Override
    public boolean isDestroy() {
        return isDestroy;
    }

    @Override
    public void register() throws RuntimeException {
        serviceDiscoveries.values().forEach(serviceDiscovery -> serviceDiscovery.register());
    }

    @Override
    public void update() throws RuntimeException {
        serviceDiscoveries.values().forEach(serviceDiscovery -> serviceDiscovery.update());
    }

    @Override
    public void unregister() throws RuntimeException {
        serviceDiscoveries.values().forEach(serviceDiscovery -> serviceDiscovery.unregister());
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
        throws NullPointerException, IllegalArgumentException {
        MultiServiceInstancesChangedListener multiListener = (MultiServiceInstancesChangedListener) listener;

        for (String registryKey : serviceDiscoveries.keySet()) {
            ServiceDiscovery serviceDiscovery = serviceDiscoveries.get(registryKey);
            SingleServiceInstancesChangedListener singleListener = multiListener.getAndComputeIfAbsent(registryKey, k ->
                new SingleServiceInstancesChangedListener(listener.getServiceNames(), serviceDiscovery, multiListener));
            serviceDiscovery.addServiceInstancesChangedListener(singleListener);
        }
    }

    @Override
    public ServiceInstancesChangedListener createListener(Set<String> serviceNames) {
        return new MultiServiceInstancesChangedListener(serviceNames, this);
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) {
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for (ServiceDiscovery serviceDiscovery : serviceDiscoveries.values()) {
            serviceInstanceList.addAll(serviceDiscovery.getInstances(serviceName));
        }
        return serviceInstanceList;
    }

    @Override
    public Set<String> getServices() {
        Set<String> services = new HashSet<>();
        for (ServiceDiscovery serviceDiscovery : serviceDiscoveries.values()) {
            services.addAll(serviceDiscovery.getServices());
        }
        return services;
    }

    @Override
    public ServiceInstance getLocalInstance() {
        return null;
    }

    @Override
    public MetadataInfo getLocalMetadata() {
        throw new UnsupportedOperationException("Multiple registry implementation does not support getMetadata() method.");
    }

    @Override
    public MetadataInfo getLocalMetadata(String revision) {
        throw new UnsupportedOperationException("Multiple registry implementation does not support getLocalMetadata() method.");
    }

    @Override
    public MetadataInfo getRemoteMetadata(String revision) {
        throw new UnsupportedOperationException("Multiple registry implementation does not support getMetadata() method.");
    }

    @Override
    public MetadataInfo getRemoteMetadata(String revision, List<ServiceInstance> instances) {
        throw new UnsupportedOperationException("Multiple registry implementation does not support getMetadata() method.");
    }

    @Override
    public void register(URL url) {
        serviceDiscoveries.values().forEach(serviceDiscovery -> serviceDiscovery.register(url));
    }

    @Override
    public void unregister(URL url) {
        serviceDiscoveries.values().forEach(serviceDiscovery -> serviceDiscovery.unregister(url));
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        serviceDiscoveries.values().forEach(serviceDiscovery -> serviceDiscovery.subscribe(url, listener));
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        serviceDiscoveries.values().forEach(serviceDiscovery -> serviceDiscovery.unsubscribe(url, listener));
    }

    @Override
    public List<URL> lookup(URL url) {
        throw new UnsupportedOperationException("Multiple registry implementation does not support lookup() method.");
    }

    protected static class MultiServiceInstancesChangedListener extends ServiceInstancesChangedListener {
        private final ConcurrentMap<String, SingleServiceInstancesChangedListener> singleListenerMap;

        public MultiServiceInstancesChangedListener(Set<String> serviceNames, ServiceDiscovery serviceDiscovery) {
            super(serviceNames, serviceDiscovery);
            this.singleListenerMap = new ConcurrentHashMap<>();
        }

        @Override
        public void onEvent(ServiceInstancesChangedEvent event) {
            List<ServiceInstance> serviceInstances = new ArrayList<>();
            for (SingleServiceInstancesChangedListener singleListener : singleListenerMap.values()) {
                if (null != singleListener.event && null != singleListener.event.getServiceInstances()) {
                    for (ServiceInstance serviceInstance : singleListener.event.getServiceInstances()) {
                        if (!serviceInstances.contains(serviceInstance)) {
                            serviceInstances.add(serviceInstance);
                        }
                    }
                }
            }

            super.onEvent(new ServiceInstancesChangedEvent(event.getServiceName(), serviceInstances));
        }

        public void putSingleListener(String registryKey, SingleServiceInstancesChangedListener singleListener) {
            singleListenerMap.put(registryKey, singleListener);
        }

        public SingleServiceInstancesChangedListener getAndComputeIfAbsent(String registryKey,
                                                                           Function<String, SingleServiceInstancesChangedListener> func) {
            return ConcurrentHashMapUtils.computeIfAbsent(singleListenerMap, registryKey, func);
        }
    }

    protected static class SingleServiceInstancesChangedListener extends ServiceInstancesChangedListener {
        private final MultiServiceInstancesChangedListener multiListener;
        volatile ServiceInstancesChangedEvent event;

        public SingleServiceInstancesChangedListener(Set<String> serviceNames, ServiceDiscovery serviceDiscovery,
                                                     MultiServiceInstancesChangedListener multiListener) {
            super(serviceNames, serviceDiscovery);
            this.multiListener = multiListener;
        }

        @Override
        public void onEvent(ServiceInstancesChangedEvent event) {
            this.event = event;
            if (multiListener != null) {
                multiListener.onEvent(event);
            }
        }
    }
}
