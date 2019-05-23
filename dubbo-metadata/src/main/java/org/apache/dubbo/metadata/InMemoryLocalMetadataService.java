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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static org.apache.dubbo.common.URL.buildKey;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;

/**
 * The {@link LocalMetadataService} implementation stores the metadata of Dubbo services in memory locally when they
 * exported.
 *
 * @see MetadataService
 * @since 2.7.2
 */
public class InMemoryLocalMetadataService implements LocalMetadataService {

    /**
     * The class name of {@link MetadataService}
     */
    static final String METADATA_SERVICE_CLASS_NAME = MetadataService.class.getName();

    // =================================== Registration =================================== //

    /**
     * All exported {@link URL urls} {@link Map} whose key is the return value of {@link URL#getServiceKey()} method
     * and value is the {@link List} of the {@link URL URLs}
     */
    private ConcurrentMap<String, List<URL>> exportedServiceURLs = new ConcurrentHashMap<>();

    // ==================================================================================== //

    // =================================== Subscription =================================== //

    /**
     * All subscribed service names
     */
    private Set<String> subscribedServices = new LinkedHashSet<>();

    /**
     * The subscribed {@link URL urls} {@link Map} of {@link MetadataService},
     * whose key is the return value of {@link URL#getServiceKey()} method and value is the {@link List} of
     * the {@link URL URLs}
     */
    private final ConcurrentMap<String, List<URL>> subscribedServiceURLs = new ConcurrentHashMap<>();

    // ==================================================================================== //

    @Override
    public List<String> getSubscribedURLs() {
        return getAllServiceURLs(subscribedServiceURLs);
    }

    @Override
    public List<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        if (ALL_SERVICE_INTERFACES.equals(serviceInterface)) {
            return getAllServiceURLs(exportedServiceURLs);
        }
        String serviceKey = buildKey(serviceInterface, group, version);
        return unmodifiableList(getServiceURLs(exportedServiceURLs, serviceKey, protocol));
    }

    protected List<String> getServiceURLs(ConcurrentMap<String, List<URL>> exportedServiceURLs, String serviceKey,
                                          String protocol) {
        List<URL> serviceURLs = getServiceURLs(exportedServiceURLs, serviceKey);
        return serviceURLs.stream().filter(
                url -> protocol == null || protocol.equals(url.getParameter(PROTOCOL_KEY)))
                .map(URL::toFullString)
                .collect(Collectors.toList());
    }


    private boolean isMetadataServiceURL(URL url) {
        String serviceInterface = url.getServiceInterface();
        return METADATA_SERVICE_CLASS_NAME.equals(serviceInterface);
    }

    @Override
    public boolean exportURL(URL url) {
        if (isMetadataServiceURL(url)) { // ignore MetadataService in the export phase
            return true;
        }
        return addURL(exportedServiceURLs, url);
    }

    @Override
    public boolean unexportURL(URL url) {
        if (isMetadataServiceURL(url)) { // ignore MetadataService in the export phase
            return true;
        }
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

    protected boolean addURL(Map<String, List<URL>> serviceURLs, URL url) {
        String serviceKey = url.getServiceKey();
        List<URL> urls = getServiceURLs(serviceURLs, serviceKey);
        if (!urls.contains(url)) {
            return urls.add(url);
        }
        return false;
    }

    protected boolean removeURL(Map<String, List<URL>> serviceURLs, URL url) {
        String serviceKey = url.getServiceKey();
        List<URL> urls = getServiceURLs(serviceURLs, serviceKey);
        return urls.remove(url);
    }

    protected List<URL> getServiceURLs(Map<String, List<URL>> serviceURLs, String serviceKey) {
        return serviceURLs.computeIfAbsent(serviceKey, s -> new LinkedList());
    }

    protected List<String> getAllServiceURLs(Map<String, List<URL>> serviceURLs) {
        return serviceURLs
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(URL::toFullString)
                .collect(Collectors.toList());
    }
}
