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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.Disposable;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.ConsumerId;
import org.apache.dubbo.metadata.DubboMetadataServiceTriple;
import org.apache.dubbo.metadata.HeartbeatMessage;
import org.apache.dubbo.metadata.InstanceMetadata;
import org.apache.dubbo.metadata.MetadataConstants;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataMessage;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceUtils;
import org.apache.dubbo.metadata.Revision;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

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

import static java.util.Collections.emptySortedSet;
import static java.util.Collections.unmodifiableSortedSet;
import static org.apache.dubbo.common.URL.buildKey;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_LOAD_METADATA;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.metadata.MetadataConstants.ALL_SERVICE_INTERFACES;

/**
 * Implementation providing remote RPC service to facilitate the query of metadata information.
 */
public class MetadataServiceDelegation extends DubboMetadataServiceTriple.MetadataServiceImplBase
        implements Disposable {
    ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final ApplicationModel applicationModel;
    private final RegistryManager registryManager;
    private ConcurrentMap<String, StreamObserver<InstanceMetadata>> instanceMetadataChangedListenerMap =
            new ConcurrentHashMap<>();

    private ConcurrentMap<String, StreamObserver<HeartbeatMessage>> heartbeatListenerMap = new ConcurrentHashMap<>();

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
    public String serviceName() {
        return ApplicationModel.ofNullable(applicationModel).getApplicationName();
    }

    public URL getMetadataURL() {
        return url;
    }

    public void setMetadataURL(URL url) {
        this.url = url;
    }

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
        return MetadataServiceUtils.toSortedStrings(bizURLs);
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
        return MetadataServiceUtils.toSortedStrings(bizURLs);
    }

    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        if (ALL_SERVICE_INTERFACES.equals(serviceInterface)) {
            return getAllUnmodifiableServiceURLs();
        }
        String serviceKey = buildKey(serviceInterface, group, version);
        return unmodifiableSortedSet(getServiceURLs(getAllServiceURLs(), serviceKey, protocol));
    }

    public SortedSet<String> getExportedURLs(String serviceInterface) {
        return getExportedURLs(serviceInterface, null);
    }

    public SortedSet<String> getExportedURLs() {
        return getExportedURLs(MetadataConstants.ALL_SERVICE_INTERFACES);
    }

    public SortedSet<String> getExportedURLs(String serviceInterface, String group) {
        return getExportedURLs(serviceInterface, group, null);
    }

    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version) {
        return getExportedURLs(serviceInterface, group, version, null);
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

    public Set<URL> getExportedServiceURLs() {
        Set<URL> set = new HashSet<>();
        registryManager.getRegistries();
        for (Map.Entry<String, SortedSet<URL>> entry : getAllServiceURLs().entrySet()) {
            set.addAll(entry.getValue());
        }
        return set;
    }

    public String getServiceDefinition(String interfaceName, String version, String group) {
        return "";
    }

    public String getServiceDefinition(String serviceKey) {
        return "";
    }

    @Override
    public MetadataMessage getMetadataInfo(Revision revision) {
        MetadataInfo wrapper = getMetadataInfo(revision.getValue());
        return wrapper == null ? MetadataMessage.newBuilder().build() : wrapper.toMetadataInfo();
    }

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

    public List<MetadataInfo> getMetadataInfos() {
        List<MetadataInfo> metadataInfos = new ArrayList<>();
        for (ServiceDiscovery sd : registryManager.getServiceDiscoveries()) {
            metadataInfos.add(sd.getLocalMetadata());
        }
        return metadataInfos;
    }

    public void exportInstanceMetadata(String instanceMetadata) {
        this.instanceMetadata = instanceMetadata;
    }

    public Map<String, StreamObserver<InstanceMetadata>> getInstanceMetadataChangedListenerMap() {
        return instanceMetadataChangedListenerMap;
    }

    public ConcurrentMap<String, StreamObserver<HeartbeatMessage>> getHeartbeatListenerMap() {
        return heartbeatListenerMap;
    }

    @Override
    public void getAndListenInstanceMetadata(ConsumerId consumerId, StreamObserver<InstanceMetadata> responseObserver) {
        instanceMetadataChangedListenerMap.put(consumerId.getId(), responseObserver);
        responseObserver.onNext(
                InstanceMetadata.newBuilder().setData(instanceMetadata).build());
    }

    @Override
    public void listenHeartbeat(ConsumerId consumerId, StreamObserver<HeartbeatMessage> responseObserver) {
        heartbeatListenerMap.put(consumerId.getId(), responseObserver);
        responseObserver.onNext(HeartbeatMessage.newBuilder().setBeat(DUBBO).build());
    }

    //    @Deprecated
    //    public String getAndListenInstanceMetadata(String consumerId, StreamObserver<InstanceMetadata> listener) {
    //        instanceMetadataChangedListenerMap.put(consumerId, listener);
    //        return instanceMetadata;
    //    }

    private SortedSet<String> getServiceURLs(
            Map<String, SortedSet<URL>> exportedServiceURLs, String serviceKey, String protocol) {

        SortedSet<URL> serviceURLs = exportedServiceURLs.get(serviceKey);

        if (isEmpty(serviceURLs)) {
            return emptySortedSet();
        }

        return MetadataServiceUtils.toSortedStrings(
                serviceURLs.stream().filter(url -> isAcceptableProtocol(protocol, url)));
    }

    private boolean isAcceptableProtocol(String protocol, URL url) {
        return protocol == null
                || protocol.equals(url.getParameter(PROTOCOL_KEY))
                || protocol.equals(url.getProtocol());
    }

    @Override
    public void destroy() {}

    static class URLComparator implements Comparator<URL> {

        public static final URLComparator INSTANCE = new URLComparator();

        @Override
        public int compare(URL o1, URL o2) {
            return o1.toFullString().compareTo(o2.toFullString());
        }
    }
}
