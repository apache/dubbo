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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.DOT_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.FilterConstants.VALIDATION_KEY;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.rpc.Constants.INTERFACES;

public class MetadataInfo implements Serializable {
    public static final MetadataInfo EMPTY = new MetadataInfo();
    private static final Logger logger = LoggerFactory.getLogger(MetadataInfo.class);

    private String app;
    private String revision;
    private Map<String, ServiceInfo> services;

    private volatile AtomicBoolean initiated = new AtomicBoolean(false);

    // used at runtime
    private transient final Map<String, String> extendParams;
    private transient final Map<String, String> instanceParams;
    protected transient AtomicBoolean updated = new AtomicBoolean(false);
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
        updated.compareAndSet(false, true);
    }

    public synchronized void removeService(URL url) {
        if (url == null) {
            return;
        }
        this.services.remove(url.getProtocolServiceKey());
        if (exportedServiceURLs != null) {
            removeURL(exportedServiceURLs, url);
        }

        updated.compareAndSet(false, true);
    }

    public String getRevision() {
        return revision;
    }

    /**
     * Reported status and metadata modification must be synchronized if used in multiple threads.
     */
    public synchronized String calAndGetRevision() {
        if (revision != null && !updated.get()) {
            return revision;
        }

        updated.compareAndSet(true, false);

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
            }
        }
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
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

    public ServiceInfo getServiceInfo(String protocolServiceKey) {
        return services.get(protocolServiceKey);
    }

    public Map<String, String> getExtendParams() {
        return extendParams;
    }

    public Map<String, String> getInstanceParams() {
        return instanceParams;
    }

    public String getParameter(String key, String serviceKey) {
        ServiceInfo serviceInfo = services.get(serviceKey);
        if (serviceInfo == null) {
            return null;
        }
        return serviceInfo.getParameter(key);
    }

    public Map<String, String> getParameters(String serviceKey) {
        ServiceInfo serviceInfo = services.get(serviceKey);
        if (serviceInfo == null) {
            return Collections.emptyMap();
        }
        return serviceInfo.getAllParams();
    }

    public String getServiceString(String protocolServiceKey) {
        if (protocolServiceKey == null) {
            return null;
        }

        ServiceInfo serviceInfo = services.get(protocolServiceKey);
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

        filters.forEach(filter -> {
            String[] included = filter.instanceParamsIncluded();
            if (ArrayUtils.isEmpty(included)) {
                /*
                 * Does not put any parameter in instance if not specified.
                 * It will bring no functional suppression as long as all params will appear in service metadata.
                 */
            } else {
                for (String p : included) {
                    String value = url.getParameter(p);
                    if (value != null) {
                        String oldValue = instanceParams.put(p, value);
                        if (oldValue != null && !oldValue.equals(value)) {
                            throw new IllegalStateException(String.format("Inconsistent instance metadata found in different services: %s, %s", oldValue, value));
                        }
                    }
                }
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

    public static class ServiceInfo implements Serializable {
        private String name;
        private String group;
        private String version;
        private String protocol;
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

        private transient URL url;

        private final static String[] KEYS_TO_REMOVE = {MONITOR_KEY, BIND_IP_KEY, BIND_PORT_KEY, QOS_ENABLE,
            QOS_HOST, QOS_PORT, ACCEPT_FOREIGN_IP, VALIDATION_KEY, INTERFACES, PID_KEY, TIMESTAMP_KEY};

        public ServiceInfo() {}

        public ServiceInfo(URL url, List<MetadataParamsFilter> filters) {
            this(url.getServiceInterface(), url.getGroup(), url.getVersion(), url.getProtocol(), url.getPath(), null);
            this.url = url;
            Map<String, String> params = new HashMap<>();
            if (filters.size() == 0) {
                params.putAll(url.getParameters());
                for (String key : KEYS_TO_REMOVE) {
                    params.remove(key);
                }
            }
            for (MetadataParamsFilter filter : filters) {
                String[] paramsIncluded = filter.serviceParamsIncluded();
                if (ArrayUtils.isNotEmpty(paramsIncluded)) {
                    for (String p : paramsIncluded) {
                        String value = url.getParameter(p);
                        if (StringUtils.isNotEmpty(value) && params.get(p) == null) {
                            params.put(p, value);
                        }
                        String[] methods = url.getParameter(METHODS_KEY, (String[]) null);
                        if (methods != null) {
                            for (String method : methods) {
                                String mValue = url.getMethodParameterStrict(method, p);
                                if (StringUtils.isNotEmpty(mValue)) {
                                    params.put(method + DOT_SEPARATOR + p, mValue);
                                }
                            }
                        }
                    }
                }
            }
            this.params = params;
            // initialize method params caches.
            this.methodParams = URLParam.initMethodParameters(params);
            this.consumerMethodParams = URLParam.initMethodParameters(consumerParams);
        }

        public ServiceInfo(String name, String group, String version, String protocol, String path, Map<String, String> params) {
            this.name = name;
            this.group = group;
            this.version = version;
            this.protocol = protocol;
            this.path = path;
            this.params = params == null ? new ConcurrentHashMap<>() : params;

            this.serviceKey = buildServiceKey(name, group, version);
            this.matchKey = buildMatchKey();
        }

        /**
         * Initialize necessary caches right after deserialization on the consumer side
         */
        protected void init() {
            buildMatchKey();
            buildServiceKey(name, group, version);
            // init method params
            this.methodParams = URLParam.initMethodParameters(params);
            // Actually, consumer params is empty after deserialization on the consumer side, so no need to initialize.
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

        public Map<String, String> getParams() {
            if (params == null) {
                return Collections.emptyMap();
            }
            return params;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }

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
            return this.getMatchKey() + path + new TreeMap<>(getParams());
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
                && this.getParams().equals(serviceInfo.getParams());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getVersion(), getGroup(), getName(), getProtocol(), getParams());

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
