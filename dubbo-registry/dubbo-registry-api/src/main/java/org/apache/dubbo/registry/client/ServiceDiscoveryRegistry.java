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
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.store.InMemoryWritableMetadataService;
import org.apache.dubbo.metadata.store.RemoteWritableMetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.proxy.MetadataServiceProxyFactory;
import org.apache.dubbo.registry.client.selector.ServiceInstanceSelector;
import org.apache.dubbo.registry.support.FailbackRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.URLBuilder.from;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPERATOR;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_PROTOCOL_DEFAULT;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.function.ThrowableAction.execute;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmptyMap;
import static org.apache.dubbo.common.utils.CollectionUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.metadata.WritableMetadataService.DEFAULT_EXTENSION;
import static org.apache.dubbo.registry.client.ServiceDiscoveryFactory.getExtension;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getMetadataStorageType;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getProtocolPort;

/**
 * {@link ServiceDiscoveryRegistry} is the service-oriented {@link Registry} and dislike the traditional one that
 * {@link #register(URL) registers} to and {@link #subscribe(URL, NotifyListener) discoveries}
 * the Dubbo's {@link URL urls} from the external registry. In the {@link #register(URL) registration}
 * phase, The {@link URL urls} of Dubbo services will be {@link WritableMetadataService#exportURL(URL) exported} into
 * {@link WritableMetadataService} that is either {@link InMemoryWritableMetadataService in-memory} or
 * {@link RemoteWritableMetadataService remote},
 * <p>
 * it's decided by metadata
 * subscribes from the remote proxy of {@link MetadataService}
 *
 * <p>
 *
 * @see ServiceDiscovery
 * @see FailbackRegistry
 * @since 2.7.4
 */
public class ServiceDiscoveryRegistry extends FailbackRegistry {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceDiscovery serviceDiscovery;

    private final Map<String, String> subscribedServices;

    private final ServiceNameMapping serviceNameMapping;

    private final WritableMetadataService writableMetadataService;

    private final Set<String> registeredListeners = new LinkedHashSet<>();

    /**
     * A cache for all URLs of services that the subscribed services exported
     * The key is the service name
     * The value is a nested {@link Map} whose key is the revision and value is all URLs of services
     */
    private final Map<String, Map<String, List<URL>>> serviceExportedURLsCache = new LinkedHashMap<>();

    public ServiceDiscoveryRegistry(URL registryURL) {
        super(registryURL);
        this.serviceDiscovery = createServiceDiscovery(registryURL);
        this.subscribedServices = getSubscribedServices(registryURL);
        this.serviceNameMapping = ServiceNameMapping.getDefaultExtension();
        String metadataStorageType = getMetadataStorageType(registryURL);
        this.writableMetadataService = WritableMetadataService.getExtension(metadataStorageType);
    }

    /**
     * Get the subscribed services from the specified registry {@link URL url}
     *
     * @param registryURL the specified registry {@link URL url}
     * @return non-null
     */
    public static Map<String, String> getSubscribedServices(URL registryURL) {
        Map<String, String> services = new HashMap<>();
        String subscribedServiceNames = registryURL.getParameter(SUBSCRIBED_SERVICE_NAMES_KEY);
        if (isBlank(subscribedServiceNames)) {
            return services;
        } else {
            of(COMMA_SPLIT_PATTERN.split(subscribedServiceNames))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .forEach(serviceProtocol -> {
                        String[] arr = serviceProtocol.split(GROUP_CHAR_SEPERATOR);
                        if (arr.length > 1) {
                            services.put(arr[0], arr[1]);
                        } else {
                            services.put(arr[0], SUBSCRIBED_PROTOCOL_DEFAULT);
                        }
                    });
        }
        return services;
    }

    /**
     * Create the {@link ServiceDiscovery} from the registry {@link URL}
     *
     * @param registryURL the {@link URL} to connect the registry
     * @return non-null
     */
    protected ServiceDiscovery createServiceDiscovery(URL registryURL) {
        ServiceDiscovery originalServiceDiscovery = getServiceDiscovery(registryURL);
        ServiceDiscovery serviceDiscovery = enhanceEventPublishing(originalServiceDiscovery);
        execute(() -> {
            serviceDiscovery.initialize(registryURL.addParameter(INTERFACE_KEY, ServiceDiscovery.class.getName())
                    .removeParameter(REGISTRY_TYPE_KEY));
        });
        return serviceDiscovery;
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

    /**
     * Enhance the original {@link ServiceDiscovery} with event publishing feature
     *
     * @param original the original {@link ServiceDiscovery}
     * @return {@link EventPublishingServiceDiscovery} instance
     */
    private ServiceDiscovery enhanceEventPublishing(ServiceDiscovery original) {
        return new EventPublishingServiceDiscovery(original);
    }

    protected boolean shouldRegister(URL providerURL) {

        String side = providerURL.getParameter(SIDE_KEY);

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
        super.register(url);
    }

    @Override
    public void doRegister(URL url) {
        if (writableMetadataService.exportURL(url)) {
            if (logger.isInfoEnabled()) {
                logger.info(format("The URL[%s] registered successfully.", url.toString()));
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.info(format("The URL[%s] has been registered.", url.toString()));
            }
        }
    }

    @Override
    public final void unregister(URL url) {
        if (!shouldRegister(url)) {
            return;
        }
        super.unregister(url);
    }

    @Override
    public void doUnregister(URL url) {
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
        super.subscribe(url, listener);
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        subscribeURLs(url, listener);
    }

    @Override
    public final void unsubscribe(URL url, NotifyListener listener) {
        if (!shouldSubscribe(url)) { // Should Not Subscribe
            return;
        }
        super.unsubscribe(url, listener);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        writableMetadataService.unsubscribeURL(url);
    }

    @Override
    public boolean isAvailable() {
        return !serviceDiscovery.getServices().isEmpty();
    }

    @Override
    public void destroy() {
        super.destroy();
        execute(() -> {
            // stop ServiceDiscovery
            serviceDiscovery.destroy();
        });
    }

    protected void subscribeURLs(URL url, NotifyListener listener) {

        writableMetadataService.subscribeURL(url);

        Map<String, String> services = getServices(url);

        services.forEach((name, proto) -> subscribeURLs(url, listener, name));

    }

    protected void subscribeURLs(URL url, NotifyListener listener, String serviceName) {

        List<ServiceInstance> serviceInstances = serviceDiscovery.getInstances(serviceName);

        subscribeURLs(url, listener, serviceName, serviceInstances);

        // register ServiceInstancesChangedListener
        registerServiceInstancesChangedListener(url, new ServiceInstancesChangedListener(serviceName, subscribedServices.get(serviceName)) {

            @Override
            public void onEvent(ServiceInstancesChangedEvent event) {
                subscribeURLs(url, listener, event.getServiceName(), new ArrayList<>(event.getServiceInstances()));
            }
        });
    }

    /**
     * Register the {@link ServiceInstancesChangedListener} If absent
     *
     * @param url      {@link URL}
     * @param listener the {@link ServiceInstancesChangedListener}
     */
    private void registerServiceInstancesChangedListener(URL url, ServiceInstancesChangedListener listener) {
        String listenerId = createListenerId(url, listener);
        if (registeredListeners.add(listenerId)) {
            serviceDiscovery.addServiceInstancesChangedListener(listener);
        }
    }

    private String createListenerId(URL url, ServiceInstancesChangedListener listener) {
        return listener.getServiceName() + ":" + url.toString(VERSION_KEY, GROUP_KEY, PROTOCOL_KEY);
    }

    protected void subscribeURLs(URL subscribedURL, NotifyListener listener, String serviceName,
                                 Collection<ServiceInstance> serviceInstances) {

        if (isEmpty(serviceInstances)) {
            logger.warn(format("There is no instance in service[name : %s]", serviceName));
            return;
        }

        List<URL> subscribedURLs = getSubscribedURLs(subscribedURL, serviceInstances, serviceName);

        listener.notify(subscribedURLs);
    }

    private List<URL> getSubscribedURLs(URL subscribedURL, Collection<ServiceInstance> instances, String serviceName) {

        // local service instances could be mutable
        List<ServiceInstance> serviceInstances = instances.stream()
                .filter(ServiceInstance::isEnabled)
                .filter(ServiceInstance::isHealthy)
                .collect(Collectors.toList());

        int size = serviceInstances.size();

        if (size == 0) {
            return emptyList();
        }

        expungeStaleRevisionExportedURLs(serviceInstances);

        initTemplateURLs(subscribedURL, serviceInstances);

        // Clone the subscribed URLs from the template URLs
        List<URL> subscribedURLs = cloneSubscribedURLs(subscribedURL, serviceInstances);
        // clear local service instances
        serviceInstances.clear();
        return subscribedURLs;
    }

    private void initTemplateURLs(URL subscribedURL, List<ServiceInstance> serviceInstances) {
        // Try to get the template URLs until success
        for (int i = 0; i < serviceInstances.size(); i++) {
            // select a instance of {@link ServiceInstance}
            ServiceInstance selectedInstance = selectServiceInstance(serviceInstances);
            // try to get the template URLs
            List<URL> templateURLs = getTemplateURLs(subscribedURL, selectedInstance);
            if (isNotEmpty(templateURLs)) { // If the result is valid
                break; // break the loop
            } else {
                serviceInstances.remove(selectedInstance); // remove if the service instance is not available
                // There may be one or more service instances from the "serviceInstances"
            }
        }
    }

    private void expungeStaleRevisionExportedURLs(List<ServiceInstance> serviceInstances) {

        if (isEmpty(serviceInstances)) { // if empty, return immediately
            return;
        }

        String serviceName = serviceInstances.get(0).getServiceName();

        synchronized (this) {

            // revisionExportedURLs is mutable
            Map<String, List<URL>> revisionExportedURLs = serviceExportedURLsCache.computeIfAbsent(serviceName, s -> new HashMap<>());

            if (revisionExportedURLs.isEmpty()) { // if empty, return immediately
                return;
            }

            Set<String> existedRevisions = revisionExportedURLs.keySet(); // read-only
            Set<String> currentRevisions = serviceInstances.stream()
                    .map(ServiceInstanceMetadataUtils::getExportedServicesRevision)
                    .collect(Collectors.toSet());
            // staleRevisions = existedRevisions(copy) - currentRevisions
            Set<String> staleRevisions = new HashSet<>(existedRevisions);
            staleRevisions.removeAll(currentRevisions);
            // remove exported URLs if staled
            staleRevisions.forEach(revisionExportedURLs::remove);
        }
    }

    private List<URL> cloneSubscribedURLs(URL subscribedURL, Collection<ServiceInstance> serviceInstances) {

        if (isEmpty(serviceInstances)) {
            return emptyList();
        }

        List<URL> clonedURLs = new LinkedList<>();

        serviceInstances.forEach(serviceInstance -> {

            String host = serviceInstance.getHost();

            getTemplateURLs(subscribedURL, serviceInstance)
                    .stream()
                    .map(templateURL -> templateURL.removeParameter(TIMESTAMP_KEY))
                    .map(templateURL -> templateURL.removeParameter(PID_KEY))
                    .map(templateURL -> {
                        String protocol = templateURL.getProtocol();
                        int port = getProtocolPort(serviceInstance, protocol);
                        if (Objects.equals(templateURL.getHost(), host)
                                && Objects.equals(templateURL.getPort(), port)) { // use templateURL if equals
                            return templateURL;
                        }

                        URLBuilder clonedURLBuilder = from(templateURL) // remove the parameters from the template URL
                                .setHost(host)  // reset the host
                                .setPort(port); // reset the port

                        return clonedURLBuilder.build();
                    })
                    .forEach(clonedURLs::add);
        });
        return clonedURLs;
    }


    /**
     * Select one {@link ServiceInstance} from the {@link List list}
     *
     * @param serviceInstances the {@link List list} of {@link ServiceInstance}
     * @return <code>null</code> if <code>serviceInstances</code> is empty.
     */
    private ServiceInstance selectServiceInstance(List<ServiceInstance> serviceInstances) {
        int size = serviceInstances.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return serviceInstances.get(0);
        }
        ServiceInstanceSelector selector = getExtensionLoader(ServiceInstanceSelector.class).getAdaptiveExtension();
        return selector.select(getUrl(), serviceInstances);
    }

    /**
     * Get the template {@link URL urls} from the specified {@link ServiceInstance}.
     * <p>
     * First, put the revision {@link ServiceInstance service instance}
     * associating {@link #getExportedURLs(URL, ServiceInstance) exported URLs} into cache.
     * <p>
     * And then compare a new {@link ServiceInstance service instances'} revision with cached one,If they are equal,
     * return the cached template {@link URL urls} immediately, or to get template {@link URL urls} that the provider
     * {@link ServiceInstance instance} exported via executing {@link #getExportedURLs(URL, ServiceInstance)}
     * method.
     * <p>
     * Eventually, the retrieving result will be cached and returned.
     *
     * @param subscribedURL    the subscribed {@link URL url}
     * @param selectedInstance the {@link ServiceInstance}
     *                         associating with the {@link URL urls}
     * @return non-null {@link List} of {@link URL urls}
     */
    protected List<URL> getTemplateURLs(URL subscribedURL, ServiceInstance selectedInstance) {

        List<URL> exportedURLs = getRevisionExportedURLs(selectedInstance);

        if (isEmpty(exportedURLs)) {
            return emptyList();
        }

        return filterSubscribedURLs(subscribedURL, exportedURLs);
    }

    private List<URL> filterSubscribedURLs(URL subscribedURL, List<URL> exportedURLs) {
        return exportedURLs.stream()
                .filter(url -> isSameServiceInterface(subscribedURL, url))
                .filter(url -> isSameParameter(subscribedURL, url, VERSION_KEY))
                .filter(url -> isSameParameter(subscribedURL, url, GROUP_KEY))
                .filter(url -> isCompatibleProtocol(subscribedURL, url))
                .collect(Collectors.toList());
    }

    private boolean isSameServiceInterface(URL one, URL another) {
        return Objects.equals(one.getServiceInterface(), another.getServiceInterface());
    }

    private boolean isSameParameter(URL one, URL another, String key) {
        return Objects.equals(one.getParameter(key), another.getParameter(key));
    }

    private boolean isCompatibleProtocol(URL one, URL another) {
        String protocol = one.getParameter(PROTOCOL_KEY);
        return isCompatibleProtocol(protocol, another);
    }

    private boolean isCompatibleProtocol(String protocol, URL targetURL) {
        return protocol == null || Objects.equals(protocol, targetURL.getParameter(PROTOCOL_KEY))
                || Objects.equals(protocol, targetURL.getProtocol());
    }

    /**
     * Get all services {@link URL URLs} that the specified {@link ServiceInstance service instance} exported with cache
     * <p>
     * Typically, the revisions of all {@link ServiceInstance instances} in one service are same. However,
     * if one service is upgrading one or more Dubbo service interfaces, one of them may have the multiple declarations
     * is deploying in the different {@link ServiceInstance service instances}, thus, it has to compare the interface
     * contract one by one, the "revision" that is the number is introduced to identify all Dubbo exported interfaces in
     * one {@link ServiceInstance service instance}.
     *
     * @param providerServiceInstance the {@link ServiceInstance} provides the Dubbo Services
     * @return the same as {@link #getExportedURLs(ServiceInstance)}
     */
    private List<URL> getRevisionExportedURLs(ServiceInstance providerServiceInstance) {

        if (providerServiceInstance == null) {
            return emptyList();
        }

        String serviceName = providerServiceInstance.getServiceName();
        // get the revision from the specified {@link ServiceInstance}
        String revision = getExportedServicesRevision(providerServiceInstance);

        List<URL> exportedURLs = null;

        synchronized (this) { // It's required to lock here because it may run in the sync or async mode

            Map<String, List<URL>> exportedURLsMap = serviceExportedURLsCache.computeIfAbsent(serviceName, s -> new LinkedHashMap());

            exportedURLs = exportedURLsMap.get(revision);

            boolean firstGet = false;

            if (exportedURLs == null) { // The hit is missing in cache

                if (!exportedURLsMap.isEmpty()) { // The case is that current ServiceInstance with the different revision
                    if (logger.isWarnEnabled()) {
                        logger.warn(format("The ServiceInstance[id: %s, host : %s , port : %s] has different revision : %s" +
                                        ", please make sure the service [name : %s] is changing or not.",
                                providerServiceInstance.getId(),
                                providerServiceInstance.getHost(),
                                providerServiceInstance.getPort(),
                                revision,
                                providerServiceInstance.getServiceName()
                        ));
                    }
                } else {// Else, it's the first time to get the exported URLs
                    firstGet = true;
                }
                exportedURLs = getExportedURLs(providerServiceInstance);

                if (exportedURLs != null) { // just allow the valid result into exportedURLsMap

                    exportedURLsMap.put(revision, exportedURLs);

                    if (logger.isDebugEnabled()) {
                        logger.debug(format("Getting the exported URLs[size : %s, first : %s] from the target service " +
                                        "instance [id: %s , service : %s , host : %s , port : %s , revision : %s]",
                                exportedURLs.size(), firstGet,
                                providerServiceInstance.getId(),
                                providerServiceInstance.getServiceName(),
                                providerServiceInstance.getHost(),
                                providerServiceInstance.getPort(),
                                revision
                        ));
                    }
                }
            }
        }

        // Get a copy from source in order to prevent the caller trying to change the cached data
        return exportedURLs != null ? new ArrayList<>(exportedURLs) : null;
    }

    /**
     * Get all services {@link URL URLs} that the specified {@link ServiceInstance service instance} exported
     * from {@link MetadataService} proxy
     *
     * @param providerServiceInstance the {@link ServiceInstance} provides the Dubbo Services
     * @return The possible result :
     * <ol>
     * <li>The normal result</li>
     * <li>The empty result if the {@link ServiceInstance service instance} did not export yet</li>
     * <li><code>null</code> if there is an invocation error on {@link MetadataService} proxy</li>
     * </ol>
     */
    private List<URL> getExportedURLs(ServiceInstance providerServiceInstance) {

        List<URL> exportedURLs = null;

        String metadataStorageType = getMetadataStorageType(providerServiceInstance);

        try {
            MetadataService metadataService = MetadataServiceProxyFactory
                    .getExtension(metadataStorageType == null ? DEFAULT_EXTENSION : metadataStorageType)
                    .getProxy(providerServiceInstance);
            SortedSet<String> urls = metadataService.getExportedURLs();
            exportedURLs = urls.stream().map(URL::valueOf).collect(Collectors.toList());
        } catch (Throwable e) {
            if (logger.isErrorEnabled()) {
                logger.error(format("It's failed to get the exported URLs from the target service instance[%s]",
                        providerServiceInstance), e);
            }
            exportedURLs = null; // set the result to be null if failed to get
        }
        return exportedURLs;
    }

    /**
     * Get the exported {@link URL urls} from the specified provider {@link ServiceInstance instance}
     *
     * @param subscribedURL           the subscribed {@link URL url}
     * @param providerServiceInstance the target provider {@link ServiceInstance instance}
     * @return non-null {@link List} of {@link URL urls}
     */
    protected List<URL> getExportedURLs(URL subscribedURL, ServiceInstance providerServiceInstance) {

        List<URL> exportedURLs = emptyList();

        String serviceInterface = subscribedURL.getServiceInterface();
        String group = subscribedURL.getParameter(GROUP_KEY);
        String version = subscribedURL.getParameter(VERSION_KEY);
        // The subscribed protocol may be null
        String protocol = subscribedURL.getParameter(PROTOCOL_KEY);
        String metadataStorageType = getMetadataStorageType(providerServiceInstance);

        try {
            MetadataService metadataService = MetadataServiceProxyFactory
                    .getExtension(metadataStorageType == null ? DEFAULT_EXTENSION : metadataStorageType)
                    .getProxy(providerServiceInstance);
            SortedSet<String> urls = metadataService.getExportedURLs(serviceInterface, group, version, protocol);
            exportedURLs = urls.stream().map(URL::valueOf).collect(Collectors.toList());
        } catch (Throwable e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
        return exportedURLs;
    }


    protected Map<String, String> getServices(URL subscribedURL) {
        Map<String, String> services = getSubscribedServices();
        if (isEmptyMap(services)) {
            services = findMappedServices(subscribedURL);
        }
        return services;
    }

    /**
     * Get the subscribed service names
     *
     * @return non-null
     */
    public Map<String, String> getSubscribedServices() {
        return subscribedServices;
    }

    /**
     * Get the mapped services name by the specified {@link URL}
     *
     * Only native Dubbo services rely on this mapping.
     *
     * @param subscribedURL
     * @return
     */
    protected Map<String, String> findMappedServices(URL subscribedURL) {
        String serviceInterface = subscribedURL.getServiceInterface();
        String group = subscribedURL.getParameter(GROUP_KEY);
        String version = subscribedURL.getParameter(VERSION_KEY);
        String protocol = subscribedURL.getParameter(PROTOCOL_KEY, DUBBO_PROTOCOL);

        Map<String, String> services = new LinkedHashMap<>();
        serviceNameMapping.get(serviceInterface, group, version, protocol).forEach(s -> {
            services.put(s, protocol);
        });
        return services;
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
}