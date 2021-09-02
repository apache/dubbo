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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.SubscribedURLsSynthesizer;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;
import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;
import static org.apache.dubbo.common.function.ThrowableAction.execute;
import static org.apache.dubbo.metadata.ServiceNameMapping.toStringKeys;
import static org.apache.dubbo.registry.client.ServiceDiscoveryFactory.getExtension;

/**
 * ServiceDiscoveryRegistry is a very special Registry implementation, which is used to bridge the old interface-level service discovery model
 * with the new service discovery model introduced in 3.0 in a compatible manner.
 *
 * It fully complies with the extension specification of the Registry SPI, but is different from the specific implementation of zookeeper and Nacos,
 * because it does not interact with any real third-party registry, but only with the relevant components of ServiceDiscovery in the process.
 * In short, it bridges the old interface model and the new service discovery model:
 *
 * - register() aggregates interface level data into MetadataInfo by mainly interacting with MetadataService.
 * - subscribe() triggers the whole subscribe process of the application level service discovery model.
 *   - Maps interface to applications depending on ServiceNameMapping.
 *   - Starts the new service discovery listener (InstanceListener) and makes NotifierListeners part of the InstanceListener.
 */
public class ServiceDiscoveryRegistry implements Registry {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceDiscovery serviceDiscovery;

    private final WritableMetadataService writableMetadataService;

    private final Set<String> registeredListeners = new LinkedHashSet<>();

    /* apps - listener */
    private final Map<String, ServiceInstancesChangedListener> serviceListeners = new ConcurrentHashMap<>();

    private URL registryURL;

    public ServiceDiscoveryRegistry(URL registryURL) {
        this.registryURL = registryURL;
        this.serviceDiscovery = createServiceDiscovery(registryURL);
        this.writableMetadataService = WritableMetadataService.getDefaultExtension();
    }

    // Currently for test purpose
    protected ServiceDiscoveryRegistry(URL registryURL, ServiceDiscovery serviceDiscovery, WritableMetadataService writableMetadataService) {
        this.registryURL = registryURL;
        this.serviceDiscovery = serviceDiscovery;
        this.writableMetadataService = writableMetadataService;
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
        ServiceDiscovery serviceDiscovery = getServiceDiscovery(registryURL);
        execute(() -> serviceDiscovery.initialize(registryURL.addParameter(INTERFACE_KEY, ServiceDiscovery.class.getName())
                .removeParameter(REGISTRY_TYPE_KEY)));
        return serviceDiscovery;
    }

    private List<SubscribedURLsSynthesizer> initSubscribedURLsSynthesizers() {
        ExtensionLoader<SubscribedURLsSynthesizer> loader = ExtensionLoader.getExtensionLoader(SubscribedURLsSynthesizer.class);
        return Collections.unmodifiableList(new ArrayList<>(loader.getSupportedExtensionInstances()));
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
                logger.debug(String.format("The URL[%s] should not be registered.", providerURL.toString()));
            }
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

    public void doRegister(URL url) {
        url = addRegistryClusterKey(url);
        if (writableMetadataService.exportURL(url)) {
            if (logger.isInfoEnabled()) {
                logger.info(format("The URL[%s] registered successfully.", url.toString()));
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn(format("The URL[%s] has been registered.", url.toString()));
            }
        }
    }

    @Override
    public final void unregister(URL url) {
        if (!shouldRegister(url)) {
            return;
        }
        doUnregister(url);
    }

    public void doUnregister(URL url) {
        url = addRegistryClusterKey(url);
        if (writableMetadataService.unexportURL(url)) {
            if (logger.isInfoEnabled()) {
                logger.info(format("The URL[%s] deregistered successfully.", url.toString()));
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.info(format("The URL[%s] has been deregistered.", url.toString()));
            }
        }
    }

    @Override
    public final void subscribe(URL url, NotifyListener listener) {
        if (!shouldSubscribe(url)) { // Should Not Subscribe
            return;
        }
        url = addRegistryClusterKey(url);
        doSubscribe(url, listener);
    }

    public void doSubscribe(URL url, NotifyListener listener) {
        writableMetadataService.subscribeURL(url);

        boolean check = url.getParameter(CHECK_KEY, false);

        Set<String> subscribedServices = Collections.emptySet();
        try {
            ServiceNameMapping serviceNameMapping = ServiceNameMapping.getDefaultExtension();
            subscribedServices = serviceNameMapping.getAndListenServices(registryURL, url, new DefaultMappingListener(url, subscribedServices, listener));
        } catch (Exception e) {
            logger.warn("Cannot find app mapping for service " + url.getServiceInterface() + ", will not migrate.", e);
        }

        if (CollectionUtils.isEmpty(subscribedServices)) {
            if (check) {
                throw new IllegalStateException("Should has at least one way to know which services this interface belongs to, subscription url: " + url);
            }
            return;
        }

        subscribeURLs(url, listener, subscribedServices);
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

    public void doUnsubscribe(URL url, NotifyListener listener) {
        // TODO: remove service name mapping listener
        writableMetadataService.unsubscribeURL(url);
        String protocolServiceKey = url.getServiceKey() + GROUP_CHAR_SEPARATOR + url.getParameter(PROTOCOL_KEY, DUBBO);
        Set<String> serviceNames = writableMetadataService.getCachedMapping(url);
        if (CollectionUtils.isNotEmpty(serviceNames)) {
            String serviceNamesKey = toStringKeys(serviceNames);
            ServiceInstancesChangedListener instancesChangedListener = serviceListeners.get(serviceNamesKey);
            if (instancesChangedListener != null) {
                instancesChangedListener.removeListener(protocolServiceKey);
                if (!instancesChangedListener.hasListeners()) {
                    serviceListeners.remove(serviceNamesKey);
                }
            }
        }
    }

    @Override
    public List<URL> lookup(URL url) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    @Override
    public boolean isAvailable() {
        return !serviceDiscovery.isDestroy() && !serviceDiscovery.getServices().isEmpty();
    }

    @Override
    public void destroy() {
        AbstractRegistryFactory.removeDestroyedRegistry(this);
        execute(() -> {
            // stop ServiceDiscovery
            serviceDiscovery.destroy();
        });
    }

    protected void subscribeURLs(URL url, NotifyListener listener, Set<String> serviceNames) {
        serviceNames = new TreeSet<>(serviceNames);
        String serviceNamesKey = toStringKeys(serviceNames);
        String protocolServiceKey = url.getServiceKey() + GROUP_CHAR_SEPARATOR + url.getParameter(PROTOCOL_KEY, DUBBO);

        // register ServiceInstancesChangedListener
        boolean serviceListenerRegistered = true;
        ServiceInstancesChangedListener serviceInstancesChangedListener;
        synchronized (this) {
            serviceInstancesChangedListener = serviceListeners.get(serviceNamesKey);
            if (serviceInstancesChangedListener == null) {
                serviceInstancesChangedListener = serviceDiscovery.createListener(serviceNames);
                serviceInstancesChangedListener.setUrl(url);
                for (String serviceName : serviceNames) {
                    List<ServiceInstance> serviceInstances = serviceDiscovery.getInstances(serviceName);
                    if (CollectionUtils.isNotEmpty(serviceInstances)) {
                        serviceInstancesChangedListener.onEvent(new ServiceInstancesChangedEvent(serviceName, serviceInstances));
                    }
                }
                serviceListenerRegistered = false;
                serviceListeners.put(serviceNamesKey, serviceInstancesChangedListener);
            }
        }

        serviceInstancesChangedListener.setUrl(url);
        listener.addServiceListener(serviceInstancesChangedListener);
        serviceInstancesChangedListener.addListenerAndNotify(protocolServiceKey, listener);
        if (!serviceListenerRegistered) {
            serviceDiscovery.addServiceInstancesChangedListener(serviceInstancesChangedListener);
        }
    }

    /**
     * Create an instance of {@link ServiceDiscoveryRegistry} if supported
     *
     * @param registryURL the {@link URL url} of registry
     * @return <code>null</code> if not supported
     */
    public static ServiceDiscoveryRegistry create(URL registryURL) {
        return supports(registryURL) ? new ServiceDiscoveryRegistry(registryURL) : null;
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

    private static boolean isCompatibleProtocol(String protocol, URL targetURL) {
        return protocol == null || Objects.equals(protocol, targetURL.getParameter(PROTOCOL_KEY))
                || Objects.equals(protocol, targetURL.getProtocol());
    }

    public Map<String, ServiceInstancesChangedListener> getServiceListeners() {
        return serviceListeners;
    }

    private class DefaultMappingListener implements MappingListener {
        private final Logger logger = LoggerFactory.getLogger(DefaultMappingListener.class);
        private URL url;
        private Set<String> oldApps;
        private NotifyListener listener;

        public DefaultMappingListener(URL subscribedURL, Set<String> serviceNames, NotifyListener listener) {
            this.url = subscribedURL;
            this.oldApps = serviceNames;
            this.listener = listener;
        }

        @Override
        public void onEvent(MappingChangedEvent event) {
            if (logger.isDebugEnabled()) {
                logger.debug("Received mapping notification from meta server, " + event);
            }
            Set<String> newApps = event.getApps();
            Set<String> tempOldApps = oldApps;
            oldApps = newApps;

            if (CollectionUtils.isEmpty(newApps)) {
                return;
            }

            if (CollectionUtils.isEmpty(tempOldApps) && newApps.size() > 0) {
                WritableMetadataService.getDefaultExtension().putCachedMapping(ServiceNameMapping.buildMappingKey(url), newApps);
                subscribeURLs(url, listener, newApps);
                return;
            }

            for (String newAppName : newApps) {
                if (!tempOldApps.contains(newAppName)) {
                    WritableMetadataService.getDefaultExtension().putCachedMapping(ServiceNameMapping.buildMappingKey(url), newApps);
                    subscribeURLs(url, listener, newApps);
                    return;
                }
            }
        }
    }
}
