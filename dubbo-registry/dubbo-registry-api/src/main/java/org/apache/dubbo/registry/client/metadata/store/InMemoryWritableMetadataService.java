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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataInfo.ServiceInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.registry.client.RegistryClusterIdentifier;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import com.google.gson.Gson;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Collections.emptySortedSet;
import static java.util.Collections.unmodifiableSortedSet;
import static org.apache.dubbo.common.URL.buildKey;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;

/**
 * The {@link WritableMetadataService} implementation stores the metadata of Dubbo services in memory locally when they
 * exported. It is used by server (provider).
 *
 * @see MetadataService
 * @see WritableMetadataService
 * @since 2.7.5
 */
public class InMemoryWritableMetadataService implements WritableMetadataService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private final Lock lock = new ReentrantLock();

    // =================================== Registration =================================== //

    /**
     * All exported {@link URL urls} {@link Map} whose key is the return value of {@link URL#getServiceKey()} method
     * and value is the {@link SortedSet sorted set} of the {@link URL URLs}
     */
    ConcurrentNavigableMap<String, SortedSet<URL>> exportedServiceURLs = new ConcurrentSkipListMap<>();
    ConcurrentMap<String, MetadataInfo> metadataInfos;
    final Semaphore metadataSemaphore = new Semaphore(1);

    // ==================================================================================== //

    // =================================== Subscription =================================== //

    /**
     * The subscribed {@link URL urls} {@link Map} of {@link MetadataService},
     * whose key is the return value of {@link URL#getServiceKey()} method and value is
     * the {@link SortedSet sorted set} of the {@link URL URLs}
     */
    ConcurrentNavigableMap<String, SortedSet<URL>> subscribedServiceURLs = new ConcurrentSkipListMap<>();

    ConcurrentNavigableMap<String, String> serviceDefinitions = new ConcurrentSkipListMap<>();

    public InMemoryWritableMetadataService() {
        this.metadataInfos = new ConcurrentHashMap<>();
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
    public boolean exportURL(URL url) {
        String registryCluster = RegistryClusterIdentifier.getExtension(url).providerKey(url);
        String[] clusters = registryCluster.split(",");
        for (String cluster : clusters) {
            MetadataInfo metadataInfo = metadataInfos.computeIfAbsent(cluster, k -> {
                return new MetadataInfo(ApplicationModel.getName());
            });
            metadataInfo.addService(new ServiceInfo(url));
        }
        metadataSemaphore.release();
        return addURL(exportedServiceURLs, url);
    }

    @Override
    public boolean unexportURL(URL url) {
        String registryCluster = RegistryClusterIdentifier.getExtension(url).providerKey(url);
        String[] clusters = registryCluster.split(",");
        for (String cluster : clusters) {
            MetadataInfo metadataInfo = metadataInfos.get(cluster);
            metadataInfo.removeService(url.getProtocolServiceKey());
            if (metadataInfo.getServices().isEmpty()) {
                metadataInfos.remove(cluster);
            }
        }
        metadataSemaphore.release();
        return removeURL(exportedServiceURLs, url);
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
    public void publishServiceDefinition(URL providerUrl) {
        try {
            if (!ProtocolUtils.isGeneric(providerUrl.getParameter(GENERIC_KEY))) {
                String interfaceName = providerUrl.getParameter(INTERFACE_KEY);
                if (StringUtils.isNotEmpty(interfaceName)) {
                    Class interfaceClass = Class.forName(interfaceName);
                    ServiceDefinition serviceDefinition = ServiceDefinitionBuilder.build(interfaceClass);
                    Gson gson = new Gson();
                    String data = gson.toJson(serviceDefinition);
                    serviceDefinitions.put(providerUrl.getServiceKey(), data);
                    return;
                }
                logger.error("publishProvider interfaceName is empty . providerUrl: " + providerUrl.toFullString());
            } else if (CONSUMER_SIDE.equalsIgnoreCase(providerUrl.getParameter(SIDE_KEY))) {
                //to avoid consumer generic invoke style error
                return;
            }
        } catch (ClassNotFoundException e) {
            //ignore error
            logger.error("publishProvider getServiceDescriptor error. providerUrl: " + providerUrl.toFullString(), e);
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
        return null;
    }

    public void blockUntilUpdated() {
        try {
            metadataSemaphore.acquire();
        } catch (InterruptedException e) {
            logger.warn("metadata refresh thread has been interrupted unexpectedly while wating for update.", e);
        }
    }

    public Map<String, MetadataInfo> getMetadataInfos() {
        return metadataInfos;
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
