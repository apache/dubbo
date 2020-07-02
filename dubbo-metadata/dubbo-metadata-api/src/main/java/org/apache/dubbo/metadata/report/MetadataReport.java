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
package org.apache.dubbo.metadata.report;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.URLRevisionResolver;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;

import com.google.gson.Gson;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySortedSet;
import static org.apache.dubbo.rpc.model.ApplicationModel.getName;

/**
 * The interface to report the metadata
 *
 * @see AutoCloseable since 2.7.8
 */
public interface MetadataReport extends AutoCloseable {

    void storeProviderMetadata(MetadataIdentifier providerMetadataIdentifier, ServiceDefinition serviceDefinition);

    void storeConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, Map<String, String> serviceParameterMap);

    /**
     * @deprecated 2.7.8
     */
    @Deprecated
    default void saveServiceMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url) {
    }

    /**
     * @deprecated 2.7.8
     */
    @Deprecated
    default void removeServiceMetadata(ServiceMetadataIdentifier metadataIdentifier) {

    }

    /**
     * @deprecated 2.7.8
     */
    @Deprecated
    default List<String> getExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        return emptyList();
    }

    void saveSubscribedData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, Collection<String> urls);

    Collection<String> getSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier);

    String getServiceDefinition(MetadataIdentifier metadataIdentifier);

    /**
     * Save the exported {@link URL#toFullString() strings} presenting the {@link URL URLs} in bulk.
     *
     * @param exportedURLs the exported {@link URL urls}
     * @return If successful, return <code>true</code>, or <code>false</code>
     * @since 2.7.8
     */
    default boolean saveExportedURLs(SortedSet<String> exportedURLs) {
        return saveExportedURLs(getName(), exportedURLs);
    }

    /**
     * Save the exported {@link URL#toFullString() strings} presenting the {@link URL URLs} in bulk.
     *
     * @param serviceName  the specified Dubbo service name
     * @param exportedURLs the exported {@link URL urls}
     * @return If successful, return <code>true</code>, or <code>false</code>
     * @since 2.7.8
     */
    default boolean saveExportedURLs(String serviceName, SortedSet<String> exportedURLs) {
        return saveExportedURLs(serviceName, new URLRevisionResolver().resolve(exportedURLs), exportedURLs);
    }

    /**
     * Save the exported {@link URL#toFullString() strings} presenting the {@link URL URLs} in bulk.
     *
     * @param serviceName              the specified Dubbo service name
     * @param exportedServicesRevision the revision of the exported Services
     * @param exportedURLs             the exported {@link URL urls}
     * @return If successful, return <code>true</code>, or <code>false</code>
     * @since 2.7.8
     */
    default boolean saveExportedURLs(String serviceName, String exportedServicesRevision, SortedSet<String> exportedURLs) {
        Gson gson = new Gson();
        String content = gson.toJson(exportedURLs);
        return saveExportedURLs(serviceName, exportedServicesRevision, content);
    }

    /**
     * Save the exported {@link URL#toFullString() strings} presenting the {@link URL URLs} in bulk.
     *
     * @param serviceName              the specified Dubbo service name
     * @param exportedServicesRevision the revision of the exported Services
     * @param exportedURLsContent      the content of the exported {@link URL urls}
     * @return If successful, return <code>true</code>, or <code>false</code>
     * @since 2.7.8
     */
    default boolean saveExportedURLs(String serviceName, String exportedServicesRevision, String exportedURLsContent) {
        return true;
    }

    /**
     * Get the {@link URL#toFullString() strings} presenting the {@link URL URLs} that were exported by the provider
     *
     * @param serviceName              the specified Dubbo service name
     * @param exportedServicesRevision the revision of the exported Services
     * @return non-null
     * @since 2.7.8
     */
    default SortedSet<String> getExportedURLs(String serviceName, String exportedServicesRevision) {
        String exportedURLsContent = getExportedURLsContent(serviceName, exportedServicesRevision);
        if (StringUtils.isBlank(exportedURLsContent)) {
            return emptySortedSet();
        }
        Gson gson = new Gson();
        return gson.fromJson(exportedURLsContent, TreeSet.class);
    }

    /**
     * Get the {@link URL#toFullString() strings} presenting the {@link URL URLs} that were exported by the provider
     *
     * @param serviceName              the specified Dubbo service name
     * @param exportedServicesRevision the revision of the exported Services
     * @return the content of the exported {@link URL urls} if found, or <code>null</code>
     * @since 2.7.8
     */
    default String getExportedURLsContent(String serviceName, String exportedServicesRevision) {
        return null;
    }

}