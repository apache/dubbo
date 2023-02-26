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
package org.apache.dubbo.registry.client.metadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.Disposable;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.InstanceMetadataChangedListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static java.util.Collections.emptySortedSet;
import static java.util.Collections.unmodifiableSortedSet;
import static org.apache.dubbo.common.URL.buildKey;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_LOAD_METADATA;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;

/**
 * Implementation providing remote RPC service to facilitate the query of metadata information.
 */
public class MetadataServiceDelegation implements MetadataService, Disposable {
    ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final ApplicationModel applicationModel;
    private final RegistryManager registryManager;
    private ConcurrentMap<String, InstanceMetadataChangedListener> instanceMetadataChangedListenerMap = new ConcurrentHashMap<>();
    private URL url;
    // works only for DNS service discovery
    private String instanceMetadata;

    public MetadataServiceDelegation(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        registryManager = RegistryManager.getInstance(applicationModel);
    }

    /**
     * Gets the current Dubbo Service name
     *
     * @return non-null
     */
    @Override
    public String serviceName() {
        return ApplicationModel.ofNullable(applicationModel).getApplicationName();
    }

    @Override
    public URL getMetadataURL() {
        return url;
    }

    public void setMetadataURL(URL url) {
        this.url = url;
    }

    @Override
    public SortedSet<String> getSubscribedURLs() {
        return getAllUnmodifiableSubscribedURLs();
    }

    private SortedSet<String> getAllUnmodifiableServiceURLs() {
        SortedSet<URL> bizURLs = new TreeSet<>(URLComparator.INSTANCE);
        List<ServiceDiscovery> serviceDiscoveries = registryManager.getServiceDiscoveries();
        for (ServiceDiscovery sd : serviceDiscoveries) {
            MetadataInfo metadataInfo = sd.getLocalMetadata();
            Map<String, SortedSet<URL>> serviceURLs = metadataInfo.getExportedServiceURLs();
            if (serviceURLs == null) {
                continue;
            }
            for (Map.Entry<String, SortedSet<URL>> entry : serviceURLs.entrySet()) {
                SortedSet<URL> urls = entry.getValue();
                if (urls != null) {
                    for (URL url : urls) {
                        if (!MetadataService.class.getName().equals(url.getServiceInterface())) {
                            bizURLs.add(url);
                        }
                    }
                }
            }
        }
        return MetadataService.toSortedStrings(bizURLs);
    }

    private SortedSet<String> getAllUnmodifiableSubscribedURLs() {
        SortedSet<URL> bizURLs = new TreeSet<>(URLComparator.INSTANCE);
        List<ServiceDiscovery> serviceDiscoveries = registryManager.getServiceDiscoveries();
        for (ServiceDiscovery sd : serviceDiscoveries) {
            MetadataInfo metadataInfo = sd.getLocalMetadata();
            Map<String, SortedSet<URL>> serviceURLs = metadataInfo.getSubscribedServiceURLs();
            if (serviceURLs == null) {
                continue;
            }
            for (Map.Entry<String, SortedSet<URL>> entry : serviceURLs.entrySet()) {
                SortedSet<URL> urls = entry.getValue();
                if (urls != null) {
                    for (URL url : urls) {
                        if (!MetadataService.class.getName().equals(url.getServiceInterface())) {
                            bizURLs.add(url);
                        }
                    }
                }
            }
        }
        return MetadataService.toSortedStrings(bizURLs);
    }

    @Override
    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        if (ALL_SERVICE_INTERFACES.equals(serviceInterface)) {
            return getAllUnmodifiableServiceURLs();
        }
        String serviceKey = buildKey(serviceInterface, group, version);
        return unmodifiableSortedSet(getServiceURLs(getAllServiceURLs(), serviceKey, protocol));
    }

    private Map<String, SortedSet<URL>> getAllServiceURLs() {
        List<ServiceDiscovery> serviceDiscoveries = registryManager.getServiceDiscoveries();
        Map<String, SortedSet<URL>> allServiceURLs = new HashMap<>();
        for (ServiceDiscovery sd : serviceDiscoveries) {
            MetadataInfo metadataInfo = sd.getLocalMetadata();
            Map<String, SortedSet<URL>> serviceURLs = metadataInfo.getExportedServiceURLs();
            allServiceURLs.putAll(serviceURLs);
        }
        return allServiceURLs;
    }

    @Override
    public Set<URL> getExportedServiceURLs() {
        Set<URL> set = new HashSet<>();
        registryManager.getRegistries();
        for (Map.Entry<String, SortedSet<URL>> entry : getAllServiceURLs().entrySet()) {
            set.addAll(entry.getValue());
        }
        return set;
    }

    @Override
    public String getServiceDefinition(String interfaceName, String version, String group) {
        return "";
    }

    @Override
    public String getServiceDefinition(String serviceKey) {
        return "";
    }

    @Override
    public MetadataInfo getMetadataInfo(String revision) {
        if (StringUtils.isEmpty(revision)) {
            return null;
        }

        for (ServiceDiscovery sd : registryManager.getServiceDiscoveries()) {
            MetadataInfo metadataInfo = sd.getLocalMetadata(revision);
            if (metadataInfo != null && revision.equals(metadataInfo.getRevision())) {
                return metadataInfo;
            }
        }

        if (logger.isWarnEnabled()) {
            logger.warn(REGISTRY_FAILED_LOAD_METADATA, "", "", "metadata not found for revision: " + revision);
        }
        return null;
    }

    @Override
    public List<MetadataInfo> getMetadataInfos() {
        List<MetadataInfo> metadataInfos = new ArrayList<>();
        for (ServiceDiscovery sd : registryManager.getServiceDiscoveries()) {
            metadataInfos.add(sd.getLocalMetadata());
        }
        return metadataInfos;
    }

    @Override
    public void exportInstanceMetadata(String instanceMetadata) {
        this.instanceMetadata = instanceMetadata;
    }

    @Override
    public Map<String, InstanceMetadataChangedListener> getInstanceMetadataChangedListenerMap() {
        return instanceMetadataChangedListenerMap;
    }

    @Override
    public String getAndListenInstanceMetadata(String consumerId, InstanceMetadataChangedListener listener) {
        instanceMetadataChangedListenerMap.put(consumerId, listener);
        return instanceMetadata;
    }

    private SortedSet<String> getServiceURLs(Map<String, SortedSet<URL>> exportedServiceURLs, String serviceKey,
                                             String protocol) {

        SortedSet<URL> serviceURLs = exportedServiceURLs.get(serviceKey);

        if (isEmpty(serviceURLs)) {
            return emptySortedSet();
        }

        return MetadataService.toSortedStrings(serviceURLs.stream().filter(url -> isAcceptableProtocol(protocol, url)));
    }

    private boolean isAcceptableProtocol(String protocol, URL url) {
        return protocol == null
            || protocol.equals(url.getParameter(PROTOCOL_KEY))
            || protocol.equals(url.getProtocol());
    }

    @Override
    public void destroy() {

    }


    static class URLComparator implements Comparator<URL> {

        public static final URLComparator INSTANCE = new URLComparator();

        @Override
        public int compare(URL o1, URL o2) {
            return o1.toFullString().compareTo(o2.toFullString());
        }
    }
}
