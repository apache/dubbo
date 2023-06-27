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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.ProtocolServiceKey;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.DOT_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;

public class MetadataInfo implements Serializable {
    public static final MetadataInfo EMPTY = new MetadataInfo();
    private static final Logger logger = LoggerFactory.getLogger(MetadataInfo.class);

    private String app;
    // revision that will report to registry or remote meta center, must always update together with rawMetadataInfo, check {@link this#calAndGetRevision}
    private volatile String revision;
    // key format is '{group}/{interface name}:{version}:{protocol}'
    private final Map<String, ServiceInfo> services;

    /* used at runtime */
    private transient AtomicBoolean initiated = new AtomicBoolean(false);
    // Json formatted metadata that will report to remote meta center, must always update together with revision, check {@link this#calAndGetRevision}
    private transient volatile String rawMetadataInfo;
    // key format is '{group}/{interface name}:{version}'
    private transient Map<String, Set<ServiceInfo>> subscribedServices;
    private transient final Map<String, String> extendParams;
    private transient final Map<String, String> instanceParams;
    protected transient volatile boolean updated = false;
    private transient ConcurrentNavigableMap<String, SortedSet<URL>> subscribedServiceURLs;
    private transient ConcurrentNavigableMap<String, SortedSet<URL>> exportedServiceURLs;
    private transient ExtensionLoader<MetadataParamsFilter> loader;

    public MetadataInfo() {
        this(null);
    }

    public MetadataInfo(String app) {
        this(app, null, null);
    }

    public MetadataInfo(String app, String revision, Map<String, ServiceInfo> services) {
        this.app = app;
        this.revision = revision;
        this.services = services == null ? new ConcurrentHashMap<>() : services;
        this.extendParams = new ConcurrentHashMap<>();
        this.instanceParams = new ConcurrentHashMap<>();
    }

    private MetadataInfo(String app, String revision, Map<String, ServiceInfo> services, AtomicBoolean initiated,
                        Map<String, String> extendParams, Map<String, String> instanceParams, boolean updated,
                        ConcurrentNavigableMap<String, SortedSet<URL>> subscribedServiceURLs,
                        ConcurrentNavigableMap<String, SortedSet<URL>> exportedServiceURLs,
                        ExtensionLoader<MetadataParamsFilter> loader) {
        this.app = app;
        this.revision = revision;
        this.services = new ConcurrentHashMap<>(services);
        this.initiated = new AtomicBoolean(initiated.get());
        this.extendParams = new ConcurrentHashMap<>(extendParams);
        this.instanceParams = new ConcurrentHashMap<>(instanceParams);
        this.updated = updated;
        this.subscribedServiceURLs = subscribedServiceURLs == null ? null : new ConcurrentSkipListMap<>(subscribedServiceURLs);
        this.exportedServiceURLs = exportedServiceURLs == null ? null : new ConcurrentSkipListMap<>(exportedServiceURLs);
        this.loader = loader;
    }

    /**
     * Initialize is needed when MetadataInfo is created from deserialization on the consumer side before being used for RPC call.
     */
    public void init() {
        if (!initiated.compareAndSet(false, true)) {
            return;
        }
        if (CollectionUtils.isNotEmptyMap(services)) {
            services.forEach((_k, serviceInfo) -> {
                serviceInfo.init();
                // create duplicate serviceKey(without protocol)->serviceInfo mapping to support metadata search when protocol is not specified on consumer side.
                if (subscribedServices == null) {
                    subscribedServices = new HashMap<>();
                }
                Set<ServiceInfo> serviceInfos = subscribedServices.computeIfAbsent(serviceInfo.getServiceKey(), _key -> new HashSet<>());
                serviceInfos.add(serviceInfo);
            });
        }
    }

    public synchronized void addService(URL url) {
        // fixme, pass in application mode context during initialization of MetadataInfo.
        if (this.loader == null) {
            this.loader = url.getOrDefaultApplicationModel().getExtensionLoader(MetadataParamsFilter.class);
        }
        List<MetadataParamsFilter> filters = loader.getActivateExtension(url, "params-filter");
        // generate service level metadata
        ServiceInfo serviceInfo = new ServiceInfo(url, filters);
        this.services.put(serviceInfo.getMatchKey(), serviceInfo);
        // extract common instance level params
        extractInstanceParams(url, filters);

        if (exportedServiceURLs == null) {
            exportedServiceURLs = new ConcurrentSkipListMap<>();
        }
        addURL(exportedServiceURLs, url);
        updated = true;
    }

    public synchronized void removeService(URL url) {
        if (url == null) {
            return;
        }
        this.services.remove(url.getProtocolServiceKey());
        if (exportedServiceURLs != null) {
            removeURL(exportedServiceURLs, url);
        }

        updated = true;
    }

    public String getRevision() {
        return revision;
    }

    /**
     * Calculation of this instance's status like revision and modification of the same instance must be synchronized among different threads.
     * <p>
     * Usage of this method is strictly restricted to certain points such as when during registration. Always try to use {@link this#getRevision()} instead.
     */
    public synchronized String calAndGetRevision() {
        if (revision != null && !updated) {
            return revision;
        }

        updated = false;

        if (CollectionUtils.isEmptyMap(services)) {
            this.revision = EMPTY_REVISION;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(app);
            for (Map.Entry<String, ServiceInfo> entry : new TreeMap<>(services).entrySet()) {
                sb.append(entry.getValue().toDescString());
            }
            String tempRevision = RevisionResolver.calRevision(sb.toString());
            if (!StringUtils.isEquals(this.revision, tempRevision)) {
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("metadata revision changed: %s -> %s, app: %s, services: %d", this.revision, tempRevision, this.app, this.services.size()));
                }
                this.revision = tempRevision;
                this.rawMetadataInfo = JsonUtils.toJson(this);
            }
        }
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Transient
    public String getContent() {
        return this.rawMetadataInfo;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Map<String, ServiceInfo> getServices() {
        return services;
    }

    /**
     * Get service info of an interface with specified group, version and protocol
     * @param protocolServiceKey key is of format '{group}/{interface name}:{version}:{protocol}'
     * @return the specific service info related to protocolServiceKey
     */
    public ServiceInfo getServiceInfo(String protocolServiceKey) {
        return services.get(protocolServiceKey);
    }

    /**
     * Get service infos of an interface with specified group, version.
     * There may have several service infos of different protocols, this method will simply pick the first one.
     *
     * @param serviceKeyWithoutProtocol key is of format '{group}/{interface name}:{version}'
     * @return the first service info related to serviceKey
     */
    public ServiceInfo getNoProtocolServiceInfo(String serviceKeyWithoutProtocol) {
        if (CollectionUtils.isEmptyMap(subscribedServices)) {
            return null;
        }
        Set<ServiceInfo> subServices = subscribedServices.get(serviceKeyWithoutProtocol);
        if (CollectionUtils.isNotEmpty(subServices)) {
           return subServices.iterator().next();
        }
        return null;
    }

    public ServiceInfo getValidServiceInfo(String serviceKey) {
        ServiceInfo serviceInfo = getServiceInfo(serviceKey);
        if (serviceInfo == null) {
            serviceInfo = getNoProtocolServiceInfo(serviceKey);
            if (serviceInfo == null) {
                return null;
            }
        }
        return serviceInfo;
    }

    public List<ServiceInfo> getMatchedServiceInfos(ProtocolServiceKey consumerProtocolServiceKey) {
        return getServices().values()
            .stream()
            .filter(serviceInfo -> serviceInfo.matchProtocolServiceKey(consumerProtocolServiceKey))
            .collect(Collectors.toList());
    }

    public Map<String, String> getExtendParams() {
        return extendParams;
    }

    public Map<String, String> getInstanceParams() {
        return instanceParams;
    }

    public String getParameter(String key, String serviceKey) {
        ServiceInfo serviceInfo = getValidServiceInfo(serviceKey);
        if (serviceInfo == null) return null;
        return serviceInfo.getParameter(key);
    }

    public Map<String, String> getParameters(String serviceKey) {
        ServiceInfo serviceInfo = getValidServiceInfo(serviceKey);
        if (serviceInfo == null) {
            return Collections.emptyMap();
        }
        return serviceInfo.getAllParams();
    }

    public String getServiceString(String protocolServiceKey) {
        if (protocolServiceKey == null) {
            return null;
        }

        ServiceInfo serviceInfo = getValidServiceInfo(protocolServiceKey);
        if (serviceInfo == null) {
            return null;
        }
        return serviceInfo.toFullString();
    }

    public synchronized void addSubscribedURL(URL url) {
        if (subscribedServiceURLs == null) {
            subscribedServiceURLs = new ConcurrentSkipListMap<>();
        }
        addURL(subscribedServiceURLs, url);
    }

    public boolean removeSubscribedURL(URL url) {
        if (subscribedServiceURLs == null) {
            return true;
        }
        return removeURL(subscribedServiceURLs, url);
    }

    public ConcurrentNavigableMap<String, SortedSet<URL>> getSubscribedServiceURLs() {
        return subscribedServiceURLs;
    }

    public ConcurrentNavigableMap<String, SortedSet<URL>> getExportedServiceURLs() {
        return exportedServiceURLs;
    }

    public Set<URL> collectExportedURLSet() {
        if (exportedServiceURLs == null) {
            return Collections.emptySet();
        }
        return exportedServiceURLs.values().stream()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private boolean addURL(Map<String, SortedSet<URL>> serviceURLs, URL url) {
        SortedSet<URL> urls = serviceURLs.computeIfAbsent(url.getServiceKey(), this::newSortedURLs);
        // make sure the parameters of tmpUrl is variable
        return urls.add(url);
    }

    boolean removeURL(Map<String, SortedSet<URL>> serviceURLs, URL url) {
        String key = url.getServiceKey();
        SortedSet<URL> urls = serviceURLs.getOrDefault(key, null);
        if (urls == null) {
            return true;
        }
        boolean r = urls.remove(url);
        // if it is empty
        if (urls.isEmpty()) {
            serviceURLs.remove(key);
        }
        return r;
    }

    private SortedSet<URL> newSortedURLs(String serviceKey) {
        return new TreeSet<>(URLComparator.INSTANCE);
    }

    @Override
    public int hashCode() {
        return Objects.hash(app, services);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof MetadataInfo)) {
            return false;
        }

        MetadataInfo other = (MetadataInfo)obj;

        return Objects.equals(app, other.getApp())
            && ((services == null && other.services == null)
                || (services != null && services.equals(other.services)));
    }

    private void extractInstanceParams(URL url, List<MetadataParamsFilter> filters) {
        if (CollectionUtils.isEmpty(filters)) {
            return;
        }

        String[] included, excluded;
        if (filters.size() == 1) {
            MetadataParamsFilter filter = filters.get(0);
            included = filter.instanceParamsIncluded();
            excluded = filter.instanceParamsExcluded();
        } else {
            Set<String> includedList = new HashSet<>();
            Set<String> excludedList = new HashSet<>();
            filters.forEach(filter -> {
                if (ArrayUtils.isNotEmpty(filter.instanceParamsIncluded())) {
                    includedList.addAll(Arrays.asList(filter.instanceParamsIncluded()));
                }
                if (ArrayUtils.isNotEmpty(filter.instanceParamsExcluded())) {
                    excludedList.addAll(Arrays.asList(filter.instanceParamsExcluded()));
                }
            });
            included = includedList.toArray(new String[0]);
            excluded = excludedList.toArray(new String[0]);
        }

        Map<String, String> tmpInstanceParams = new HashMap<>();
        if (ArrayUtils.isNotEmpty(included)) {
            for (String p : included) {
                String value = url.getParameter(p);
                if (value != null) {
                    tmpInstanceParams.put(p, value);
                }
            }
        } else if (ArrayUtils.isNotEmpty(excluded)) {
            tmpInstanceParams.putAll(url.getParameters());
            for (String p : excluded) {
                tmpInstanceParams.remove(p);
            }
        }

        tmpInstanceParams.forEach((key, value) -> {
            String oldValue = instanceParams.put(key, value);
            if (!TIMESTAMP_KEY.equals(key) && oldValue != null && !oldValue.equals(value)) {
                throw new IllegalStateException(String.format("Inconsistent instance metadata found in different services: %s, %s", oldValue, value));
            }
        });
    }

    @Override
    public String toString() {
        return "metadata{" +
            "app='" + app + "'," +
            "revision='" + revision + "'," +
            "size=" + (services == null ? 0 : services.size()) + "," +
            "services=" + getSimplifiedServices(services) +
            "}";
    }

    public String toFullString() {
        return "metadata{" +
            "app='" + app + "'," +
            "revision='" + revision + "'," +
            "services=" + services +
            "}";
    }

    private String getSimplifiedServices(Map<String, ServiceInfo> services) {
        if (services == null) {
            return "[]";
        }

        return services.keySet().toString();
    }

    @Override
    public synchronized MetadataInfo clone() {
        return new MetadataInfo(app, revision, services, initiated, extendParams, instanceParams, updated, subscribedServiceURLs, exportedServiceURLs, loader);
    }

    private Object readResolve() {
        // create a new object from the deserialized one, in order to call constructor
        return new MetadataInfo(this.app, this.revision, this.services);
    }

    public static class ServiceInfo implements Serializable {
        private String name;
        private String group;
        private String version;
        private String protocol;
        private int port = -1;
        private String path; // most of the time, path is the same with the interface name.
        private Map<String, String> params;

        // params configured on consumer side,
        private volatile transient Map<String, String> consumerParams;
        // cached method params
        private volatile transient Map<String, Map<String, String>> methodParams;
        private volatile transient Map<String, Map<String, String>> consumerMethodParams;
        // cached numbers
        private volatile transient Map<String, Number> numbers;
        private volatile transient Map<String, Map<String, Number>> methodNumbers;
        // service + group + version
        private volatile transient String serviceKey;
        // service + group + version + protocol
        private volatile transient String matchKey;

        private volatile transient ProtocolServiceKey protocolServiceKey;

        private transient URL url;

        public ServiceInfo() {}

        public ServiceInfo(URL url, List<MetadataParamsFilter> filters) {
            this(url.getServiceInterface(), url.getGroup(), url.getVersion(), url.getProtocol(), url.getPort(), url.getPath(), null);
            this.url = url;
            Map<String, String> params = extractServiceParams(url, filters);
            // initialize method params caches.
            this.methodParams = URLParam.initMethodParameters(params);
            this.consumerMethodParams = URLParam.initMethodParameters(consumerParams);
        }

        public ServiceInfo(String name, String group, String version, String protocol, int port, String path, Map<String, String> params) {
            this.name = name;
            this.group = group;
            this.version = version;
            this.protocol = protocol;
            this.port = port;
            this.path = path;
            this.params = params == null ? new ConcurrentHashMap<>() : params;

            this.serviceKey = buildServiceKey(name, group, version);
            this.matchKey = buildMatchKey();
        }

        private Map<String, String> extractServiceParams(URL url, List<MetadataParamsFilter> filters) {
            Map<String, String> params = new HashMap<>();

            if (CollectionUtils.isEmpty(filters)) {
                params.putAll(url.getParameters());
                this.params = params;
                return params;
            }

            String[] included, excluded;
            if (filters.size() == 1) {
                included = filters.get(0).serviceParamsIncluded();
                excluded = filters.get(0).serviceParamsExcluded();
            } else {
                Set<String> includedList = new HashSet<>();
                Set<String> excludedList = new HashSet<>();
                for (MetadataParamsFilter filter : filters) {
                    if (ArrayUtils.isNotEmpty(filter.serviceParamsIncluded())) {
                        includedList.addAll(Arrays.asList(filter.serviceParamsIncluded()));
                    }
                    if (ArrayUtils.isNotEmpty(filter.serviceParamsExcluded())) {
                        excludedList.addAll(Arrays.asList(filter.serviceParamsExcluded()));
                    }
                }
                included = includedList.toArray(new String[0]);
                excluded = excludedList.toArray(new String[0]);
            }

            if (ArrayUtils.isNotEmpty(included)) {
                String[] methods = url.getParameter(METHODS_KEY, (String[]) null);
                for (String p : included) {
                    String value = url.getParameter(p);
                    if (StringUtils.isNotEmpty(value) && params.get(p) == null) {
                        params.put(p, value);
                    }
                    appendMethodParams(url, params, methods, p);
                }
            } else if (ArrayUtils.isNotEmpty(excluded)) {
                for (Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    boolean shouldAdd = true;
                    for (String excludeKey : excluded) {
                        if (key.equalsIgnoreCase(excludeKey) || key.contains("." + excludeKey)) {
                            shouldAdd = false;
                            break;
                        }
                    }
                    if (shouldAdd) {
                        params.put(key, value);
                    }
                }
            }

            this.params = params;
            return params;
        }

        private void appendMethodParams(URL url, Map<String, String> params, String[] methods, String p) {
            if (methods != null) {
                for (String method : methods) {
                    String mValue = url.getMethodParameterStrict(method, p);
                    if (StringUtils.isNotEmpty(mValue)) {
                        params.put(method + DOT_SEPARATOR + p, mValue);
                    }
                }
            }
        }

        /**
         * Initialize necessary caches right after deserialization on the consumer side
         */
        protected void init() {
            buildMatchKey();
            buildServiceKey(name, group, version);
            // init method params
            this.methodParams = URLParam.initMethodParameters(params);
            // Actually, consumer params is empty after deserialized on the consumer side, so no need to initialize.
            // Check how InstanceAddressURL operates on consumer url for more detail.
//            this.consumerMethodParams = URLParam.initMethodParameters(consumerParams);
            // no need to init numbers for it's only for cache purpose
        }

        public String getMatchKey() {
            if (matchKey != null) {
                return matchKey;
            }
            buildMatchKey();
            return matchKey;
        }

        private String buildMatchKey() {
            matchKey = getServiceKey();
            if (StringUtils.isNotEmpty(protocol)) {
                matchKey = getServiceKey() + GROUP_CHAR_SEPARATOR + protocol;
            }
            return matchKey;
        }

        public boolean matchProtocolServiceKey(ProtocolServiceKey protocolServiceKey) {
            return ProtocolServiceKey.Matcher.isMatch(protocolServiceKey, getProtocolServiceKey());
        }

        public ProtocolServiceKey getProtocolServiceKey() {
            if (protocolServiceKey != null) {
                return protocolServiceKey;
            }
            protocolServiceKey = new ProtocolServiceKey(name, version, group,  protocol);
            return protocolServiceKey;
        }

        private String buildServiceKey(String name, String group, String version) {
            this.serviceKey = URL.buildKey(name, group, version);
            return this.serviceKey;
        }

        public String getServiceKey() {
            if (serviceKey != null) {
                return serviceKey;
            }
            buildServiceKey(name, group, version);
            return serviceKey;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public Map<String, String> getParams() {
            if (params == null) {
                return Collections.emptyMap();
            }
            return params;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }

        @Transient
        public Map<String, String> getAllParams() {
            if (consumerParams != null) {
                Map<String, String> allParams = new HashMap<>((int) ((params.size() + consumerParams.size()) / 0.75f + 1));
                allParams.putAll(params);
                allParams.putAll(consumerParams);
                return allParams;
            }
            return params;
        }

        public String getParameter(String key) {
            if (consumerParams != null) {
                String value = consumerParams.get(key);
                if (value != null) {
                    return value;
                }
            }
            return params.get(key);
        }

        public String getMethodParameter(String method, String key, String defaultValue) {
            String value = getMethodParameter(method, key, consumerMethodParams);
            if (value != null) {
                return value;
            }
            value = getMethodParameter(method, key, methodParams);
            return value == null ? defaultValue : value;
        }

        private String getMethodParameter(String method, String key, Map<String, Map<String, String>> map) {
            String value = null;
            if (map == null) {
                return value;
            }

            Map<String, String> keyMap = map.get(method);
            if (keyMap != null) {
                value = keyMap.get(key);
            }
            return value;
        }

        public boolean hasMethodParameter(String method, String key) {
            String value = this.getMethodParameter(method, key, (String) null);
            return StringUtils.isNotEmpty(value);
        }

        public boolean hasMethodParameter(String method) {
            return (consumerMethodParams != null && consumerMethodParams.containsKey(method))
                || (methodParams != null && methodParams.containsKey(method));
        }

        public String toDescString() {
            return this.getMatchKey() + port + path + new TreeMap<>(getParams());
        }

        public void addParameter(String key, String value) {
            if (consumerParams != null) {
                this.consumerParams.put(key, value);
            }
            // refresh method params
            consumerMethodParams = URLParam.initMethodParameters(consumerParams);
        }

        public void addParameterIfAbsent(String key, String value) {
            if (consumerParams != null) {
                this.consumerParams.putIfAbsent(key, value);
            }
            // refresh method params
            consumerMethodParams = URLParam.initMethodParameters(consumerParams);
        }

        public void addConsumerParams(Map<String, String> params) {
            // copy once for one service subscription
            if (consumerParams == null) {
                consumerParams = new ConcurrentHashMap<>(params);
                // init method params
                consumerMethodParams = URLParam.initMethodParameters(consumerParams);
            }
        }

        public Map<String, Number> getNumbers() {
            // concurrent initialization is tolerant
            if (numbers == null) {
                numbers = new ConcurrentHashMap<>();
            }
            return numbers;
        }

        public Map<String, Map<String, Number>> getMethodNumbers() {
            if (methodNumbers == null) { // concurrent initialization is tolerant
                methodNumbers = new ConcurrentHashMap<>();
            }
            return methodNumbers;
        }

        public URL getUrl() {
            return url;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof ServiceInfo)) {
                return false;
            }

            ServiceInfo serviceInfo = (ServiceInfo) obj;
            /**
             * Equals to Objects.equals(this.getMatchKey(), serviceInfo.getMatchKey()), but match key will not get initialized
             * on json deserialization.
             */
            return Objects.equals(this.getVersion(), serviceInfo.getVersion())
                && Objects.equals(this.getGroup(), serviceInfo.getGroup())
                && Objects.equals(this.getName(), serviceInfo.getName())
                && Objects.equals(this.getProtocol(), serviceInfo.getProtocol())
                && Objects.equals(this.getPort(), serviceInfo.getPort())
                && this.getParams().equals(serviceInfo.getParams());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getVersion(), getGroup(), getName(), getProtocol(), getPort(), getParams());
        }

        @Override
        public String toString() {
            return getMatchKey();
        }

        public String toFullString() {
            return "service{" +
                "name='" + name + "'," +
                "group='" + group + "'," +
                "version='" + version + "'," +
                "protocol='" + protocol + "'," +
                "port='" + port + "'," +
                "params=" + params + "," +
                "}";
        }
    }

    static class URLComparator implements Comparator<URL> {

        public static final URLComparator INSTANCE = new URLComparator();

        @Override
        public int compare(URL o1, URL o2) {
            return o1.toFullString().compareTo(o2.toFullString());
        }
    }
}
