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
package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.proxy.MetadataServiceProxyFactory;
import org.apache.dubbo.registry.client.selector.ServiceInstanceSelector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_DEFAULT;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.common.utils.CollectionUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.metadata.report.support.Constants.METADATA_REPORT_KEY;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getMetadataServiceURLsParams;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getProviderHost;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getProviderPort;

/**
 * Service-Oriented {@link Registry} that is dislike the traditional {@link Registry} will not communicate to
 * registry immediately instead of persisting into the metadata's repository when the Dubbo service exports.
 * The metadata repository will be used as the data source of Dubbo Metadata service that is about to export and be
 * subscribed by the consumers.
 * <p>
 *
 * @see ServiceDiscovery
 * @see FailbackRegistry
 * @since 2.7.4
 */
public class ServiceOrientedRegistry extends FailbackRegistry {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceDiscovery serviceDiscovery;

    private final Set<String> subscribedServices;

    private final ServiceNameMapping serviceNameMapping;

    private final WritableMetadataService writableMetadataService;

    private final MetadataServiceProxyFactory metadataServiceProxyFactory;

    public ServiceOrientedRegistry(URL registryURL) {
        super(registryURL);
        this.serviceDiscovery = buildServiceDiscovery(registryURL);
        this.subscribedServices = buildSubscribedServices(registryURL);
        this.serviceNameMapping = ServiceNameMapping.getDefaultExtension();

        String metadata = registryURL.getParameter(METADATA_REPORT_KEY, METADATA_DEFAULT);
        this.writableMetadataService = WritableMetadataService.getExtension(metadata);
        this.metadataServiceProxyFactory = MetadataServiceProxyFactory.getExtension(metadata);
    }

    private Set<String> buildSubscribedServices(URL url) {
        String subscribedServiceNames = url.getParameter(SUBSCRIBED_SERVICE_NAMES_KEY);
        return isBlank(subscribedServiceNames) ? emptySet() :
                unmodifiableSet(of(subscribedServiceNames.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotEmpty)
                        .collect(toSet()));
    }

    private ServiceDiscovery buildServiceDiscovery(URL url) {
        ServiceDiscoveryFactory serviceDiscoveryFactory = ServiceDiscoveryFactory.getDefaultExtension();
        ServiceDiscovery serviceDiscovery = serviceDiscoveryFactory.create(url);
        serviceDiscovery.start();
        return serviceDiscovery;
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
    public void doRegister(URL url) {
        if (!shouldRegister(url)) { // Should Not Register
            return;
        }
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
    public void doUnregister(URL url) {
        if (!shouldRegister(url)) {
            return;
        }
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
    public void doSubscribe(URL url, NotifyListener listener) {
        if (!shouldSubscribe(url)) { // Should Not Subscribe
            return;
        }
        subscribeURLs(url, listener);
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
        // stop ServiceDiscovery
        serviceDiscovery.stop();
    }

    protected void subscribeURLs(URL url, NotifyListener listener) {

        writableMetadataService.subscribeURL(url);

        Set<String> serviceNames = getServices(url);

        serviceNames.forEach(serviceName -> subscribeURLs(url, listener, serviceName));

    }

    protected void subscribeURLs(URL url, NotifyListener listener, String serviceName) {

        List<ServiceInstance> serviceInstances = serviceDiscovery.getInstances(serviceName);

        subscribeURLs(url, listener, serviceName, serviceInstances);

        // Add Listener
        serviceDiscovery.addServiceInstancesChangedListener(serviceName, event -> {
            subscribeURLs(url, listener, event.getServiceName(), new ArrayList<>(event.getServiceInstances()));
        });
    }

    protected void subscribeURLs(URL subscribedURL, NotifyListener listener, String serviceName,
                                 Collection<ServiceInstance> serviceInstances) {

        if (isEmpty(serviceInstances)) {
            logger.warn(format("There is no instance in service[name : %s]", serviceName));
            return;
        }

        List<URL> subscribedURLs = getSubscribedURLs(subscribedURL, serviceInstances);

        listener.notify(subscribedURLs);
    }

    private List<URL> getSubscribedURLs(URL subscribedURL, Collection<ServiceInstance> instances) {

        List<URL> subscribedURLs = new LinkedList<>();

        // local service instances could be mutable
        List<ServiceInstance> serviceInstances = instances.stream()
                .filter(ServiceInstance::isEnabled)
                .filter(ServiceInstance::isHealthy)
                .collect(Collectors.toList());

        /**
         * A caches all revisions of exported services in different {@link ServiceInstance}s
         * associating with the {@link URL urls}
         */
        Map<String, List<URL>> revisionURLsCache = new HashMap<>();

        // try to get the exported URLs from every instance until it's successful.
        for (int i = 0; i < serviceInstances.size(); i++) {
            // select a instance of {@link ServiceInstance}
            ServiceInstance selectedInstance = selectServiceInstance(serviceInstances);
            List<URL> templateURLs = getTemplateURLs(subscribedURL, selectedInstance, revisionURLsCache);
            if (isNotEmpty(templateURLs)) {
                // add templateURLs into subscribedURLs
                subscribedURLs.addAll(templateURLs);
                // remove the selected ServiceInstance in this time, it remains N - 1 elements.
                serviceInstances.remove(selectedInstance);
                break;
            }
        }

        // Clone the subscribed URLs from the template URLs
        List<URL> clonedURLs = cloneSubscribedURLs(subscribedURL, serviceInstances, revisionURLsCache);
        // Add all cloned URLs into subscribedURLs
        subscribedURLs.addAll(clonedURLs);
        // clear all revisions
        revisionURLsCache.clear();
        // clear local service instances
        serviceInstances.clear();

        return subscribedURLs;
    }

    private List<URL> cloneSubscribedURLs(URL subscribedURL, Collection<ServiceInstance> serviceInstances,
                                          Map<String, List<URL>> revisionURLsCache) {

        // If revisionURLsCache is not empty, clone the template URLs to be the subscribed URLs
        if (!revisionURLsCache.isEmpty()) {

            List<URL> clonedURLs = new LinkedList<>();

            Iterator<ServiceInstance> iterator = serviceInstances.iterator();

            while (iterator.hasNext()) {

                ServiceInstance serviceInstance = iterator.next();

                List<URL> templateURLs = getTemplateURLs(subscribedURL, serviceInstance, revisionURLsCache);
                // The parameters of URLs that the MetadataService exported
                Map<String, Map<String, Object>> serviceURLsParams = getMetadataServiceURLsParams(serviceInstance);

                templateURLs.forEach(templateURL -> {

                    String protocol = templateURL.getProtocol();

                    Map<String, Object> serviceURLParams = serviceURLsParams.get(protocol);

                    String host = getProviderHost(serviceURLParams);

                    Integer port = getProviderPort(serviceURLParams);

                    /**
                     * clone the subscribed {@link URL urls} based on the template {@link URL url}
                     */
                    URL newSubscribedURL = new URL(protocol, host, port, templateURL.getParameters());
                    clonedURLs.add(newSubscribedURL);
                });
            }

            return clonedURLs;
        }

        return Collections.emptyList();
    }


    /**
     * Select one {@link ServiceInstance} from the {@link List list}
     *
     * @param serviceInstances the {@link List list} of {@link ServiceInstance}
     * @return <code>null</code> if <code>serviceInstances</code> is empty.
     */
    private ServiceInstance selectServiceInstance(List<ServiceInstance> serviceInstances) {
        ServiceInstanceSelector selector = getExtensionLoader(ServiceInstanceSelector.class).getAdaptiveExtension();
        return selector.select(getUrl(), serviceInstances);
    }


    /**
     * Get the template {@link URL urls} from the specified {@link ServiceInstance}.
     * <p>
     * Typically, the revisions of all {@link ServiceInstance instances} in one service are same. However,
     * if one service is upgrading one or more Dubbo service interfaces, one of them may have the multiple declarations
     * is deploying in the different {@link ServiceInstance service instances}, thus, it has to compare the interface
     * contract one by one, the "revision" that is the number is introduced to identify all Dubbo exported interfaces in
     * one {@link ServiceInstance service instance}.
     * <p>
     * First, put the revision {@link ServiceInstance service instance}
     * associating {@link #getProviderExportedURLs(URL, ServiceInstance) exported URLs} into cache.
     * <p>
     * And then compare a new {@link ServiceInstance service instances'} revision with cached one,If they are equal,
     * return the cached template {@link URL urls} immediately, or to get template {@link URL urls} that the provider
     * {@link ServiceInstance instance} exported via executing {@link #getProviderExportedURLs(URL, ServiceInstance)}
     * method.
     * <p>
     * Eventually, the retrieving result will be cached and returned.
     *
     * @param subscribedURL     the subscribed {@link URL url}
     * @param selectedInstance  the {@link ServiceInstance}
     * @param revisionURLsCache A caches all revisions of exported services in different {@link ServiceInstance}s
     *                          associating with the {@link URL urls}
     * @return non-null {@link List} of {@link URL urls}
     */
    protected List<URL> getTemplateURLs(URL subscribedURL, ServiceInstance selectedInstance,
                                        Map<String, List<URL>> revisionURLsCache) {
        // get the revision from the specified {@link ServiceInstance}
        String revision = getExportedServicesRevision(selectedInstance);
        // try to get templateURLs from cache
        List<URL> templateURLs = revisionURLsCache.get(revision);

        if (isEmpty(templateURLs)) { // not exists or getting failed last time

            if (!revisionURLsCache.isEmpty()) { // it's not first time
                if (logger.isWarnEnabled()) {
                    logger.warn(format("The ServiceInstance[id: %s, host : %s , port : %s] has different revision : %s" +
                                    ", please make sure the service [name : %s] is changing or not.",
                            selectedInstance.getId(),
                            selectedInstance.getHost(),
                            selectedInstance.getPort(),
                            revision,
                            selectedInstance.getServiceName()
                    ));
                }
            }
            // get or get again
            templateURLs = getProviderExportedURLs(subscribedURL, selectedInstance);
            // put into cache
            revisionURLsCache.put(revision, templateURLs);
        }

        return templateURLs;
    }

    /**
     * Get the exported {@link URL urls} from the specified provider {@link ServiceInstance instance}
     *
     * @param subscribedURL    the subscribed {@link URL url}
     * @param providerInstance the target provider {@link ServiceInstance instance}
     * @return non-null {@link List} of {@link URL urls}
     */
    protected List<URL> getProviderExportedURLs(URL subscribedURL, ServiceInstance providerInstance) {

        List<URL> exportedURLs = emptyList();

        String serviceInterface = subscribedURL.getServiceInterface();
        String group = subscribedURL.getParameter(GROUP_KEY);
        String version = subscribedURL.getParameter(VERSION_KEY);
        // The subscribed protocol may be null
        String protocol = subscribedURL.getParameter(PROTOCOL_KEY);

        try {
            MetadataService metadataService = metadataServiceProxyFactory.getProxy(providerInstance);
            List<String> urls = metadataService.getExportedURLs(serviceInterface, group, version, protocol);
            exportedURLs = urls.stream().map(URL::valueOf).collect(Collectors.toList());
        } catch (Throwable e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }

        return exportedURLs;
    }


    protected Set<String> getServices(URL subscribedURL) {
        Set<String> serviceNames = getSubscribedServices();
        if (isEmpty(serviceNames)) {
            serviceNames = findMappedServices(subscribedURL);
        }
        return serviceNames;
    }

    /**
     * Get the subscribed service names
     *
     * @return non-null
     */
    public Set<String> getSubscribedServices() {
        return subscribedServices;
    }

    /**
     * Get the mapped services name by the specified {@link URL}
     *
     * @param subscribedURL
     * @return
     */
    protected Set<String> findMappedServices(URL subscribedURL) {
        String serviceInterface = subscribedURL.getServiceInterface();
        String group = subscribedURL.getParameter(GROUP_KEY);
        String version = subscribedURL.getParameter(VERSION_KEY);
        String protocol = subscribedURL.getParameter(PROTOCOL_KEY, DUBBO_PROTOCOL);
        return serviceNameMapping.get(serviceInterface, group, version, protocol);
    }

    /**
     * Create an instance of {@link ServiceOrientedRegistry} if supported
     *
     * @param registryURL the {@link URL url} of registry
     * @return <code>null</code> if not supported
     */
    public static ServiceOrientedRegistry create(URL registryURL) {
        return supports(registryURL) ? new ServiceOrientedRegistry(registryURL) : null;
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

    /**
     * Get the instance of {@link ServiceDiscovery}
     *
     * @return non-null
     */
    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }
}
