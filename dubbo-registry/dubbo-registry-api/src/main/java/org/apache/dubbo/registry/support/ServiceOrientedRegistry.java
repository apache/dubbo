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
import org.apache.dubbo.metadata.LocalMetadataService;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.MetadataServiceProxyFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.common.utils.CollectionUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.StringUtils.split;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getProtocolPort;

/**
 * Service-Oriented {@link Registry} that is dislike the traditional {@link Registry} will not communicate to
 * registry immediately instead of persisting into the metadata's repository when the Dubbo service exports.
 * The metadata repository will be used as the data source of Dubbo Metadata service that is about to export and be
 * subscribed by the consumers.
 * <p>
 *
 * @see ServiceDiscovery
 * @see FailbackRegistry
 * @since 2.7.3
 */
public class ServiceOrientedRegistry extends FailbackRegistry {

    /**
     * The parameter value of {@link ServiceDiscovery}'s type
     */
    public static final String TYPE_PARAM_VALUE = "service";

    /**
     * The parameter name of the subscribed service names
     */
    public static final String SUBSCRIBED_SERVICE_NAMES_PARAM_NAME = "subscribed-services";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceDiscovery serviceDiscovery;

    private final Set<String> subscribedServices;

    private final ServiceNameMapping serviceNameMapping;

    private final LocalMetadataService localMetadataService;

    private final MetadataServiceProxyFactory metadataServiceProxyFactory;


    public ServiceOrientedRegistry(URL url) {
        super(url);
        this.serviceDiscovery = buildServiceDiscovery(url);
        this.subscribedServices = buildSubscribedServices(url);
        this.serviceNameMapping = ServiceNameMapping.getDefaultExtension();
        this.localMetadataService = LocalMetadataService.getDefaultExtension();
        this.metadataServiceProxyFactory = new MetadataServiceProxyFactory();
    }

    private Set<String> buildSubscribedServices(URL url) {
        String[] subscribedServices = split(url.getParameter(SUBSCRIBED_SERVICE_NAMES_PARAM_NAME), ',');
        return unmodifiableSet(
                of(subscribedServices)
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

    protected boolean shouldRegister(URL url) {
        String side = url.getParameter(SIDE_KEY);

        boolean should = PROVIDER_SIDE.equals(side); // Only register the Provider.

        if (!should) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("The URL[%s] should not be registered.", url.toString()));
            }
        }

        return should;
    }

    @Override
    public void doRegister(URL url) {
        if (!shouldRegister(url)) {
            return;
        }
        if (localMetadataService.exportURL(url)) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("The URL[%s] registered successfully.", url.toString()));
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.info(String.format("The URL[%s] has been registered.", url.toString()));
            }
        }
    }

    @Override
    public void doUnregister(URL url) {
        if (!shouldRegister(url)) {
            return;
        }
        if (localMetadataService.unexportURL(url)) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("The URL[%s] deregistered successfully.", url.toString()));
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.info(String.format("The URL[%s] has been deregistered.", url.toString()));
            }
        }
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        subscribeURLs(url, listener);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        localMetadataService.unsubscribeURL(url);
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

        Set<String> serviceNames = getServices(url);

        serviceNames.forEach(serviceName -> subscribeURLs(url, listener, serviceName));

        localMetadataService.subscribeURL(url);
    }

    protected void subscribeURLs(URL url, NotifyListener listener, String serviceName) {

        List<ServiceInstance> serviceInstances = serviceDiscovery.getInstances(serviceName);

        subscribeURLs(url, listener, serviceName, serviceInstances);

        // Add Listener
        serviceDiscovery.addServiceInstancesChangedListener(serviceName, event -> {
            subscribeURLs(url, listener, event.getServiceName(), new ArrayList<>(event.getServiceInstances()));
        });
    }

    protected void subscribeURLs(URL url, NotifyListener listener, String serviceName,
                                 Collection<ServiceInstance> serviceInstances) {

        if (isEmpty(serviceInstances)) {
            logger.warn(String.format("There is no instance in service[name : %s]", serviceName));
            return;
        }

        List<URL> subscribedURLs = getSubscribedURLs(url, serviceInstances);

        listener.notify(subscribedURLs);
    }


    private List<URL> getSubscribedURLs(URL url, Collection<ServiceInstance> instances) {

        List<URL> subscribedURLs = new LinkedList<>();

        List<ServiceInstance> serviceInstances = new ArrayList<>(instances);

        Iterator<ServiceInstance> iterator = serviceInstances.iterator();

        List<URL> templateURLs = null;

        // try to get the exported URLs from every instance until it's successful.

        while (iterator.hasNext()) {

            ServiceInstance serviceInstance = iterator.next();

            templateURLs = getSubscribedURLs(url, serviceInstance);
            if (isNotEmpty(templateURLs)) {
                // templateURLs as  the first result should be added into subscribedURLs
                subscribedURLs.addAll(templateURLs);
                break;
            }
        }

        // If templateURLs is not empty, duplicate it multiple times with different hosts and ports

        if (isNotEmpty(templateURLs)) {

            templateURLs.forEach(templateURL -> {

                String protocol = templateURL.getProtocol();

                while (iterator.hasNext()) {
                    ServiceInstance serviceInstance = iterator.next();

                    String host = serviceInstance.getHost();
                    Integer port = getProtocolPort(serviceInstance, protocol);

                    if (port == null) {
                        if (logger.isWarnEnabled()) {
                            logger.warn(String.format("The protocol[%s] port of Dubbo  service instance[host : %s] " +
                                    "can't be resolved", protocol, host));
                        }
                        continue;
                    }

                    URL subscribedURL = new URL(protocol, host, port, templateURL.getParameters());
                    subscribedURLs.add(subscribedURL);
                }
            });
        }


        return subscribedURLs;
    }

    private List<URL> getSubscribedURLs(URL url, ServiceInstance serviceInstance) {
        String serviceInterface = url.getServiceInterface();
        String group = url.getParameter(GROUP_KEY);
        String version = url.getParameter(VERSION_KEY);
        // The subscribed protocol may be null
        String protocol = url.getParameter(PROTOCOL_KEY);

        List<URL> exportedURLs = emptyList();

        try {
            MetadataService metadataService = metadataServiceProxyFactory.createProxy(serviceInstance);
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
        String protocol = subscribedURL.getProtocol();
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
        return TYPE_PARAM_VALUE.equalsIgnoreCase(registryURL.getParameter(TYPE_PARAM_NAME));
    }
}
