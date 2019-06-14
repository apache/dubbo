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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.dubbo.common.URL.buildKey;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;

/**
 * The {@link WritableMetadataService} implementation stores the metadata of Dubbo services in memory locally when they
 * exported.
 *
 * @see MetadataService
 * @see WritableMetadataService
 * @since 2.7.3
 */
public class InMemoryWritableMetadataService implements WritableMetadataService {

    /**
     * The class name of {@link MetadataService}
     */
    static final String METADATA_SERVICE_CLASS_NAME = MetadataService.class.getName();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Lock lock = new ReentrantLock();

    // =================================== Registration =================================== //

    /**
     * All exported {@link URL urls} {@link Map} whose key is the return value of {@link URL#getServiceKey()} method
     * and value is the {@link List} of the {@link URL URLs}
     */
    private ConcurrentMap<String, List<URL>> exportedServiceURLs = new ConcurrentHashMap<>();

    // ==================================================================================== //

    // =================================== Subscription =================================== //

    /**
     * The subscribed {@link URL urls} {@link Map} of {@link MetadataService},
     * whose key is the return value of {@link URL#getServiceKey()} method and value is the {@link List} of
     * the {@link URL URLs}
     */
    private final ConcurrentMap<String, List<URL>> subscribedServiceURLs = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> serviceDefinitions = new ConcurrentHashMap<>();

    // ==================================================================================== //

    @Override
    public List<String> getSubscribedURLs() {
        return getAllUnmodifiableServiceURLs(subscribedServiceURLs);
    }

    @Override
    public List<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        if (ALL_SERVICE_INTERFACES.equals(serviceInterface)) {
            return getAllUnmodifiableServiceURLs(exportedServiceURLs);
        }
        String serviceKey = buildKey(serviceInterface, group, version);
        return unmodifiableList(getServiceURLs(exportedServiceURLs, serviceKey, protocol));
    }

    @Override
    public boolean exportURL(URL url) {
//        if (isMetadataServiceURL(url)) { // ignore MetadataService in the export phase
//            return true;
//        }
        return addURL(exportedServiceURLs, url);
    }

    @Override
    public boolean unexportURL(URL url) {
//        if (isMetadataServiceURL(url)) { // ignore MetadataService in the export phase
//            return true;
//        }
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

    private boolean addURL(Map<String, List<URL>> serviceURLs, URL url) {
        return executeMutually(() -> {
            List<URL> urls = serviceURLs.computeIfAbsent(url.getServiceKey(), s -> new LinkedList());
            if (!urls.contains(url)) {
                return urls.add(url);
            }
            return false;
        });
    }

    private boolean removeURL(Map<String, List<URL>> serviceURLs, URL url) {
        return executeMutually(() -> {
            List<URL> urls = serviceURLs.get(url.getServiceKey());
            if (isEmpty(urls)) {
                return false;
            }
            return urls.remove(url);
        });
    }

    private boolean executeMutually(Callable<Boolean> callable) {
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

    private static List<String> getServiceURLs(Map<String, List<URL>> exportedServiceURLs, String serviceKey,
                                               String protocol) {
        List<URL> serviceURLs = exportedServiceURLs.get(serviceKey);

        if (isEmpty(serviceURLs)) {
            return emptyList();
        }

        return serviceURLs
                .stream()
                .filter(url -> isAcceptableProtocol(protocol, url))
                .map(URL::toFullString)
                .collect(Collectors.toList());
    }

    private static boolean isAcceptableProtocol(String protocol, URL url) {
        return protocol == null
                || protocol.equals(url.getParameter(PROTOCOL_KEY))
                || protocol.equals(url.getProtocol());
    }

//    private static boolean isMetadataServiceURL(URL url) {
//        String serviceInterface = url.getServiceInterface();
//        return METADATA_SERVICE_CLASS_NAME.equals(serviceInterface);
//    }

    private static List<String> getAllUnmodifiableServiceURLs(Map<String, List<URL>> serviceURLs) {
        return unmodifiableList(
                serviceURLs
                        .values()
                        .stream()
                        .flatMap(Collection::stream)
                        .map(URL::toFullString)
                        .collect(Collectors.toList()));
    }
}
