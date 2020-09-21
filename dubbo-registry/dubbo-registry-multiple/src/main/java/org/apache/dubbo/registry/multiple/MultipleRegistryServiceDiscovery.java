/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2020 All Rights Reserved.
 */
package org.apache.dubbo.registry.multiple;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.DefaultPage;
import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.event.ConditionalEventListener;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copyright (C) 2013-2018 Ant Financial Services Group
 *
 * @author quhongwei
 * @version : MultipleRegistryServiceDiscovery.java, v 0.1 2020年09月09日 11:24 quhongwei Exp $
 */
public class MultipleRegistryServiceDiscovery implements ServiceDiscovery {
    public static final String REGISTRY_PREFIX_KEY = "child.";
    private final Map<String, ServiceDiscovery> serviceDiscoveries = new ConcurrentHashMap<>();
    private URL registryURL;
    private ServiceInstance serviceInstance;
    private String applicationName;

    @Override
    public void initialize(URL registryURL) throws Exception {
        this.registryURL = registryURL;
        this.applicationName = registryURL.getParameter(CommonConstants.APPLICATION_KEY);

        Map<String, String> parameters = registryURL.getParameters();
        for (String key : parameters.keySet()) {
            if (key.startsWith(REGISTRY_PREFIX_KEY)) {
                URL url = URL.valueOf(registryURL.getParameter(key)).addParameter(CommonConstants.APPLICATION_KEY, applicationName)
                        .addParameter("registry-type", "service");
                ServiceDiscovery serviceDiscovery = ServiceDiscoveryFactory.getExtension(url).getServiceDiscovery(url);
                serviceDiscovery.initialize(url);
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
        for (ServiceDiscovery serviceDiscovery : serviceDiscoveries.values()) {
            serviceDiscovery.destroy();
        }
    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {
        this.serviceInstance = serviceInstance;
        serviceDiscoveries.values().forEach(serviceDiscovery -> serviceDiscovery.register(serviceInstance));
    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {
        serviceDiscoveries.values().forEach(serviceDiscovery -> serviceDiscovery.update(serviceInstance));
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        serviceDiscoveries.values().forEach(serviceDiscovery -> serviceDiscovery.unregister(serviceInstance));
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        MultiServiceInstancesChangedListener multiListener = new MultiServiceInstancesChangedListener(listener);

        for (String registryKey : serviceDiscoveries.keySet()) {
            SingleServiceInstancesChangedListener singleListener = new SingleServiceInstancesChangedListener(listener.getServiceNames(), serviceDiscoveries.get(registryKey), multiListener);
            multiListener.putSingleListener(registryKey, singleListener);
            serviceDiscoveries.get(registryKey).addServiceInstancesChangedListener(singleListener);
        }
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize, boolean healthyOnly) throws NullPointerException, IllegalArgumentException, UnsupportedOperationException {

        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for (ServiceDiscovery serviceDiscovery : serviceDiscoveries.values()) {
            Page<ServiceInstance> serviceInstancePage =  serviceDiscovery.getInstances(serviceName, offset, pageSize, healthyOnly);
            serviceInstanceList.addAll(serviceInstancePage.getData());
        }

        return new DefaultPage<>(offset, pageSize, serviceInstanceList, serviceInstanceList.size());
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
        return serviceInstance;
    }

    protected  static class MultiServiceInstancesChangedListener  implements ConditionalEventListener<ServiceInstancesChangedEvent> {
        private final ServiceInstancesChangedListener sourceListener;
        private final Map<String, SingleServiceInstancesChangedListener> singleListenerMap = new ConcurrentHashMap<>();

        public MultiServiceInstancesChangedListener(ServiceInstancesChangedListener sourceListener) {
            this.sourceListener = sourceListener;
        }

        @Override
        public boolean accept(ServiceInstancesChangedEvent event) {
            return sourceListener.getServiceNames().contains(event.getServiceName());
        }

        @Override
        public void onEvent(ServiceInstancesChangedEvent event) {
            List<ServiceInstance> serviceInstances = new ArrayList<>();
            for (SingleServiceInstancesChangedListener singleListener : singleListenerMap.values()) {
                if (null != singleListener.event) {
                    serviceInstances.addAll(singleListener.event.getServiceInstances());
                }
            }

            sourceListener.onEvent(new ServiceInstancesChangedEvent(event.getServiceName(), serviceInstances));
        }

        public void putSingleListener(String registryKey, SingleServiceInstancesChangedListener singleListener) {
            singleListenerMap.put(registryKey, singleListener);
        }
    }

    protected  static class SingleServiceInstancesChangedListener extends ServiceInstancesChangedListener {
        private final MultiServiceInstancesChangedListener multiListener;
        volatile  ServiceInstancesChangedEvent event;

        public SingleServiceInstancesChangedListener(Set<String> serviceNames, ServiceDiscovery serviceDiscovery, MultiServiceInstancesChangedListener multiListener) {
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