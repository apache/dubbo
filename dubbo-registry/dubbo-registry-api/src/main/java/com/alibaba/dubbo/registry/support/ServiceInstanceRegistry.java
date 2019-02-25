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
package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.alibaba.dubbo.common.Constants.CONFIGURATORS_CATEGORY;
import static com.alibaba.dubbo.common.Constants.CONSUMERS_CATEGORY;
import static com.alibaba.dubbo.common.Constants.PROVIDERS_CATEGORY;
import static com.alibaba.dubbo.common.Constants.ROUTERS_CATEGORY;
import static java.lang.Long.getLong;
import static java.lang.System.getProperty;

/**
 * {@link FailbackRegistry} extension that is used as the Oriented Service Instance registration, for
 *
 * @param <S> The actual type of service instance
 * @since 2.6.6
 */
public abstract class ServiceInstanceRegistry<S> extends FailbackRegistry {

    /**
     * All supported categories
     */
    private static final String[] ALL_SUPPORTED_CATEGORIES = of(
            PROVIDERS_CATEGORY,
            CONSUMERS_CATEGORY,
            ROUTERS_CATEGORY,
            CONFIGURATORS_CATEGORY
    );

    private static final int CATEGORY_INDEX = 0;

    private static final int SERVICE_INTERFACE_INDEX = CATEGORY_INDEX + 1;

    private static final int SERVICE_VERSION_INDEX = SERVICE_INTERFACE_INDEX + 1;

    private static final int SERVICE_GROUP_INDEX = SERVICE_VERSION_INDEX + 1;

    private static final String WILDCARD = "*";

    /**
     * The separator for service name
     */
    private static final String SERVICE_NAME_SEPARATOR = getProperty("dubbo.service.name.separator", ":");

    /**
     * The interval in second of lookup service names(only for Dubbo-OPS)
     */
    private static final long LOOKUP_INTERVAL = getLong("dubbo.service.names.lookup.interval", 30);

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * {@link ScheduledExecutorService} lookup service names(only for Dubbo-OPS)
     */
    private volatile ScheduledExecutorService serviceNamesScheduler;

    public ServiceInstanceRegistry(URL url) {
        super(url);
    }

    @Override
    protected final void doRegister(URL url) {
        String serviceName = getServiceName(url);
        Registration registration = createRegistration(serviceName, url);
        register(serviceName, toServiceInstance(registration), url);
    }

    @Override
    protected final void doUnregister(URL url) {
        String serviceName = getServiceName(url);
        Registration registration = createRegistration(serviceName, url);
        deregister(serviceName, toServiceInstance(registration), url);
    }

    @Override
    protected final void doSubscribe(URL url, NotifyListener listener) {
        Set<String> serviceNames = getServiceNames(url, listener);
        doSubscribe(url, listener, serviceNames);
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        if (isAdminProtocol(url)) {
            shutdownServiceNamesLookup();
        }
    }

    /**
     * Adapts {@link Registration} to an actual service instance
     *
     * @param registration {@link Registration}
     * @return
     */
    protected abstract S toServiceInstance(Registration registration);

    /**
     * Adapts {@link S} to an {@link Registration}
     *
     * @param serviceInstance {@link S}
     * @return an {@link Registration}
     */
    protected abstract Registration toRegistration(S serviceInstance);

    /**
     * Register a {@link S service instance}
     *
     * @param serviceName     the service name
     * @param serviceInstance {@link S service instance}
     * @param url             Dubbo's {@link URL}
     */
    protected abstract void register(String serviceName, S serviceInstance, URL url);

    /**
     * Deregister a {@link DubboRegistration Dubbol registration}
     *
     * @param serviceName     the service name
     * @param serviceInstance {@link S service instance}
     * @param url             Dubbo's {@link URL}
     */
    protected abstract void deregister(String serviceName, S serviceInstance, URL url);

    private void doSubscribe(final URL url, final NotifyListener listener, final Set<String> serviceNames) {
        Collection<S> serviceInstances = new LinkedList<S>();

        for (String serviceName : serviceNames) {
            serviceInstances.addAll(findServiceInstances(serviceName));
        }
        notifySubscriber(url, listener, serviceInstances);
    }

    /**
     * Notify the Healthy {@link DubboRegistration service instance} to subscriber.
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
        filter(serviceInstances, new Filter<S>() {
            @Override
            public boolean accept(S serviceInstance) {
                return filterHealthyRegistration(serviceInstance);
            }
        });
    }

    /**
     * Find the {@link Collection} of {@link S service instances} by the service name
     *
     * @param serviceName the service name
     * @return a {@link Collection} of {@link S service instances}
     */
    protected abstract Collection<S> findServiceInstances(String serviceName);

    /**
     * Filter Healthy the {@link S service instance}
     *
     * @param serviceInstance the {@link S service instance}
     * @return if healthy , return <code>true</code>
     */
    protected abstract boolean filterHealthyRegistration(S serviceInstance);

    private void shutdownServiceNamesLookup() {
        if (serviceNamesScheduler != null) {
            serviceNamesScheduler.shutdown();
        }
    }

    private void scheduleServiceNamesLookup(final URL url,
                                            final NotifyListener listener) {
        if (serviceNamesScheduler == null) {
            serviceNamesScheduler = Executors.newSingleThreadScheduledExecutor();
            serviceNamesScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    Set<String> serviceNames = findAllServiceNames();
                    filter(serviceNames, new Filter<String>() {
                        @Override
                        public boolean accept(String serviceName) {
                            boolean accepted = false;
                            for (String category : ALL_SUPPORTED_CATEGORIES) {
                                String prefix = category + SERVICE_NAME_SEPARATOR;
                                if (serviceName.startsWith(prefix)) {
                                    accepted = true;
                                    break;
                                }
                            }
                            return accepted;
                        }
                    });
                    doSubscribe(url, listener, serviceNames);
                }
            }, LOOKUP_INTERVAL, LOOKUP_INTERVAL, TimeUnit.SECONDS);
        }
    }

    /**
     * Find all service names
     *
     * @return all service names
     */
    protected abstract Set<String> findAllServiceNames();

    private List<URL> buildURLs(URL consumerURL, Collection<S> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            return Collections.emptyList();
        }
        List<URL> urls = new LinkedList<URL>();
        for (S serviceInstance : serviceInstances) {
            Registration registration = toRegistration(serviceInstance);
            URL url = buildURL(registration);
            if (UrlUtils.isMatch(consumerURL, url)) {
                urls.add(url);
            }
        }
        return urls;
    }

    private URL buildURL(Registration registration) {
        URL url = new URL(registration.getMetadata().get(Constants.PROTOCOL_KEY),
                registration.getIp(), registration.getPort(),
                registration.getMetadata());
        return url;
    }

    /**
     * Get the service names for Dubbo OPS
     *
     * @param url {@link URL}
     * @return non-null
     */
    private Set<String> getSubscribedServiceNamesForOps(URL url) {
        Set<String> serviceNames = findAllServiceNames();
        filterServiceNames(serviceNames, url);
        return serviceNames;
    }

    private <T> void filter(Collection<T> collection, Filter<T> filter) {
        Iterator<T> iterator = collection.iterator();
        while (iterator.hasNext()) {
            T data = iterator.next();
            if (!filter.accept(data)) { // remove if not accept
                iterator.remove();
            }
        }
    }

    private void filterServiceNames(Set<String> serviceNames, URL url) {

        final String[] categories = getCategories(url);

        final String targetServiceInterface = url.getServiceInterface();

        final String targetVersion = url.getParameter(Constants.VERSION_KEY);

        final String targetGroup = url.getParameter(Constants.GROUP_KEY);

        filter(serviceNames, new Filter<String>() {
            @Override
            public boolean accept(String serviceName) {
                // split service name to segments
                // (required) segments[0] = category
                // (required) segments[1] = serviceInterface
                // (required) segments[2] = version
                // (optional) segments[3] = group
                String[] segments = getServiceSegments(serviceName);
                int length = segments.length;
                if (length < 4) { // must present 4 segments or more
                    return false;
                }

                String category = getCategory(segments);
                if (Arrays.binarySearch(categories, category) > -1) { // no match category
                    return false;
                }

                String serviceInterface = getServiceInterface(segments);
                if (!WILDCARD.equals(targetServiceInterface)
                        && !StringUtils.isEquals(targetServiceInterface, serviceInterface)) { // no match interface
                    return false;
                }

                String version = getServiceVersion(segments);
                if (!WILDCARD.equals(targetVersion)
                        && !StringUtils.isEquals(targetVersion, version)) { // no match service
                    // version
                    return false;
                }

                String group = getServiceGroup(segments);
                if (group != null && !WILDCARD.equals(targetGroup)
                        && !StringUtils.isEquals(targetGroup, group)) { // no match service
                    // group
                    return false;
                }

                return true;
            }
        });
    }

    protected Registration createRegistration(String serviceName, URL url) {
        // Append default category if absent
        String category = url.getParameter(Constants.CATEGORY_KEY,
                Constants.DEFAULT_CATEGORY);
        URL newURL = url.addParameter(Constants.CATEGORY_KEY, category);
        newURL = newURL.addParameter(Constants.PROTOCOL_KEY, url.getProtocol());
        String ip = url.getHost();
        int port = url.getPort();
        DubboRegistration registration = new DubboRegistration();
        registration.setServiceName(serviceName);
        registration.setIp(ip);
        registration.setPort(port);
        registration.setMetadata(new LinkedHashMap<String, String>(newURL.getParameters()));

        return registration;
    }

    /**
     * Get the categories from {@link URL}
     *
     * @param url {@link URL}
     * @return non-null array
     */
    private String[] getCategories(URL url) {
        return Constants.ANY_VALUE.equals(url.getServiceInterface())
                ? ALL_SUPPORTED_CATEGORIES
                : of(Constants.DEFAULT_CATEGORY);
    }

    /**
     * A filter
     */
    private interface Filter<T> {

        /**
         * Tests whether or not the specified data should be accepted.
         *
         * @param data The data to be tested
         * @return <code>true</code> if and only if <code>data</code> should be accepted
         */
        boolean accept(T data);

    }

    /**
     * Get the subscribed service names from the specified {@link URL url}
     *
     * @param url      {@link URL}
     * @param listener {@link NotifyListener}
     * @return non-null
     */
    private Set<String> getServiceNames(URL url, NotifyListener listener) {
        if (isAdminProtocol(url)) {
            scheduleServiceNamesLookup(url, listener);
            return getSubscribedServiceNamesForOps(url);
        } else {
            return getServiceNames(url);
        }
    }

    private Set<String> getServiceNames(URL url) {
        String[] categories = getCategories(url);
        Set<String> serviceNames = new LinkedHashSet<String>(categories.length);
        for (String category : categories) {
            final String serviceName = getServiceName(url, category);
            serviceNames.add(serviceName);
        }
        return serviceNames;
    }

    private boolean isAdminProtocol(URL url) {
        return Constants.ADMIN_PROTOCOL.equals(url.getProtocol());
    }

    /**
     * Get the service name
     *
     * @param url {@link URL}
     * @return non-null
     */
    public static String getServiceName(URL url) {
        String category = url.getParameter(Constants.CATEGORY_KEY,
                Constants.DEFAULT_CATEGORY);
        return getServiceName(url, category);
    }

    private static String getServiceName(URL url, String category) {
        StringBuilder serviceNameBuilder = new StringBuilder(category);
        appendIfPresent(serviceNameBuilder, url, Constants.INTERFACE_KEY);
        appendIfPresent(serviceNameBuilder, url, Constants.VERSION_KEY);
        appendIfPresent(serviceNameBuilder, url, Constants.GROUP_KEY);
        return serviceNameBuilder.toString();
    }

    private static void appendIfPresent(StringBuilder target, URL url,
                                        String parameterName) {
        String parameterValue = url.getParameter(parameterName);
        appendIfPresent(target, parameterValue);
    }

    public static String[] getServiceSegments(String serviceName) {
        return serviceName.split(SERVICE_NAME_SEPARATOR);
    }

    public static String getCategory(String[] segments) {
        return segments[CATEGORY_INDEX];
    }

    public static String getServiceInterface(String[] segments) {
        return segments[SERVICE_INTERFACE_INDEX];
    }

    public static String getServiceVersion(String[] segments) {
        return segments[SERVICE_VERSION_INDEX];
    }

    public static String getServiceGroup(String[] segments) {
        return segments.length > 4 ? segments[SERVICE_GROUP_INDEX] : null;
    }

    private static <T> T[] of(T... values) {
        return values;
    }

    private static void appendIfPresent(StringBuilder target, String parameterValue) {
        if (StringUtils.isNotEmpty(parameterValue)) {
            target.append(SERVICE_NAME_SEPARATOR).append(parameterValue);
        }
    }
}
