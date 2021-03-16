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
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.nacos.util.NacosInstanceManageUtil;
import org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils;
import org.apache.dubbo.registry.support.FailbackRegistry;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTERS_CATEGORY;
import static org.apache.dubbo.registry.Constants.ADMIN_PROTOCOL;
import static org.apache.dubbo.registry.nacos.NacosServiceName.valueOf;

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
    private static final List<String> ALL_SUPPORTED_CATEGORIES = Arrays.asList(
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

    private final NamingService namingService;

    public NacosRegistry(URL url, NamingService namingService) {
        super(url);
        this.namingService = namingService;
    }

    @Override
    public boolean isAvailable() {
        return "UP".equals(namingService.getServerStatus());
    }

    @Override
    public List<URL> lookup(final URL url) {
        final List<URL> urls = new LinkedList<>();
        execute(namingService -> {
            Set<String> serviceNames = getServiceNames(url, null);
            for (String serviceName : serviceNames) {
                List<Instance> instances = namingService.getAllInstances(serviceName,
                        getUrl().getParameter(GROUP_KEY, Constants.DEFAULT_GROUP));
                urls.addAll(buildURLs(url, instances));
            }
        });
        return urls;
    }

    @Override
    public void doRegister(URL url) {
        final String serviceName = getServiceName(url);
        final Instance instance = createInstance(url);
        /**
         *  namingService.registerInstance with {@link org.apache.dubbo.registry.support.AbstractRegistry#registryUrl}
         *  default {@link DEFAULT_GROUP}
         *
         * in https://github.com/apache/dubbo/issues/5978
         */
        execute(namingService -> namingService.registerInstance(serviceName,
                getUrl().getParameter(GROUP_KEY, Constants.DEFAULT_GROUP), instance));
    }

    @Override
    public void doUnregister(final URL url) {
        execute(namingService -> {
            String serviceName = getServiceName(url);
            Instance instance = createInstance(url);
            namingService.deregisterInstance(serviceName,
                    getUrl().getParameter(GROUP_KEY, Constants.DEFAULT_GROUP),
                    instance.getIp(),
                    instance.getPort());
        });
    }

    @Override
    public void doSubscribe(final URL url, final NotifyListener listener) {
        Set<String> serviceNames = getServiceNames(url, listener);

        //Set corresponding serviceNames for easy search later
        if (isServiceNamesWithCompatibleMode(url)) {
            for (String serviceName : serviceNames) {
                NacosInstanceManageUtil.setCorrespondingServiceNames(serviceName, serviceNames);
            }
        }

        doSubscribe(url, listener, serviceNames);
    }

    private void doSubscribe(final URL url, final NotifyListener listener, final Set<String> serviceNames) {
        execute(namingService -> {
            if (isServiceNamesWithCompatibleMode(url)) {
                List<Instance> allCorrespondingInstanceList = Lists.newArrayList();

                /**
                 * Get all instances with serviceNames to avoid instance overwrite and but with empty instance mentioned
                 * in https://github.com/apache/dubbo/issues/5885 and https://github.com/apache/dubbo/issues/5899
                 *
                 * namingService.getAllInstances with {@link org.apache.dubbo.registry.support.AbstractRegistry#registryUrl}
                 * default {@link DEFAULT_GROUP}
                 *
                 * in https://github.com/apache/dubbo/issues/5978
                 */
                for (String serviceName : serviceNames) {
                    List<Instance> instances = namingService.getAllInstances(serviceName,
                            getUrl().getParameter(GROUP_KEY, Constants.DEFAULT_GROUP));
                    NacosInstanceManageUtil.initOrRefreshServiceInstanceList(serviceName, instances);
                    allCorrespondingInstanceList.addAll(instances);
                }
                notifySubscriber(url, listener, allCorrespondingInstanceList);
                for (String serviceName : serviceNames) {
                    subscribeEventListener(serviceName, url, listener);
                }
            } else {
                List<Instance> instances = new LinkedList<>();
                for (String serviceName : serviceNames) {
                    instances.addAll(namingService.getAllInstances(serviceName
                            , getUrl().getParameter(GROUP_KEY, Constants.DEFAULT_GROUP)));
                    notifySubscriber(url, listener, instances);
                    subscribeEventListener(serviceName, url, listener);
                }
            }

        });
    }

    /**
     * Since 2.7.6 the legacy service name will be added to serviceNames
     * to fix bug with https://github.com/apache/dubbo/issues/5442
     *
     * @param url
     * @return
     */
    private boolean isServiceNamesWithCompatibleMode(final URL url) {
        return !isAdminProtocol(url) && createServiceName(url).isConcrete();
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
    private Set<String> getServiceNames(URL url, NotifyListener listener) {
        if (isAdminProtocol(url)) {
            scheduleServiceNamesLookup(url, listener);
            return getServiceNamesForOps(url);
        } else {
            return getServiceNames0(url);
        }
    }

    private Set<String> getServiceNames0(URL url) {
        NacosServiceName serviceName = createServiceName(url);

        final Set<String> serviceNames;

        if (serviceName.isConcrete()) { // is the concrete service name
            serviceNames = new LinkedHashSet<>();
            serviceNames.add(serviceName.toString());
            // Add the legacy service name since 2.7.6
            String legacySubscribedServiceName = getLegacySubscribedServiceName(url);
            if (!serviceName.toString().equals(legacySubscribedServiceName)) {
                //avoid duplicated service names
                serviceNames.add(legacySubscribedServiceName);
            }
        } else {
            serviceNames = filterServiceNames(serviceName);
        }

        return serviceNames;
    }

    private Set<String> filterServiceNames(NacosServiceName serviceName) {
        Set<String> serviceNames = new LinkedHashSet<>();

        execute(namingService -> {

            serviceNames.addAll(namingService.getServicesOfServer(1, Integer.MAX_VALUE,
                    getUrl().getParameter(GROUP_KEY, Constants.DEFAULT_GROUP)).getData()
                    .stream()
                    .map(NacosServiceName::new)
                    .filter(serviceName::isCompatible)
                    .map(NacosServiceName::toString)
                    .collect(Collectors.toList()));

        });

        return serviceNames;
    }

    /**
     * Get the legacy subscribed service name for compatible with Dubbo 2.7.3 and below
     *
     * @param url {@link URL}
     * @return non-null
     * @since 2.7.6
     */
    private String getLegacySubscribedServiceName(URL url) {
        StringBuilder serviceNameBuilder = new StringBuilder(DEFAULT_CATEGORY);
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


    private boolean isAdminProtocol(URL url) {
        return ADMIN_PROTOCOL.equals(url.getProtocol());
    }

    private void scheduleServiceNamesLookup(final URL url, final NotifyListener listener) {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                Set<String> serviceNames = getAllServiceNames();
                filterData(serviceNames, serviceName -> {
                    boolean accepted = false;
                    for (String category : ALL_SUPPORTED_CATEGORIES) {
                        String prefix = category + SERVICE_NAME_SEPARATOR;
                        if (serviceName != null && serviceName.startsWith(prefix)) {
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
    private Set<String> getServiceNamesForOps(URL url) {
        Set<String> serviceNames = getAllServiceNames();
        filterServiceNames(serviceNames, url);
        return serviceNames;
    }

    private Set<String> getAllServiceNames() {

        final Set<String> serviceNames = new LinkedHashSet<>();

        execute(namingService -> {

            int pageIndex = 1;
            ListView<String> listView = namingService.getServicesOfServer(pageIndex, PAGINATION_SIZE,
                    getUrl().getParameter(GROUP_KEY, Constants.DEFAULT_GROUP));
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
                listView = namingService.getServicesOfServer(++pageIndex, PAGINATION_SIZE,
                        getUrl().getParameter(GROUP_KEY, Constants.DEFAULT_GROUP));
                serviceNames.addAll(listView.getData());
            }

        });

        return serviceNames;
    }

    private void filterServiceNames(Set<String> serviceNames, URL url) {

        final List<String> categories = getCategories(url);

        final String targetServiceInterface = url.getServiceInterface();

        final String targetVersion = url.getParameter(VERSION_KEY, "");

        final String targetGroup = url.getParameter(GROUP_KEY, "");

        filterData(serviceNames, serviceName -> {
            // split service name to segments
            // (required) segments[0] = category
            // (required) segments[1] = serviceInterface
            // (optional) segments[2] = version
            // (optional) segments[3] = group
            String[] segments = serviceName.split(SERVICE_NAME_SEPARATOR, -1);
            int length = segments.length;
            if (length != 4) { // must present 4 segments
                return false;
            }

            String category = segments[CATEGORY_INDEX];
            if (!categories.contains(category)) { // no match category
                return false;
            }

            String serviceInterface = segments[SERVICE_INTERFACE_INDEX];
            // no match service interface
            if (!WILDCARD.equals(targetServiceInterface) &&
                    !StringUtils.isEquals(targetServiceInterface, serviceInterface)) {
                return false;
            }

            // no match service version
            String version = segments[SERVICE_VERSION_INDEX];
            if (!WILDCARD.equals(targetVersion) && !StringUtils.isEquals(targetVersion, version)) {
                return false;
            }

            String group = segments[SERVICE_GROUP_INDEX];
            return group == null || WILDCARD.equals(targetGroup) || StringUtils.isEquals(targetGroup, group);
        });
    }

    private <T> void filterData(Collection<T> collection, NacosDataFilter<T> filter) {
        // remove if not accept
        collection.removeIf(data -> !filter.accept(data));
    }

    @Deprecated
    private List<String> doGetServiceNames(URL url) {
        List<String> categories = getCategories(url);
        List<String> serviceNames = new ArrayList<>(categories.size());
        for (String category : categories) {
            final String serviceName = getServiceName(url, category);
            serviceNames.add(serviceName);
        }
        return serviceNames;
    }

    private List<URL> toUrlWithEmpty(URL consumerURL, Collection<Instance> instances) {
        List<URL> urls = buildURLs(consumerURL, instances);
        if (urls.size() == 0) {
            URL empty = URLBuilder.from(consumerURL)
                    .setProtocol(EMPTY_PROTOCOL)
                    .addParameter(CATEGORY_KEY, DEFAULT_CATEGORY)
                    .build();
            urls.add(empty);
        }
        return urls;
    }

    private List<URL> buildURLs(URL consumerURL, Collection<Instance> instances) {
        List<URL> urls = new LinkedList<>();
        if (instances != null && !instances.isEmpty()) {
            for (Instance instance : instances) {
                URL url = buildURL(instance);
                if (UrlUtils.isMatch(consumerURL, url)) {
                    urls.add(url);
                }
            }
        }
        return urls;
    }

    private void subscribeEventListener(String serviceName, final URL url, final NotifyListener listener)
            throws NacosException {
        EventListener eventListener = event -> {
            if (event instanceof NamingEvent) {
                NamingEvent e = (NamingEvent) event;
                List<Instance> instances = e.getInstances();


                if (isServiceNamesWithCompatibleMode(url)) {
                    /**
                     * Get all instances with corresponding serviceNames to avoid instance overwrite and but with empty instance mentioned
                     * in https://github.com/apache/dubbo/issues/5885 and https://github.com/apache/dubbo/issues/5899
                     */
                    NacosInstanceManageUtil.initOrRefreshServiceInstanceList(serviceName, instances);
                    instances = NacosInstanceManageUtil.getAllCorrespondingServiceInstanceList(serviceName);
                }

                notifySubscriber(url, listener, instances);
            }
        };
        namingService.subscribe(serviceName,
                getUrl().getParameter(GROUP_KEY, Constants.DEFAULT_GROUP),
                eventListener);
    }

    /**
     * Notify the Enabled {@link Instance instances} to subscriber.
     *
     * @param url       {@link URL}
     * @param listener  {@link NotifyListener}
     * @param instances all {@link Instance instances}
     */
    private void notifySubscriber(URL url, NotifyListener listener, Collection<Instance> instances) {
        List<Instance> enabledInstances = new LinkedList<>(instances);
        if (enabledInstances.size() > 0) {
            //  Instances
            filterEnabledInstances(enabledInstances);
        }
        List<URL> urls = toUrlWithEmpty(url, enabledInstances);
        NacosRegistry.this.notify(url, listener, urls);
    }

    /**
     * Get the categories from {@link URL}
     *
     * @param url {@link URL}
     * @return non-null array
     */
    private List<String> getCategories(URL url) {
        return ANY_VALUE.equals(url.getServiceInterface()) ?
                ALL_SUPPORTED_CATEGORIES : Arrays.asList(DEFAULT_CATEGORY);
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
        newURL = newURL.addParameters(NacosNamingServiceUtils.getNacosPreservedParam(getUrl()));
        String ip = url.getHost();
        int port = url.getPort();
        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(port);
        instance.setMetadata(new HashMap<>(newURL.getParameters()));
        return instance;
    }

    private NacosServiceName createServiceName(URL url) {
        return valueOf(url);
    }

    private String getServiceName(URL url) {
        return getServiceName(url, url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY));
    }

    private String getServiceName(URL url, String category) {
        return category + SERVICE_NAME_SEPARATOR + url.getColonSeparatedKey();
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

    private void filterEnabledInstances(Collection<Instance> instances) {
        filterData(instances, Instance::isEnabled);
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
