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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.DubboServiceAddressURL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryNotifier;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.registry.support.SkipFailbackWrapperException;
import org.apache.dubbo.rpc.RpcException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_NACOS_EXCEPTION;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_ENABLE_EMPTY_PROTECTION;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.ENABLE_EMPTY_PROTECTION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.NACOE_REGISTER_COMPATIBLE;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTER_CONSUMER_URL_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTERS_CATEGORY;
import static org.apache.dubbo.registry.Constants.ADMIN_PROTOCOL;
import static org.apache.dubbo.registry.nacos.NacosServiceName.NAME_SEPARATOR;
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
    private static final List<String> ALL_SUPPORTED_CATEGORIES =
            Arrays.asList(PROVIDERS_CATEGORY, CONSUMERS_CATEGORY, ROUTERS_CATEGORY, CONFIGURATORS_CATEGORY);

    private static final int CATEGORY_INDEX = 0;

    private static final int SERVICE_INTERFACE_INDEX = 1;

    private static final int SERVICE_VERSION_INDEX = 2;

    private static final int SERVICE_GROUP_INDEX = 3;

    private static final String WILDCARD = "*";

    private static final String UP = "UP";

    /**
     * The separator for service name Change a constant to be configurable, it's designed for Windows file name that is
     * compatible with old Nacos binary release(< 0.6.1)
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

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(NacosRegistry.class);
    private final NacosNamingServiceWrapper namingService;
    /**
     * {@link ScheduledExecutorService} lookup Nacos service names(only for Dubbo-OPS)
     */
    private volatile ScheduledExecutorService scheduledExecutorService;

    private final Map<URL, Map<NotifyListener, NacosAggregateListener>> originToAggregateListener =
            new ConcurrentHashMap<>();

    private final Map<URL, Map<NacosAggregateListener, Map<String, EventListener>>> nacosListeners =
            new ConcurrentHashMap<>();
    private final boolean supportLegacyServiceName;

    public NacosRegistry(URL url, NacosNamingServiceWrapper namingService) {
        super(url);
        this.namingService = namingService;
        this.supportLegacyServiceName = url.getParameter("nacos.subscribe.legacy-name", true);
    }

    @Override
    public boolean isAvailable() {
        return UP.equals(namingService.getServerStatus());
    }

    @Override
    public List<URL> lookup(final URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            List<URL> urls = new LinkedList<>();
            Set<String> serviceNames = getServiceNames(url, null);
            for (String serviceName : serviceNames) {
                List<Instance> instances =
                        namingService.getAllInstances(serviceName, getUrl().getGroup(Constants.DEFAULT_GROUP));
                urls.addAll(buildURLs(url, instances));
            }
            return urls;
        } catch (SkipFailbackWrapperException exception) {
            throw exception;
        } catch (Exception cause) {
            throw new RpcException(
                    "Failed to lookup " + url + " from nacos " + getUrl() + ", cause: " + cause.getMessage(), cause);
        }
    }

    @Override
    public void doRegister(URL url) {
        try {
            if (PROVIDER_SIDE.equals(url.getSide()) || getUrl().getParameter(REGISTER_CONSUMER_URL_KEY, false)) {
                Instance instance = createInstance(url);

                Set<String> serviceNames = new HashSet<>();
                // by default servicename is "org.apache.dubbo.xxService:1.0.0:"
                String serviceName = getServiceName(url, false);
                serviceNames.add(serviceName);

                // in https://github.com/apache/dubbo/issues/14075
                if (getUrl().getParameter(NACOE_REGISTER_COMPATIBLE, false)) {
                    // servicename is "org.apache.dubbo.xxService:1.0.0"
                    String compatibleServiceName = getServiceName(url, true);
                    serviceNames.add(compatibleServiceName);
                }

                /**
                 *  namingService.registerInstance with
                 *  {@link org.apache.dubbo.registry.support.AbstractRegistry#registryUrl}
                 *  default {@link DEFAULT_GROUP}
                 *
                 * in https://github.com/apache/dubbo/issues/5978
                 */
                for (String service : serviceNames) {
                    namingService.registerInstance(service, getUrl().getGroup(Constants.DEFAULT_GROUP), instance);
                }
            } else {
                logger.info("Please set 'dubbo.registry.parameters.register-consumer-url=true' to turn on consumer "
                        + "url registration.");
            }
        } catch (SkipFailbackWrapperException exception) {
            throw exception;
        } catch (Exception cause) {
            throw new RpcException(
                    "Failed to register " + url + " to nacos " + getUrl() + ", cause: " + cause.getMessage(), cause);
        }
    }

    @Override
    public void doUnregister(final URL url) {
        try {
            Instance instance = createInstance(url);

            Set<String> serviceNames = new HashSet<>();
            // by default servicename is "org.apache.dubbo.xxService:1.0.0:"
            String serviceName = getServiceName(url, false);
            serviceNames.add(serviceName);

            // in https://github.com/apache/dubbo/issues/14075
            if (getUrl().getParameter(NACOE_REGISTER_COMPATIBLE, false)) {
                // servicename is "org.apache.dubbo.xxService:1.0.0"
                String serviceName1 = getServiceName(url, true);
                serviceNames.add(serviceName1);
            }

            for (String service : serviceNames) {
                namingService.deregisterInstance(
                        service, getUrl().getGroup(Constants.DEFAULT_GROUP), instance.getIp(), instance.getPort());
            }
        } catch (SkipFailbackWrapperException exception) {
            throw exception;
        } catch (Exception cause) {
            throw new RpcException(
                    "Failed to unregister " + url + " to nacos " + getUrl() + ", cause: " + cause.getMessage(), cause);
        }
    }

    @Override
    public void doSubscribe(final URL url, final NotifyListener listener) {
        NacosAggregateListener nacosAggregateListener = new NacosAggregateListener(listener);
        originToAggregateListener
                .computeIfAbsent(url, k -> new ConcurrentHashMap<>())
                .put(listener, nacosAggregateListener);

        Set<String> serviceNames = getServiceNames(url, nacosAggregateListener);

        doSubscribe(url, nacosAggregateListener, serviceNames);
    }

    private void doSubscribe(final URL url, final NacosAggregateListener listener, final Set<String> serviceNames) {
        try {
            if (isServiceNamesWithCompatibleMode(url)) {

                /**
                 * Get all instances with serviceNames to avoid instance overwrite and but with empty instance mentioned
                 * in https://github.com/apache/dubbo/issues/5885 and https://github.com/apache/dubbo/issues/5899
                 *
                 * namingService.getAllInstances with
                 * {@link org.apache.dubbo.registry.support.AbstractRegistry#registryUrl}
                 * default {@link DEFAULT_GROUP}
                 *
                 * in https://github.com/apache/dubbo/issues/5978
                 */
                for (String serviceName : serviceNames) {
                    List<Instance> instances =
                            namingService.getAllInstances(serviceName, getUrl().getGroup(Constants.DEFAULT_GROUP));
                    notifySubscriber(url, serviceName, listener, instances);
                }
                for (String serviceName : serviceNames) {
                    subscribeEventListener(serviceName, url, listener);
                }
            } else {
                for (String serviceName : serviceNames) {
                    List<Instance> instances = new LinkedList<>();
                    instances.addAll(
                            namingService.getAllInstances(serviceName, getUrl().getGroup(Constants.DEFAULT_GROUP)));
                    String serviceInterface = serviceName;
                    String[] segments = serviceName.split(SERVICE_NAME_SEPARATOR, -1);
                    if (segments.length == 4) {
                        serviceInterface = segments[SERVICE_INTERFACE_INDEX];
                    }
                    URL subscriberURL = url.setPath(serviceInterface)
                            .addParameters(INTERFACE_KEY, serviceInterface, CHECK_KEY, String.valueOf(false));
                    notifySubscriber(subscriberURL, serviceName, listener, instances);
                    subscribeEventListener(serviceName, subscriberURL, listener);
                }
            }
        } catch (SkipFailbackWrapperException exception) {
            throw exception;
        } catch (Throwable cause) {
            throw new RpcException(
                    "Failed to subscribe " + url + " to nacos " + getUrl() + ", cause: " + cause.getMessage(), cause);
        }
    }

    /**
     * Since 2.7.6 the legacy service name will be added to serviceNames to fix bug with
     * https://github.com/apache/dubbo/issues/5442
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
        } else {
            Map<NotifyListener, NacosAggregateListener> listenerMap = originToAggregateListener.get(url);
            if (listenerMap == null) {
                logger.warn(
                        REGISTRY_NACOS_EXCEPTION,
                        "",
                        "",
                        String.format(
                                "No aggregate listener found for url %s, "
                                        + "this service might have already been unsubscribed.",
                                url));
                return;
            }
            NacosAggregateListener nacosAggregateListener = listenerMap.remove(listener);
            if (nacosAggregateListener != null) {
                Set<String> serviceNames = nacosAggregateListener.getServiceNames();
                try {
                    doUnsubscribe(url, nacosAggregateListener, serviceNames);
                } catch (NacosException e) {
                    logger.error(
                            REGISTRY_NACOS_EXCEPTION,
                            "",
                            "",
                            "Failed to unsubscribe " + url + " to nacos " + getUrl() + ", cause: " + e.getMessage(),
                            e);
                }
            }
            if (listenerMap.isEmpty()) {
                originToAggregateListener.remove(url);
            }
        }
    }

    private void doUnsubscribe(
            final URL url, final NacosAggregateListener nacosAggregateListener, final Set<String> serviceNames)
            throws NacosException {
        for (String serviceName : serviceNames) {
            unsubscribeEventListener(serviceName, url, nacosAggregateListener);
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
    private Set<String> getServiceNames(URL url, NacosAggregateListener listener) {
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
            if (supportLegacyServiceName) {
                // Add the legacy service name since 2.7.6
                String legacySubscribedServiceName = getLegacySubscribedServiceName(url);
                if (!serviceName.toString().equals(legacySubscribedServiceName)) {
                    // avoid duplicated service names
                    serviceNames.add(legacySubscribedServiceName);
                }
            }
        } else {
            serviceNames = filterServiceNames(serviceName);
        }

        return serviceNames;
    }

    private Set<String> filterServiceNames(NacosServiceName serviceName) {
        try {
            Set<String> serviceNames = new LinkedHashSet<>();
            serviceNames.addAll(
                    namingService
                            .getServicesOfServer(1, Integer.MAX_VALUE, getUrl().getGroup(Constants.DEFAULT_GROUP))
                            .getData()
                            .stream()
                            .filter(this::isConformRules)
                            .map(NacosServiceName::new)
                            .filter(serviceName::isCompatible)
                            .map(NacosServiceName::toString)
                            .collect(Collectors.toList()));
            return serviceNames;
        } catch (SkipFailbackWrapperException exception) {
            throw exception;
        } catch (Throwable cause) {
            throw new RpcException(
                    "Failed to filter serviceName from nacos, url: " + getUrl() + ", serviceName: " + serviceName
                            + ", cause: " + cause.getMessage(),
                    cause);
        }
    }

    /**
     * Verify whether it is a dubbo service
     *
     * @param serviceName
     * @return
     * @since 2.7.12
     */
    private boolean isConformRules(String serviceName) {
        return serviceName.split(NAME_SEPARATOR, -1).length == 4;
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

    private void scheduleServiceNamesLookup(final URL url, final NacosAggregateListener listener) {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(
                    () -> {
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
                    },
                    LOOKUP_INTERVAL,
                    LOOKUP_INTERVAL,
                    TimeUnit.SECONDS);
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
        try {
            final Set<String> serviceNames = new LinkedHashSet<>();
            int pageIndex = 1;
            ListView<String> listView = namingService.getServicesOfServer(
                    pageIndex, PAGINATION_SIZE, getUrl().getGroup(Constants.DEFAULT_GROUP));
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
                listView = namingService.getServicesOfServer(
                        ++pageIndex, PAGINATION_SIZE, getUrl().getGroup(Constants.DEFAULT_GROUP));
                serviceNames.addAll(listView.getData());
            }
            return serviceNames;
        } catch (SkipFailbackWrapperException exception) {
            throw exception;
        } catch (Throwable cause) {
            throw new RpcException(
                    "Failed to get all serviceName from nacos, url: " + getUrl() + ", cause: " + cause.getMessage(),
                    cause);
        }
    }

    private void filterServiceNames(Set<String> serviceNames, URL url) {

        final List<String> categories = getCategories(url);

        final String targetServiceInterface = url.getServiceInterface();

        final String targetVersion = url.getVersion("");

        final String targetGroup = url.getGroup("");

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
            if (!WILDCARD.equals(targetServiceInterface)
                    && !StringUtils.isEquals(targetServiceInterface, serviceInterface)) {
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

    @Override
    public void destroy() {
        super.destroy();
        try {
            this.namingService.shutdown();
        } catch (NacosException e) {
            logger.warn(REGISTRY_NACOS_EXCEPTION, "", "", "Unable to shutdown nacos naming service", e);
        }
        this.nacosListeners.clear();
    }

    private List<URL> toUrlWithEmpty(URL consumerURL, Collection<Instance> instances) {
        consumerURL = removeParamsFromConsumer(consumerURL);
        List<URL> urls = buildURLs(consumerURL, instances);
        // Nacos does not support configurators and routers from registry, so all notifications are of providers type.
        if (urls.size() == 0 && !getUrl().getParameter(ENABLE_EMPTY_PROTECTION_KEY, DEFAULT_ENABLE_EMPTY_PROTECTION)) {
            logger.warn(
                    REGISTRY_NACOS_EXCEPTION,
                    "",
                    "",
                    "Received empty url address list and empty protection is "
                            + "disabled, will clear current available addresses");
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
                URL url = buildURL(consumerURL, instance);
                if (UrlUtils.isMatch(consumerURL, url)) {
                    urls.add(url);
                }
            }
        }
        return urls;
    }

    private void subscribeEventListener(String serviceName, final URL url, final NacosAggregateListener listener)
            throws NacosException {
        Map<NacosAggregateListener, Map<String, EventListener>> listeners =
                nacosListeners.computeIfAbsent(url, k -> new ConcurrentHashMap<>());

        Map<String, EventListener> eventListeners = listeners.computeIfAbsent(listener, k -> new ConcurrentHashMap<>());

        EventListener eventListener = eventListeners.computeIfAbsent(
                serviceName, k -> new RegistryChildListenerImpl(serviceName, url, listener));

        namingService.subscribe(serviceName, getUrl().getGroup(Constants.DEFAULT_GROUP), eventListener);
    }

    private void unsubscribeEventListener(String serviceName, final URL url, final NacosAggregateListener listener)
            throws NacosException {
        Map<NacosAggregateListener, Map<String, EventListener>> listenerToServiceEvent = nacosListeners.get(url);
        if (listenerToServiceEvent == null) {
            return;
        }
        Map<String, EventListener> serviceToEventMap = listenerToServiceEvent.get(listener);
        if (serviceToEventMap == null) {
            return;
        }
        EventListener eventListener = serviceToEventMap.remove(serviceName);
        if (eventListener == null) {
            return;
        }
        namingService.unsubscribe(
                serviceName, getUrl().getParameter(GROUP_KEY, Constants.DEFAULT_GROUP), eventListener);
        if (serviceToEventMap.isEmpty()) {
            listenerToServiceEvent.remove(listener);
        }
        if (listenerToServiceEvent.isEmpty()) {
            nacosListeners.remove(url);
        }
    }

    /**
     * Notify the Enabled {@link Instance instances} to subscriber.
     *
     * @param url       {@link URL}
     * @param listener  {@link NotifyListener}
     * @param instances all {@link Instance instances}
     */
    private void notifySubscriber(
            URL url, String serviceName, NacosAggregateListener listener, Collection<Instance> instances) {
        List<Instance> enabledInstances = new LinkedList<>(instances);
        if (enabledInstances.size() > 0) {
            //  Instances
            filterEnabledInstances(enabledInstances);
        }
        List<URL> aggregatedUrls =
                toUrlWithEmpty(url, listener.saveAndAggregateAllInstances(serviceName, enabledInstances));
        NacosRegistry.this.notify(url, listener.getNotifyListener(), aggregatedUrls);
    }

    /**
     * Get the categories from {@link URL}
     *
     * @param url {@link URL}
     * @return non-null array
     */
    private List<String> getCategories(URL url) {
        return ANY_VALUE.equals(url.getServiceInterface()) ? ALL_SUPPORTED_CATEGORIES : Arrays.asList(DEFAULT_CATEGORY);
    }

    private URL buildURL(URL consumerURL, Instance instance) {
        Map<String, String> metadata = instance.getMetadata();
        String protocol = metadata.get(PROTOCOL_KEY);
        String path = metadata.get(PATH_KEY);
        URL url = new ServiceConfigURL(protocol, instance.getIp(), instance.getPort(), path, instance.getMetadata());
        return new DubboServiceAddressURL(url.getUrlAddress(), url.getUrlParam(), consumerURL, null);
    }

    private Instance createInstance(URL url) {
        // Append default category if absent
        String category = url.getCategory(DEFAULT_CATEGORY);
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

    private NacosServiceName createServiceName(URL url) {
        return valueOf(url);
    }

    private String getServiceName(URL url, boolean needCompatible) {
        if (needCompatible) {
            return getCompatibleServiceName(url, url.getCategory(DEFAULT_CATEGORY));
        }
        return getServiceName(url, url.getCategory(DEFAULT_CATEGORY));
    }

    private String getServiceName(URL url, String category) {
        return category + SERVICE_NAME_SEPARATOR + url.getColonSeparatedKey();
    }

    private String getCompatibleServiceName(URL url, String category) {
        return category + SERVICE_NAME_SEPARATOR + url.getCompatibleColonSeparatedKey();
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

    private class RegistryChildListenerImpl implements EventListener {
        private final RegistryNotifier notifier;

        private final String serviceName;

        private final URL consumerUrl;

        private final NacosAggregateListener listener;

        public RegistryChildListenerImpl(String serviceName, URL consumerUrl, NacosAggregateListener listener) {
            this.serviceName = serviceName;
            this.consumerUrl = consumerUrl;
            this.listener = listener;
            this.notifier = new RegistryNotifier(getUrl(), NacosRegistry.this.getDelay()) {
                @Override
                protected void doNotify(Object rawAddresses) {
                    List<Instance> instances = (List<Instance>) rawAddresses;
                    NacosRegistry.this.notifySubscriber(consumerUrl, serviceName, listener, instances);
                }
            };
        }

        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                NamingEvent e = (NamingEvent) event;
                notifier.notify(e.getInstances());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RegistryChildListenerImpl that = (RegistryChildListenerImpl) o;
            return Objects.equals(serviceName, that.serviceName)
                    && Objects.equals(consumerUrl, that.consumerUrl)
                    && Objects.equals(listener, that.listener);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceName, consumerUrl, listener);
        }
    }
}
