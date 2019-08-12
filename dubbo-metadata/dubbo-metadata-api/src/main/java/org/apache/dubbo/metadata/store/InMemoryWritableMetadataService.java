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
package org.apache.dubbo.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Collections.emptySortedSet;
import static java.util.Collections.unmodifiableSortedSet;
import static org.apache.dubbo.common.URL.buildKey;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;

/**
 * The {@link WritableMetadataService} implementation stores the metadata of Dubbo services in memory locally when they
 * exported. It is used by server (provider).
 *
 * @see MetadataService
 * @see WritableMetadataService
 * @since 2.7.4
 */
public class InMemoryWritableMetadataService implements WritableMetadataService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Lock lock = new ReentrantLock();

    // =================================== Registration =================================== //

    /**
     * All exported {@link URL urls} {@link Map} whose key is the return value of {@link URL#getServiceKey()} method
     * and value is the {@link SortedSet sorted set} of the {@link URL URLs}
     */
    ConcurrentNavigableMap<String, SortedSet<URL>> exportedServiceURLs = new ConcurrentSkipListMap<>();

    // ==================================================================================== //

    // =================================== Subscription =================================== //

    /**
     * The subscribed {@link URL urls} {@link Map} of {@link MetadataService},
     * whose key is the return value of {@link URL#getServiceKey()} method and value is
     * the {@link SortedSet sorted set} of the {@link URL URLs}
     */
    final ConcurrentNavigableMap<String, SortedSet<URL>> subscribedServiceURLs = new ConcurrentSkipListMap<>();

    final ConcurrentNavigableMap<String, String> serviceDefinitions = new ConcurrentSkipListMap<>();

    // ==================================================================================== //

    @Override
    public SortedSet<String> getSubscribedURLs() {
        return getAllUnmodifiableServiceURLs(subscribedServiceURLs);
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
        return addURL(exportedServiceURLs, url);
    }

    @Override
    public boolean unexportURL(URL url) {
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
    public boolean refreshMetadata(String revision) {
        boolean result = true;
        for (SortedSet<URL> urls : exportedServiceURLs.values()) {
            Iterator<URL> iterator = urls.iterator();
            List<URL> newList = new ArrayList<>(urls.size());
            while (iterator.hasNext()) {
                URL url = iterator.next();
                // refresh revision in urls
                Map<String, String> parameters = new HashMap<>(url.getParameters());
                parameters.put(REVISION_KEY, revision);
                URL newUrl = new URL(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), url.getPath(), parameters);
                newList.add(newUrl);
                if (!finishRefreshExportedMetadata(newUrl)) {
                    result = false;
                }
            }
            urls.clear();
            urls.addAll(newList);
        }
        return result;
    }

    protected boolean finishRefreshExportedMetadata(URL url) {
        return true;
    }

    @Override
    public void publishServiceDefinition(URL providerUrl) {
        try {
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

    boolean addURL(Map<String, SortedSet<URL>> serviceURLs, URL url) {
        return executeMutually(() -> {
            SortedSet<URL> urls = serviceURLs.computeIfAbsent(url.getServiceKey(), this::newSortedURLs);
            // make sure the parameters of tmpUrl is variable
            return urls.add(url);
        });
    }

    boolean removeURL(Map<String, SortedSet<URL>> serviceURLs, URL url) {
        return executeMutually(() -> {
            SortedSet<URL> urls = serviceURLs.getOrDefault(url.getServiceKey(), emptySortedSet());
            return urls.remove(url);
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

    private static SortedSet<String> getServiceURLs(Map<String, SortedSet<URL>> exportedServiceURLs, String serviceKey,
                                                    String protocol) {

        SortedSet<URL> serviceURLs = exportedServiceURLs.get(serviceKey);

        if (isEmpty(serviceURLs)) {
            return emptySortedSet();
        }

        return MetadataService.toSortedStrings(serviceURLs.stream().filter(url -> isAcceptableProtocol(protocol, url)));
    }

    private static boolean isAcceptableProtocol(String protocol, URL url) {
        return protocol == null
                || protocol.equals(url.getParameter(PROTOCOL_KEY))
                || protocol.equals(url.getProtocol());
    }

    private static SortedSet<String> getAllUnmodifiableServiceURLs(Map<String, SortedSet<URL>> serviceURLs) {
        return MetadataService.toSortedStrings(serviceURLs.values().stream().flatMap(Collection::stream));
    }


    static class URLComparator implements Comparator<URL> {

        public static final URLComparator INSTANCE = new URLComparator();

        @Override
        public int compare(URL o1, URL o2) {
            return o1.toFullString().compareTo(o2.toFullString());
        }
    }

}
