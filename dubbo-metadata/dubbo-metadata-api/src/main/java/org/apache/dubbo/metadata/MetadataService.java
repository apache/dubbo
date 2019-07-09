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
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;

/**
 * A framework interface of Dubbo Metadata Service defines the contract of Dubbo Services registartion and subscription
 * between Dubbo service providers and its consumers. The implementationwill be exported as a normal Dubbo service that
 * the clients would subscribe, whose version comes from the {@link #version()} method and group gets from
 * {@link #serviceName()}, that means, The different Dubbo service(application) will export the different
 * {@link MetadataService} that persists all the exported and subscribed metadata, they are present by
 * {@link #getExportedURLs()} and {@link #getSubscribedURLs()} respectively. What's more, {@link MetadataService}
 * also providers the fine-grain methods for the precise queries.
 *
 * @see WritableMetadataService
 * @since 2.7.4
 */
public interface MetadataService {

    /**
     * The value of all service names
     */
    String ALL_SERVICE_NAMES = "*";

    /**
     * The value of All service instances
     */
    String ALL_SERVICE_INTERFACES = "*";

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
     * @return non-null read-only {@link List}
     */
    List<String> getSubscribedURLs();

    /**
     * Get the list of String that presents all Dubbo exported {@link URL urls}
     *
     * @return non-null read-only {@link List}
     */
    default List<String> getExportedURLs() {
        return getExportedURLs(ALL_SERVICE_INTERFACES);
    }

    /**
     * Get the list of String that presents the specified Dubbo exported {@link URL urls} by the <code>serviceInterface</code>
     *
     * @param serviceInterface The class name of Dubbo service interface
     * @return non-null read-only {@link List}
     * @see URL
     */
    default List<String> getExportedURLs(String serviceInterface) {
        return getExportedURLs(serviceInterface, null);
    }

    /**
     * Get the list of String that presents the specified Dubbo exported {@link URL urls} by the
     * <code>serviceInterface</code> and <code>group</code>
     *
     * @param serviceInterface The class name of Dubbo service interface
     * @param group            the Dubbo Service Group (optional)
     * @return non-null read-only {@link List}
     * @see URL
     */
    default List<String> getExportedURLs(String serviceInterface, String group) {
        return getExportedURLs(serviceInterface, group, null);
    }

    /**
     * Get the list of String that presents the specified Dubbo exported {@link URL urls} by the
     * <code>serviceInterface</code>, <code>group</code> and <code>version</code>
     *
     * @param serviceInterface The class name of Dubbo service interface
     * @param group            the Dubbo Service Group (optional)
     * @param version          the Dubbo Service Version (optional)
     * @return non-null read-only {@link List}
     * @see URL
     */
    default List<String> getExportedURLs(String serviceInterface, String group, String version) {
        return getExportedURLs(serviceInterface, group, version, null);
    }

    /**
     * Get the list of String that presents the specified Dubbo exported {@link URL urls} by the
     * <code>serviceInterface</code>, <code>group</code>, <code>version</code> and <code>protocol</code>
     *
     * @param serviceInterface The class name of Dubbo service interface
     * @param group            the Dubbo Service Group (optional)
     * @param version          the Dubbo Service Version (optional)
     * @param protocol         the Dubbo Service Protocol (optional)
     * @return non-null read-only {@link List}
     * @see URL
     */
    List<String> getExportedURLs(String serviceInterface, String group, String version, String protocol);

    /**
     * Interface definition.
     * @return
     */
    String getServiceDefinition(String interfaceName, String version, String group);

    /**
     * Interface definition.
     * @return
     */
    String getServiceDefinition(String serviceKey);

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
}
