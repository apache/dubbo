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
package org.apache.dubbo.registry.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.FailbackRegistry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.registry.Constants.ADMIN_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTERS_CATEGORY;

/**
 * Nacos {@link Registry}
 *
 * @see #SERVICE_NAME_SEPARATOR
 * @see #PAGINATION_SIZE
 * @see #LOOKUP_INTERVAL
 * @since 2.6.5
 */
public class NacosRegistry extends FailbackRegistry {

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

    private static final int SERVICE_INTERFACE_INDEX = 1;

    private static final int SERVICE_VERSION_INDEX = 2;

    private static final int SERVICE_GROUP_INDEX = 3;

    private static final String WILDCARD = "*";

    /**
     * The separator for service name
     * Change a constant to be configurable, it's designed for Windows file name that is compatible with old
     * Nacos binary release(< 0.6.1)
     */
    private static final String SERVICE_NAME_SEPARATOR = System.getProperty("nacos.service.name.separator", ":");

    /**
     * The pagination size of query for Nacos service names(only for Dubbo-OPS)
     */
    private static final int PAGINATION_SIZE = Integer.getInteger("nacos.service.names.pagination.size", 100);

    /**
     * The interval in second of lookup Nacos service names(only for Dubbo-OPS)
     */
    private static final long LOOKUP_INTERVAL = Long.getLong("nacos.service.names.lookup.interval", 30);

    /**
     * {@link ScheduledExecutorService} lookup Nacos service names(only for Dubbo-OPS)
     */
    private volatile ScheduledExecutorService scheduledExecutorService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final NamingService namingService;

    private final ConcurrentMap<String, EventListener> nacosListeners;

    public NacosRegistry(URL url, NamingService namingService) {
        super(url);
        this.namingService = namingService;
        this.nacosListeners = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isAvailable() {
        return "UP".equals(namingService.getServerStatus());
    }

    @Override
    public List<URL> lookup(final URL url) {
        final List<URL> urls = new LinkedList<>();
        execute(namingService -> {
            List<String> serviceNames = getServiceNames(url, null);
            for (String serviceName : serviceNames) {
                List<Instance> instances = namingService.getAllInstances(serviceName);
                urls.addAll(buildURLs(url, instances));
            }
        });
        return urls;
    }

    @Override
    public void doRegister(URL url) {
        final String serviceName = getServiceName(url);
        final Instance instance = createInstance(url);
        execute(namingService -> namingService.registerInstance(serviceName, instance));
    }

    @Override
    public void doUnregister(final URL url) {
        execute(namingService -> {
            String serviceName = getServiceName(url);
            Instance instance = createInstance(url);
            namingService.deregisterInstance(serviceName, instance.getIp(), instance.getPort());
        });
    }

    @Override
    public void doSubscribe(final URL url, final NotifyListener listener) {
        List<String> serviceNames = getServiceNames(url, listener);
        doSubscribe(url, listener, serviceNames);
    }

    private void doSubscribe(final URL url, final NotifyListener listener, final List<String> serviceNames) {
        execute(namingService -> {
            for (String serviceName : serviceNames) {
                List<Instance> instances = namingService.getAllInstances(serviceName);
                notifySubscriber(url, listener, instances);
                subscribeEventListener(serviceName, url, listener);
            }
        });
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        if (isAdminProtocol(url)) {
            shutdownServiceNamesLookup();
        }
    }

    private void shutdownServiceNamesLookup() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
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
            scheduleServiceNamesLookup(url, listener);
            return getServiceNamesForOps(url);
        } else {
            return doGetServiceNames(url);
        }
    }

    private boolean isAdminProtocol(URL url) {
        return ADMIN_PROTOCOL.equals(url.getProtocol());
    }

    private void scheduleServiceNamesLookup(final URL url, final NotifyListener listener) {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                List<String> serviceNames = getAllServiceNames();
                filterData(serviceNames, serviceName -> {
                    boolean accepted = false;
                    for (String category : ALL_SUPPORTED_CATEGORIES) {
                        String prefix = category + SERVICE_NAME_SEPARATOR;
                        if (StringUtils.startsWith(serviceName, prefix)) {
                            accepted = true;
                            break;
                        }
                    }
                    return accepted;
                });
                doSubscribe(url, listener, serviceNames);
            }, LOOKUP_INTERVAL, LOOKUP_INTERVAL, TimeUnit.SECONDS);
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
        filterServiceNames(serviceNames, url);
        return serviceNames;
    }

    private List<String> getAllServiceNames() {

        final List<String> serviceNames = new LinkedList<>();

        execute(namingService -> {

            int pageIndex = 1;
            ListView<String> listView = namingService.getServicesOfServer(pageIndex, PAGINATION_SIZE);
            // First page data
            List<String> firstPageData = listView.getData();
            // Append first page into list
            serviceNames.addAll(firstPageData);
            // the total count
            int count = listView.getCount();
            // the number of pages
            int pageNumbers = count / PAGINATION_SIZE;
            int remainder = count % PAGINATION_SIZE;
            // remain
            if (remainder > 0) {
                pageNumbers += 1;
            }
            // If more than 1 page
            while (pageIndex < pageNumbers) {
                listView = namingService.getServicesOfServer(++pageIndex, PAGINATION_SIZE);
                serviceNames.addAll(listView.getData());
            }

        });

        return serviceNames;
    }

    private void filterServiceNames(List<String> serviceNames, URL url) {

        final String[] categories = getCategories(url);

        final String targetServiceInterface = url.getServiceInterface();

        final String targetVersion = url.getParameter(VERSION_KEY);

        final String targetGroup = url.getParameter(GROUP_KEY);

        filterData(serviceNames, serviceName -> {
            // split service name to segments
            // (required) segments[0] = category
            // (required) segments[1] = serviceInterface
            // (required) segments[2] = version
            // (optional) segments[3] = group
            String[] segments = StringUtils.split(serviceName, SERVICE_NAME_SEPARATOR);
            int length = segments.length;
            if (length < 3) { // must present 3 segments or more
                return false;
            }

            String category = segments[CATEGORY_INDEX];
            if (!ArrayUtils.contains(categories, category)) { // no match category
                return false;
            }

            String serviceInterface = segments[SERVICE_INTERFACE_INDEX];
            if (!WILDCARD.equals(targetServiceInterface) &&
                    !StringUtils.equals(targetServiceInterface, serviceInterface)) { // no match service interface
                return false;
            }

            String version = segments[SERVICE_VERSION_INDEX];
            if (!WILDCARD.equals(targetVersion) &&
                    !StringUtils.equals(targetVersion, version)) { // no match service version
                return false;
            }

            String group = length > 3 ? segments[SERVICE_GROUP_INDEX] : null;
            // no match service group
            return group == null || WILDCARD.equals(targetGroup)
                    || StringUtils.equals(targetGroup, group);
        });
    }

    private <T> void filterData(Collection<T> collection, NacosDataFilter<T> filter) {
        // remove if not accept
        collection.removeIf(data -> !filter.accept(data));
    }

    private List<String> doGetServiceNames(URL url) {
        String[] categories = getCategories(url);
        List<String> serviceNames = new ArrayList<>(categories.length);
        for (String category : categories) {
            final String serviceName = getServiceName(url, category);
            serviceNames.add(serviceName);
        }
        return serviceNames;
    }

    private List<URL> buildURLs(URL consumerURL, Collection<Instance> instances) {
        if (instances.isEmpty()) {
            return Collections.emptyList();
        }
        List<URL> urls = new LinkedList<>();
        for (Instance instance : instances) {
            URL url = buildURL(instance);
            if (UrlUtils.isMatch(consumerURL, url)) {
                urls.add(url);
            }
        }
        return urls;
    }

    private void subscribeEventListener(String serviceName, final URL url, final NotifyListener listener)
            throws NacosException {
        if (!nacosListeners.containsKey(serviceName)) {
            EventListener eventListener = event -> {
                if (event instanceof NamingEvent) {
                    NamingEvent e = (NamingEvent) event;
                    notifySubscriber(url, listener, e.getInstances());
                }
            };
            namingService.subscribe(serviceName, eventListener);
            nacosListeners.put(serviceName, eventListener);
        }
    }

    /**
     * Notify the Healthy {@link Instance instances} to subscriber.
     *
     * @param url       {@link URL}
     * @param listener  {@link NotifyListener}
     * @param instances all {@link Instance instances}
     */
    private void notifySubscriber(URL url, NotifyListener listener, Collection<Instance> instances) {
        List<Instance> healthyInstances = new LinkedList<>(instances);
        // Healthy Instances
        filterHealthyInstances(healthyInstances);
        List<URL> urls = buildURLs(url, healthyInstances);
        NacosRegistry.this.notify(url, listener, urls);
    }

    /**
     * Get the categories from {@link URL}
     *
     * @param url {@link URL}
     * @return non-null array
     */
    private String[] getCategories(URL url) {
        return ANY_VALUE.equals(url.getServiceInterface()) ?
                ALL_SUPPORTED_CATEGORIES : of(DEFAULT_CATEGORY);
    }

    private URL buildURL(Instance instance) {
        Map<String, String> metadata = instance.getMetadata();
        String protocol = metadata.get(PROTOCOL_KEY);
        String path = metadata.get(PATH_KEY);
        return new URL(protocol,
                instance.getIp(),
                instance.getPort(),
                path,
                instance.getMetadata());
    }

    private Instance createInstance(URL url) {
        // Append default category if absent
        String category = url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
        URL newURL = url.addParameter(CATEGORY_KEY, category);
        newURL = newURL.addParameter(PROTOCOL_KEY, url.getProtocol());
        newURL = newURL.addParameter(PATH_KEY, url.getPath());
        String ip = url.getHost();
        int port = url.getPort();
        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(port);
        instance.setMetadata(new HashMap<>(newURL.getParameters()));
        return instance;
    }

    private String getServiceName(URL url) {
        String category = url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
        return getServiceName(url, category);
    }

    private String getServiceName(URL url, String category) {
        StringBuilder serviceNameBuilder = new StringBuilder(category);
        appendIfPresent(serviceNameBuilder, url, INTERFACE_KEY);
        appendIfPresent(serviceNameBuilder, url, VERSION_KEY);
        appendIfPresent(serviceNameBuilder, url, GROUP_KEY);
        return serviceNameBuilder.toString();
    }

    private void appendIfPresent(StringBuilder target, URL url, String parameterName) {
        String parameterValue = url.getParameter(parameterName);
        if (!StringUtils.isBlank(parameterValue)) {
            target.append(SERVICE_NAME_SEPARATOR).append(parameterValue);
        }
    }

    private void execute(NamingServiceCallback callback) {
        try {
            callback.callback(namingService);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
        }
    }

    private void filterHealthyInstances(Collection<Instance> instances) {
        filterData(instances, Instance::isEnabled);
    }

    @SafeVarargs
    private static <T> T[] of(T... values) {
        return values;
    }


    /**
     * A filter for Nacos data
     *
     * @since 2.6.5
     */
    private interface NacosDataFilter<T> {

        /**
         * Tests whether or not the specified data should be accepted.
         *
         * @param data The data to be tested
         * @return <code>true</code> if and only if <code>data</code>
         * should be accepted
         */
        boolean accept(T data);

    }

    /**
     * {@link NamingService} Callback
     *
     * @since 2.6.5
     */
    interface NamingServiceCallback {

        /**
         * Callback
         *
         * @param namingService {@link NamingService}
         * @throws NacosException
         */
        void callback(NamingService namingService) throws NacosException;

    }
}
