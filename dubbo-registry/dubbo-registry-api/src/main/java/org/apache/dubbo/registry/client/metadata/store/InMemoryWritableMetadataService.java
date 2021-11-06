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
package org.apache.dubbo.registry.client.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.InstanceMetadataChangedListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataInfo.ServiceInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.registry.client.RegistryClusterIdentifier;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Collections.emptySortedSet;
import static java.util.Collections.unmodifiableSortedSet;
import static org.apache.dubbo.common.URL.buildKey;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_METADATA_PUBLISH_DELAY;
import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PUBLISH_DELAY_KEY;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;

/**
 * The {@link WritableMetadataService} implementation stores the metadata of Dubbo services in memory locally when they
 * exported. It is used by server (provider).
 *
 * @see MetadataService
 * @see WritableMetadataService
 * @since 2.7.5
 */
public class InMemoryWritableMetadataService implements WritableMetadataService, ScopeModelAware {

    Logger logger = LoggerFactory.getLogger(getClass());

    private final Lock lock = new ReentrantLock();

    // =================================== Registration =================================== //

    /**
     * All exported {@link URL urls} {@link Map} whose key is the return value of {@link URL#getServiceKey()} method
     * and value is the {@link SortedSet sorted set} of the {@link URL URLs}
     */
    private ConcurrentNavigableMap<String, SortedSet<URL>> exportedServiceURLs = new ConcurrentSkipListMap<>();
    private URL metadataServiceURL;
    private ConcurrentMap<String, MetadataInfo> metadataInfos;

    // used to mark whether current metadata info is being updated to registry,
    // readLock for export or unExport which are support concurrency update,
    // writeLock for ServiceInstance update which should not work during exporting services
    private final ReentrantReadWriteLock updateLock = new ReentrantReadWriteLock();
    private final Semaphore metadataSemaphore = new Semaphore(0);
    private final Map<String, Set<String>> serviceToAppsMapping = new HashMap<>();

    private String instanceMetadata;
    private ConcurrentMap<String, InstanceMetadataChangedListener> instanceMetadataChangedListenerMap = new ConcurrentHashMap<>();


    // ==================================================================================== //

    // =================================== Subscription =================================== //

    /**
     * The subscribed {@link URL urls} {@link Map} of {@link MetadataService},
     * whose key is the return value of {@link URL#getServiceKey()} method and value is
     * the {@link SortedSet sorted set} of the {@link URL URLs}
     */
    private ConcurrentNavigableMap<String, SortedSet<URL>> subscribedServiceURLs = new ConcurrentSkipListMap<>();

    private ConcurrentNavigableMap<String, String> serviceDefinitions = new ConcurrentSkipListMap<>();
    private ApplicationModel applicationModel;
    private long metadataPublishDelayTime ;

    public InMemoryWritableMetadataService() {
        this.metadataInfos = new ConcurrentHashMap<>();
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
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.metadataPublishDelayTime = ConfigurationUtils.get(applicationModel, METADATA_PUBLISH_DELAY_KEY, DEFAULT_METADATA_PUBLISH_DELAY);
    }

    @Override
    public SortedSet<String> getSubscribedURLs() {
        return getAllUnmodifiableServiceURLs(subscribedServiceURLs);
    }

    private SortedSet<String> getAllUnmodifiableServiceURLs(Map<String, SortedSet<URL>> serviceURLs) {
        SortedSet<URL> bizURLs = new TreeSet<>(InMemoryWritableMetadataService.URLComparator.INSTANCE);
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
        return MetadataService.toSortedStrings(bizURLs);
    }

    @Override
    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        if (ALL_SERVICE_INTERFACES.equals(serviceInterface)) {
            return getAllUnmodifiableServiceURLs(exportedServiceURLs);
        }
        String serviceKey = buildKey(serviceInterface, group, version);
        return unmodifiableSortedSet(getServiceURLs(exportedServiceURLs, serviceKey, protocol));
    }

    @Override
    public Set<URL> getExportedServiceURLs() {
        Set<URL> set = new HashSet<>();
        for (Map.Entry<String, SortedSet<URL>> entry : exportedServiceURLs.entrySet()) {
            set.addAll(entry.getValue());
        }
        return set;
    }

    @Override
    public boolean exportURL(URL url) {
        if (MetadataService.class.getName().equals(url.getServiceInterface())) {
            this.metadataServiceURL = url;
            return true;
        }

        updateLock.readLock().lock();
        try {
            String[] clusters = getRegistryCluster(url).split(",");
            for (String cluster : clusters) {
                MetadataInfo metadataInfo = metadataInfos.computeIfAbsent(cluster, k -> new MetadataInfo(applicationModel.getApplicationName()));
                metadataInfo.addService(new ServiceInfo(url));
            }
            metadataSemaphore.release();
            return addURL(exportedServiceURLs, url);
        } finally {
            updateLock.readLock().unlock();
        }
    }

    public void addMetadataInfo(String key, MetadataInfo metadataInfo) {
        updateLock.readLock().lock();
        try {
            metadataInfos.put(key, metadataInfo);
        } finally {
            updateLock.readLock().unlock();
        }
    }

    @Override
    public boolean unexportURL(URL url) {
        if (MetadataService.class.getName().equals(url.getServiceInterface())) {
            this.metadataServiceURL = null;
            return true;
        }

        updateLock.readLock().lock();
        try {
            String[] clusters = getRegistryCluster(url).split(",");
            for (String cluster : clusters) {
                MetadataInfo metadataInfo = metadataInfos.get(cluster);
                metadataInfo.removeService(url.getProtocolServiceKey());
//            if (metadataInfo.getServices().isEmpty()) {
//                metadataInfos.remove(cluster);
//            }
            }
            metadataSemaphore.release();
            return removeURL(exportedServiceURLs, url);
        } finally {
            updateLock.readLock().unlock();
        }
    }

    private String getRegistryCluster(URL url) {
        String registryCluster = RegistryClusterIdentifier.getExtension(url).providerKey(url);
        if (StringUtils.isEmpty(registryCluster)) {
            registryCluster = DEFAULT_KEY;
        }
        return registryCluster;
    }

    @Override
    public boolean subscribeURL(URL url) {
        return addURL(subscribedServiceURLs, url);
    }

    @Override
    public boolean unsubscribeURL(URL url) {
        return removeURL(subscribedServiceURLs, url);
    }

    @Override
    public void publishServiceDefinition(URL url) {
        try {
            String interfaceName = url.getServiceInterface();
            if (StringUtils.isNotEmpty(interfaceName)
                && !ProtocolUtils.isGeneric(url.getParameter(GENERIC_KEY))) {
                ClassLoader classLoader = url.getServiceModel() != null ?
                    url.getServiceModel().getClassLoader() :
                    ClassUtils.getClassLoader();
                Class interfaceClass = Class.forName(interfaceName, false, classLoader);
                ServiceDefinition serviceDefinition = ServiceDefinitionBuilder.build(interfaceClass);
                Gson gson = new Gson();
                String data = gson.toJson(serviceDefinition);
                serviceDefinitions.put(url.getServiceKey(), data);
                return;
            } else if (CONSUMER_SIDE.equalsIgnoreCase(url.getParameter(SIDE_KEY))) {
                //to avoid consumer generic invoke style error
                return;
            }
            logger.error("publish service definition interfaceName is empty. url: " + url.toFullString());
        } catch (Throwable e) {
            //ignore error
            logger.error("publish service definition getServiceDescriptor error. url: " + url.toFullString(), e);
        }
    }

    @Override
    public String getServiceDefinition(String interfaceName, String version, String group) {
        return serviceDefinitions.get(URL.buildKey(interfaceName, group, version));
    }

    @Override
    public String getServiceDefinition(String serviceKey) {
        return serviceDefinitions.get(serviceKey);
    }

    @Override
    public MetadataInfo getMetadataInfo(String revision) {
        if (StringUtils.isEmpty(revision)) {
            return null;
        }
        for (Map.Entry<String, MetadataInfo> entry : metadataInfos.entrySet()) {
            MetadataInfo metadataInfo = entry.getValue();
            if (revision.equals(metadataInfo.calAndGetRevision())) {
                return metadataInfo;
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("metadata not found for revision: " + revision);
        }
        return null;
    }

    @Override
    public void exportInstanceMetadata(String metadata) {
        this.instanceMetadata = metadata;
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

    @Override
    public MetadataInfo getDefaultMetadataInfo() {
        if (CollectionUtils.isEmptyMap(metadataInfos)) {
            return null;
        }
        for (Map.Entry<String, MetadataInfo> entry : metadataInfos.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(DEFAULT_KEY)) {
                return entry.getValue();
            }
        }
        return metadataInfos.entrySet().iterator().next().getValue();
    }

    public void blockUntilUpdated() {
        try {
            metadataSemaphore.tryAcquire(metadataPublishDelayTime, TimeUnit.MILLISECONDS);
            metadataSemaphore.drainPermits();
            updateLock.writeLock().lock();
        } catch (InterruptedException e) {
            if (!applicationModel.isDestroyed()) {
                logger.warn("metadata refresh thread has been interrupted unexpectedly while waiting for update.", e);
            }
        }
    }

    public void releaseBlock() {
        updateLock.writeLock().unlock();
    }

    public Map<String, MetadataInfo> getMetadataInfos() {
        return Collections.unmodifiableMap(metadataInfos);
    }

    void addMetaServiceURL(URL url) {
        this.metadataServiceURL = url;
    }

    @Override
    public URL getMetadataServiceURL() {
        return this.metadataServiceURL;
    }

    @Override
    public void putCachedMapping(String serviceKey, Set<String> apps) {
        serviceToAppsMapping.put(serviceKey, new TreeSet<>(apps));
    }

    @Override
    public Set<String> getCachedMapping(String mappingKey) {
        return serviceToAppsMapping.get(mappingKey);
    }

    @Override
    public Set<String> getCachedMapping(URL consumerURL) {
        String serviceKey = ServiceNameMapping.buildMappingKey(consumerURL);
        return serviceToAppsMapping.get(serviceKey);
    }

    @Override
    public Set<String> removeCachedMapping(String serviceKey) {
        return serviceToAppsMapping.remove(serviceKey);
    }

    @Override
    public Map<String, Set<String>> getCachedMapping() {
        return serviceToAppsMapping;
    }

    @Override
    public void setMetadataServiceURL(URL url) {
        this.metadataServiceURL = url;
    }

    boolean addURL(Map<String, SortedSet<URL>> serviceURLs, URL url) {
        return executeMutually(() -> {
            SortedSet<URL> urls = serviceURLs.computeIfAbsent(url.getServiceKey(), this::newSortedURLs);
            // make sure the parameters of tmpUrl is variable
            return urls.add(url);
        });
    }

    boolean removeURL(Map<String, SortedSet<URL>> serviceURLs, URL url) {
        return executeMutually(() -> {
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
        });
    }

    private SortedSet<URL> newSortedURLs(String serviceKey) {
        return new TreeSet<>(InMemoryWritableMetadataService.URLComparator.INSTANCE);
    }

    boolean executeMutually(Callable<Boolean> callable) {
        boolean success = false;
        try {
            lock.lock();
            try {
                success = callable.call();
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e);
                }
            }
        } finally {
            lock.unlock();
        }
        return success;
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


    static class URLComparator implements Comparator<URL> {

        public static final URLComparator INSTANCE = new URLComparator();

        @Override
        public int compare(URL o1, URL o2) {
            return o1.toFullString().compareTo(o2.toFullString());
        }
    }
}
