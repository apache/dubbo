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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.metadata.AbstractServiceNameMapping;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;
import static org.apache.dubbo.common.function.ThrowableAction.execute;
import static org.apache.dubbo.common.utils.CollectionUtils.toTreeSet;
import static org.apache.dubbo.metadata.ServiceNameMapping.toStringKeys;
import static org.apache.dubbo.registry.client.ServiceDiscoveryFactory.getExtension;

/**
 * TODO, this bridge implementation is not necessary now, protocol can interact with service discovery directly.
 * <p>
 * ServiceDiscoveryRegistry is a very special Registry implementation, which is used to bridge the old interface-level service discovery model
 * with the new service discovery model introduced in 3.0 in a compatible manner.
 * <p>
 * It fully complies with the extension specification of the Registry SPI, but is different from the specific implementation of zookeeper and Nacos,
 * because it does not interact with any real third-party registry, but only with the relevant components of ServiceDiscovery in the process.
 * In short, it bridges the old interface model and the new service discovery model:
 * <p>
 * - register() aggregates interface level data into MetadataInfo by mainly interacting with MetadataService.
 * - subscribe() triggers the whole subscribe process of the application level service discovery model.
 * - Maps interface to applications depending on ServiceNameMapping.
 * - Starts the new service discovery listener (InstanceListener) and makes NotifierListeners part of the InstanceListener.
 */
public class ServiceDiscoveryRegistry extends FailbackRegistry {

    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final ServiceDiscovery serviceDiscovery;

    private final AbstractServiceNameMapping serviceNameMapping;

    /* apps - listener */
    private final Map<String, ServiceInstancesChangedListener> serviceListeners = new ConcurrentHashMap<>();
    private final Map<String, MappingListener> mappingListeners = new ConcurrentHashMap<>();
    /* This lock has the same scope and lifecycle as its corresponding instance listener.
    It's used to make sure that only one interface mapping to the same app list can do subscribe or unsubscribe at the same moment.
    And the lock should be destroyed when listener destroying its corresponding instance listener.
    * */
    private final ConcurrentMap<String, Lock> appSubscriptionLocks = new ConcurrentHashMap<>();

    public ServiceDiscoveryRegistry(URL registryURL, ApplicationModel applicationModel) {
        super(registryURL);
        this.serviceDiscovery = createServiceDiscovery(registryURL);
        this.serviceNameMapping = (AbstractServiceNameMapping) ServiceNameMapping.getDefaultExtension(registryURL.getScopeModel());
        super.applicationModel = applicationModel;
    }

    // Currently, for test purpose
    protected ServiceDiscoveryRegistry(URL registryURL, ServiceDiscovery serviceDiscovery, ServiceNameMapping serviceNameMapping) {
        super(registryURL);
        this.serviceDiscovery = serviceDiscovery;
        this.serviceNameMapping = (AbstractServiceNameMapping) serviceNameMapping;
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    /**
     * Create the {@link ServiceDiscovery} from the registry {@link URL}
     *
     * @param registryURL the {@link URL} to connect the registry
     * @return non-null
     */
    protected ServiceDiscovery createServiceDiscovery(URL registryURL) {
        return getServiceDiscovery(registryURL.addParameter(INTERFACE_KEY, ServiceDiscovery.class.getName())
            .removeParameter(REGISTRY_TYPE_KEY));
    }

    /**
     * Get the instance {@link ServiceDiscovery} from the registry {@link URL} using
     * {@link ServiceDiscoveryFactory} SPI
     *
     * @param registryURL the {@link URL} to connect the registry
     * @return
     */
    private ServiceDiscovery getServiceDiscovery(URL registryURL) {
        ServiceDiscoveryFactory factory = getExtension(registryURL);
        return factory.getServiceDiscovery(registryURL);
    }

    protected boolean shouldRegister(URL providerURL) {

        String side = providerURL.getSide();

        boolean should = PROVIDER_SIDE.equals(side); // Only register the Provider.

        if (!should) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("The URL[%s] should not be registered.", providerURL));
            }
        }

        if (!acceptable(providerURL)) {
            logger.info("URL " + providerURL + " will not be registered to Registry. Registry " + this.getUrl() + " does not accept service of this protocol type.");
            return false;
        }

        return should;
    }

    protected boolean shouldSubscribe(URL subscribedURL) {
        return !shouldRegister(subscribedURL);
    }

    @Override
    public final void register(URL url) {
        if (!shouldRegister(url)) { // Should Not Register
            return;
        }
        doRegister(url);
    }

    @Override
    public void doRegister(URL url) {
        // fixme, add registry-cluster is not necessary anymore
        url = addRegistryClusterKey(url);
        serviceDiscovery.register(url);
    }

    @Override
    public final void unregister(URL url) {
        if (!shouldRegister(url)) {
            return;
        }
        doUnregister(url);
    }

    @Override
    public void doUnregister(URL url) {
        // fixme, add registry-cluster is not necessary anymore
        url = addRegistryClusterKey(url);
        serviceDiscovery.unregister(url);
    }

    @Override
    public final void subscribe(URL url, NotifyListener listener) {
        if (!shouldSubscribe(url)) { // Should Not Subscribe
            return;
        }
        doSubscribe(url, listener);
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        url = addRegistryClusterKey(url);

        serviceDiscovery.subscribe(url, listener);

        Set<String> mappingByUrl = ServiceNameMapping.getMappingByUrl(url);

        String key = ServiceNameMapping.buildMappingKey(url);


        if (mappingByUrl == null) {
            Lock mappingLock = serviceNameMapping.getMappingLock(key);
            try {
                mappingLock.lock();
                mappingByUrl = serviceNameMapping.getMapping(url);
                try {
                    MappingListener mappingListener = new DefaultMappingListener(url, mappingByUrl, listener);
                    mappingByUrl = serviceNameMapping.getAndListen(this.getUrl(), url, mappingListener);
                    mappingListeners.put(url.getProtocolServiceKey(), mappingListener);
                } catch (Exception e) {
                    logger.warn(INTERNAL_ERROR, "", "", "Cannot find app mapping for service " + url.getServiceInterface() + ", will not migrate.", e);
                }

                if (CollectionUtils.isEmpty(mappingByUrl)) {
                    logger.info("No interface-apps mapping found in local cache, stop subscribing, will automatically wait for mapping listener callback: " + url);
//                if (check) {
//                    throw new IllegalStateException("Should has at least one way to know which services this interface belongs to, subscription url: " + url);
//                }
                    return;
                }
            } finally {
                mappingLock.unlock();
            }
        }
        subscribeURLs(url, listener, mappingByUrl);
    }

    @Override
    public final void unsubscribe(URL url, NotifyListener listener) {
        if (!shouldSubscribe(url)) { // Should Not Subscribe
            return;
        }
        url = addRegistryClusterKey(url);
        doUnsubscribe(url, listener);
    }

    private URL addRegistryClusterKey(URL url) {
        String registryCluster = serviceDiscovery.getUrl().getParameter(REGISTRY_CLUSTER_KEY);
        if (registryCluster != null && url.getParameter(REGISTRY_CLUSTER_KEY) == null) {
            url = url.addParameter(REGISTRY_CLUSTER_KEY, registryCluster);
        }
        return url;
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        // TODO: remove service name mapping listener
        serviceDiscovery.unsubscribe(url, listener);
        String protocolServiceKey = url.getProtocolServiceKey();
        Set<String> serviceNames = serviceNameMapping.getMapping(url);
        serviceNameMapping.stopListen(url, mappingListeners.remove(protocolServiceKey));
        if (CollectionUtils.isNotEmpty(serviceNames)) {
            String serviceNamesKey = toStringKeys(serviceNames);
            Lock appSubscriptionLock = getAppSubscription(serviceNamesKey);
            try {
                appSubscriptionLock.lock();
                ServiceInstancesChangedListener instancesChangedListener = serviceListeners.get(serviceNamesKey);
                if (instancesChangedListener != null) {
                    instancesChangedListener.removeListener(protocolServiceKey, listener);
                    if (!instancesChangedListener.hasListeners()) {
                        instancesChangedListener.destroy();
                        serviceListeners.remove(serviceNamesKey);
                        removeAppSubscriptionLock(serviceNamesKey);
                    }
                }
            } finally {
                appSubscriptionLock.unlock();
            }
        }
    }

    @Override
    public List<URL> lookup(URL url) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public boolean isAvailable() {
        if (serviceDiscovery instanceof NopServiceDiscovery) {
            // NopServiceDiscovery is designed for compatibility, check availability is meaningless, just return true
            return true;
        }
        return !serviceDiscovery.isDestroy() && !serviceDiscovery.getServices().isEmpty();
    }

    @Override
    public void destroy() {
        registryManager.removeDestroyedRegistry(this);
        // stop ServiceDiscovery
        execute(serviceDiscovery::destroy);
        // destroy all event listener
        for (ServiceInstancesChangedListener listener : serviceListeners.values()) {
            listener.destroy();
        }
        appSubscriptionLocks.clear();
        serviceListeners.clear();
        mappingListeners.clear();
    }

    @Override
    public boolean isServiceDiscovery() {
        return true;
    }

    protected void subscribeURLs(URL url, NotifyListener listener, Set<String> serviceNames) {
        serviceNames = toTreeSet(serviceNames);
        String serviceNamesKey = toStringKeys(serviceNames);
        String serviceKey = url.getServiceKey();
        logger.info(String.format("Trying to subscribe from apps %s for service key %s, ", serviceNamesKey, serviceKey));

        // register ServiceInstancesChangedListener
        Lock appSubscriptionLock = getAppSubscription(serviceNamesKey);
        try {
            appSubscriptionLock.lock();
            ServiceInstancesChangedListener serviceInstancesChangedListener = serviceListeners.get(serviceNamesKey);
            if (serviceInstancesChangedListener == null) {
                serviceInstancesChangedListener = serviceDiscovery.createListener(serviceNames);
                for (String serviceName : serviceNames) {
                    List<ServiceInstance> serviceInstances = serviceDiscovery.getInstances(serviceName);
                    if (CollectionUtils.isNotEmpty(serviceInstances)) {
                        serviceInstancesChangedListener.onEvent(new ServiceInstancesChangedEvent(serviceName, serviceInstances));
                    }
                }
                serviceListeners.put(serviceNamesKey, serviceInstancesChangedListener);
            }

            if (!serviceInstancesChangedListener.isDestroyed()) {
                listener.addServiceListener(serviceInstancesChangedListener);
                serviceInstancesChangedListener.addListenerAndNotify(url, listener);
                ServiceInstancesChangedListener finalServiceInstancesChangedListener = serviceInstancesChangedListener;

                MetricsEventBus.post(RegistryEvent.toSsEvent(url.getApplicationModel(), serviceKey),
                    () -> {
                        serviceDiscovery.addServiceInstancesChangedListener(finalServiceInstancesChangedListener);
                        return null;
                    }
                );
            } else {
                logger.info(String.format("Listener of %s has been destroyed by another thread.", serviceNamesKey));
                serviceListeners.remove(serviceNamesKey);
            }
        } finally {
            appSubscriptionLock.unlock();
        }
    }

    /**
     * Supports or not ?
     *
     * @param registryURL the {@link URL url} of registry
     * @return if supported, return <code>true</code>, or <code>false</code>
     */
    public static boolean supports(URL registryURL) {
        return SERVICE_REGISTRY_TYPE.equalsIgnoreCase(registryURL.getParameter(REGISTRY_TYPE_KEY));
    }

    public Map<String, ServiceInstancesChangedListener> getServiceListeners() {
        return serviceListeners;
    }

    private class DefaultMappingListener implements MappingListener {
        private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DefaultMappingListener.class);
        private final URL url;
        private Set<String> oldApps;
        private NotifyListener listener;
        private volatile boolean stopped;

        public DefaultMappingListener(URL subscribedURL, Set<String> serviceNames, NotifyListener listener) {
            this.url = subscribedURL;
            this.oldApps = serviceNames;
            this.listener = listener;
        }

        @Override
        public synchronized void onEvent(MappingChangedEvent event) {
            logger.info("Received mapping notification from meta server, " + event);

            if (stopped) {
                logger.warn(INTERNAL_ERROR, "", "", "Listener has been stopped, ignore mapping notification, check why listener is not removed.");
                return;
            }
            Set<String> newApps = event.getApps();
            Set<String> tempOldApps = oldApps;

            if (CollectionUtils.isEmpty(newApps) || CollectionUtils.equals(newApps, tempOldApps)) {
                return;
            }

            logger.info("Mapping of service " + event.getServiceKey() + "changed from " + tempOldApps + " to " + newApps);

            Lock mappingLock = serviceNameMapping.getMappingLock(event.getServiceKey());
            try {
                mappingLock.lock();
                if (CollectionUtils.isEmpty(tempOldApps) && newApps.size() > 0) {
                    serviceNameMapping.putCachedMapping(ServiceNameMapping.buildMappingKey(url), newApps);
                    subscribeURLs(url, listener, newApps);
                    oldApps = newApps;
                    return;
                }

                for (String newAppName : newApps) {
                    if (!tempOldApps.contains(newAppName)) {
                        serviceNameMapping.removeCachedMapping(ServiceNameMapping.buildMappingKey(url));
                        serviceNameMapping.putCachedMapping(ServiceNameMapping.buildMappingKey(url), newApps);
                        // old instance listener related to old app list that needs to be destroyed after subscribe refresh.
                        ServiceInstancesChangedListener oldListener = listener.getServiceListener();
                        if (oldListener != null) {
                            String appKey = toStringKeys(toTreeSet(tempOldApps));
                            Lock appSubscriptionLock = getAppSubscription(appKey);
                            try {
                                appSubscriptionLock.lock();
                                oldListener.removeListener(url.getServiceKey(), listener);
                                if (!oldListener.hasListeners()) {
                                    oldListener.destroy();
                                    removeAppSubscriptionLock(appKey);
                                }
                            } finally {
                                appSubscriptionLock.unlock();
                            }
                        }

                        subscribeURLs(url, listener, newApps);
                        oldApps = newApps;
                        return;
                    }
                }
            } finally {
                mappingLock.unlock();
            }
        }

        @Override
        public void stop() {
            stopped = true;
        }
    }

    public Lock getAppSubscription(String key) {
        return ConcurrentHashMapUtils.computeIfAbsent(appSubscriptionLocks, key, _k -> new ReentrantLock());
    }

    public void removeAppSubscriptionLock(String key) {
        Lock lock = appSubscriptionLocks.get(key);
        if (lock != null) {
            try {
                lock.lock();
                appSubscriptionLocks.remove(key);
            } finally {
                lock.unlock();
            }
        }
    }
}
