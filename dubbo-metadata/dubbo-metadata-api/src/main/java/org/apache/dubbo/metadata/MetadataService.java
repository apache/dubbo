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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.unmodifiableSortedSet;
import static java.util.stream.StreamSupport.stream;
import static org.apache.dubbo.common.URL.buildKey;

/**
 * A framework interface of Dubbo Metadata Service defines the contract of Dubbo Services registartion and subscription
 * between Dubbo service providers and its consumers. The implementation will be exported as a normal Dubbo service that
 * the clients would subscribe, whose version comes from the {@link #version()} method and group gets from
 * {@link #serviceName()}, that means, The different Dubbo service(application) will export the different
 * {@link MetadataService} that persists all the exported and subscribed metadata, they are present by
 * {@link #getExportedURLs()} and {@link #getSubscribedURLs()} respectively. What's more, {@link MetadataService}
 * also providers the fine-grain methods for the precise queries.
 *
 * @see WritableMetadataService
 * @since 2.7.5
 */
public interface MetadataService {

    //FIXME the value is default, it was used by testing temporarily
    static final String DEFAULT_EXTENSION = "default";

    /**
     * The value of all service names
     */
    String ALL_SERVICE_NAMES = "*";

    /**
     * The value of All service instances
     */
    String ALL_SERVICE_INTERFACES = "*";

    /**
     * The service interface name of {@link MetadataService}
     */
    String SERVICE_INTERFACE_NAME = MetadataService.class.getName();

    /**
     * The contract version of {@link MetadataService}, the future update must make sure compatible.
     */
    String VERSION = "1.0.0";

    /**
     * Gets the current Dubbo Service name
     *
     * @return non-null
     */
    String serviceName();

    /**
     * Gets the version of {@link MetadataService} that always equals {@link #VERSION}
     *
     * @return non-null
     * @see #VERSION
     */
    default String version() {
        return VERSION;
    }

    /**
     * the list of String that presents all Dubbo subscribed {@link URL urls}
     *
     * @return the non-null read-only {@link SortedSet sorted set} of {@link URL#toFullString() strings} presenting the {@link URL URLs}
     * @see #toSortedStrings(Stream)
     * @see URL#toFullString()
     */
    default SortedSet<String> getSubscribedURLs() {
        throw new UnsupportedOperationException("This operation is not supported for consumer.");
    }

    /**
     * Get the {@link SortedSet sorted set} of String that presents all Dubbo exported {@link URL urls}
     *
     * @return the non-null read-only {@link SortedSet sorted set} of {@link URL#toFullString() strings} presenting the {@link URL URLs}
     * @see #toSortedStrings(Stream)
     * @see URL#toFullString()
     */
    default SortedSet<String> getExportedURLs() {
        return getExportedURLs(ALL_SERVICE_INTERFACES);
    }

    /**
     * Get the {@link SortedSet sorted set} of String that presents the specified Dubbo exported {@link URL urls} by the <code>serviceInterface</code>
     *
     * @param serviceInterface The class name of Dubbo service interface
     * @return the non-null read-only {@link SortedSet sorted set} of {@link URL#toFullString() strings} presenting the {@link URL URLs}
     * @see #toSortedStrings(Stream)
     * @see URL#toFullString()
     */
    default SortedSet<String> getExportedURLs(String serviceInterface) {
        return getExportedURLs(serviceInterface, null);
    }

    /**
     * Get the {@link SortedSet sorted set} of String that presents the specified Dubbo exported {@link URL urls} by the
     * <code>serviceInterface</code> and <code>group</code>
     *
     * @param serviceInterface The class name of Dubbo service interface
     * @param group            the Dubbo Service Group (optional)
     * @return the non-null read-only {@link SortedSet sorted set} of {@link URL#toFullString() strings} presenting the {@link URL URLs}
     * @see #toSortedStrings(Stream)
     * @see URL#toFullString()
     */
    default SortedSet<String> getExportedURLs(String serviceInterface, String group) {
        return getExportedURLs(serviceInterface, group, null);
    }

    /**
     * Get the {@link SortedSet sorted set} of String that presents the specified Dubbo exported {@link URL urls} by the
     * <code>serviceInterface</code>, <code>group</code> and <code>version</code>
     *
     * @param serviceInterface The class name of Dubbo service interface
     * @param group            the Dubbo Service Group (optional)
     * @param version          the Dubbo Service Version (optional)
     * @return the non-null read-only {@link SortedSet sorted set} of {@link URL#toFullString() strings} presenting the {@link URL URLs}
     * @see #toSortedStrings(Stream)
     * @see URL#toFullString()
     */
    default SortedSet<String> getExportedURLs(String serviceInterface, String group, String version) {
        return getExportedURLs(serviceInterface, group, version, null);
    }

    /**
     * Get the sorted set of String that presents the specified Dubbo exported {@link URL urls} by the
     * <code>serviceInterface</code>, <code>group</code>, <code>version</code> and <code>protocol</code>
     *
     * @param serviceInterface The class name of Dubbo service interface
     * @param group            the Dubbo Service Group (optional)
     * @param version          the Dubbo Service Version (optional)
     * @param protocol         the Dubbo Service Protocol (optional)
     * @return the non-null read-only {@link SortedSet sorted set} of {@link URL#toFullString() strings} presenting the {@link URL URLs}
     * @see #toSortedStrings(Stream)
     * @see URL#toFullString()
     */
    SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol);

    /**
     * Interface definition.
     *
     * @return
     */
    default String getServiceDefinition(String interfaceName, String version, String group) {
        return getServiceDefinition(buildKey(interfaceName, group, version));
    }

    /**
     * Interface definition.
     *
     * @return
     */
    String getServiceDefinition(String serviceKey);

    MetadataInfo getMetadataInfo(String revision);

    Map<String, MetadataInfo> getMetadataInfos();

    /**
     * Is the {@link URL} for the {@link MetadataService} or not?
     *
     * @param url {@link URL url}
     * @return
     */
    static boolean isMetadataServiceURL(URL url) {
        String serviceInterface = url.getServiceInterface();
        return SERVICE_INTERFACE_NAME.equals(serviceInterface);
    }

    /**
     * Convert the multiple {@link URL urls} to a {@link List list} of {@link URL urls}
     *
     * @param urls the strings presents the {@link URL Dubbo URLs}
     * @return non-null
     */
    static List<URL> toURLs(Iterable<String> urls) {
        return stream(urls.spliterator(), false)
                .map(URL::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Convert the specified {@link Iterable} of {@link URL URLs} to be the {@link URL#toFullString() strings} presenting
     * the {@link URL URLs}
     *
     * @param iterable {@link Iterable} of {@link URL}
     * @return the non-null read-only {@link SortedSet sorted set} of {@link URL#toFullString() strings} presenting
     * @see URL#toFullString()
     */
    static SortedSet<String> toSortedStrings(Iterable<URL> iterable) {
        return toSortedStrings(StreamSupport.stream(iterable.spliterator(), false));
    }

    /**
     * Convert the specified {@link Stream} of {@link URL URLs} to be the {@link URL#toFullString() strings} presenting
     * the {@link URL URLs}
     *
     * @param stream {@link Stream} of {@link URL}
     * @return the non-null read-only {@link SortedSet sorted set} of {@link URL#toFullString() strings} presenting
     * @see URL#toFullString()
     */
    static SortedSet<String> toSortedStrings(Stream<URL> stream) {
        return unmodifiableSortedSet(stream.map(URL::toFullString).collect(TreeSet::new, Set::add, Set::addAll));
    }

    /**
     * Export Metadata in Service Instance of Service Discovery
     * <p>
     * Used for consumer to get Service Instance Metadata
     * if Registry is unsupported with publishing metadata
     *
     * @param metadata {@link Map} of provider Service Instance Metadata
     * @since 3.0
     */
    default void exportServiceDiscoveryMetadata(String metadata) {
        throw new UnsupportedOperationException("This operation is not supported for consumer.");
    }

    /**
     * Get all Metadata listener from local
     * <p>
     * Used for consumer to get Service Instance Metadata
     * if Registry is unsupported with publishing metadata
     *
     * @return {@link Map} of {@link MetadataChangeListener}
     * @since 3.0
     */
    default Map<String, MetadataChangeListener> getMetadataChangeListenerMap() {
        throw new UnsupportedOperationException("This operation is not supported for consumer.");
    }

    /**
     * 1. Fetch Metadata in Service Instance of Service Discovery
     * 2. Add a metadata change listener
     * <p>
     * Used for consumer to get Service Instance Metadata
     * if Registry is unsupported with publishing metadata
     *
     * @param consumerId consumerId
     * @param listener   {@link MetadataChangeListener} used to notify event
     * @return {@link Map} of provider Service Instance Metadata
     * @since 3.0
     */
    default String getAndListenServiceDiscoveryMetadata(String consumerId, MetadataChangeListener listener) {
        throw new UnsupportedOperationException("This operation is not supported for consumer.");
    }
}
