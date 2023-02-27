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
<<<<<<< HEAD
=======
import org.apache.dubbo.common.config.configcenter.ConfigItem;
>>>>>>> origin/3.2
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MetadataReport {
    /**
     * Service Definition -- START
     **/
    void storeProviderMetadata(MetadataIdentifier providerMetadataIdentifier, ServiceDefinition serviceDefinition);

    String getServiceDefinition(MetadataIdentifier metadataIdentifier);

    /**
     * Application Metadata -- START
     **/
    default void publishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
    }

<<<<<<< HEAD
=======
    default void unPublishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
    }

>>>>>>> origin/3.2
    default MetadataInfo getAppMetadata(SubscriberMetadataIdentifier identifier, Map<String, String> instanceMetadata) {
        return null;
    }

    /**
<<<<<<< HEAD
     * Service<-->Application Mapping -- START
     **/
    default Set<String> getServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        return Collections.emptySet();
    }

    default void registerServiceAppMapping(String serviceKey, String application, URL url) {
        return;
    }

    /**
     * deprecated or need triage
     **/
    void storeConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, Map<String, String> serviceParameterMap);

    List<String> getExportedURLs(ServiceMetadataIdentifier metadataIdentifier);

    void saveServiceMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url);

    void removeServiceMetadata(ServiceMetadataIdentifier metadataIdentifier);

    void saveSubscribedData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, Set<String> urls);

    List<String> getSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier);

=======
     * deprecated or need triage
     **/
    void storeConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, Map<String, String> serviceParameterMap);

    List<String> getExportedURLs(ServiceMetadataIdentifier metadataIdentifier);

    void destroy();

    void saveServiceMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url);

    void removeServiceMetadata(ServiceMetadataIdentifier metadataIdentifier);

    void saveSubscribedData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, Set<String> urls);

    List<String> getSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier);

    default ConfigItem getConfigItem(String key, String group) {
        return new ConfigItem();
    }

    default boolean registerServiceAppMapping(String serviceInterface, String defaultMappingGroup, String newConfigContent, Object ticket) {
        return false;
    }

    default boolean registerServiceAppMapping(String serviceKey, String application, URL url) {
        return false;
    }

    default void removeServiceAppMappingListener(String serviceKey, MappingListener listener) {

    }

    /**
     * Service<-->Application Mapping -- START
     **/
    default Set<String> getServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        return Collections.emptySet();
    }

    default Set<String> getServiceAppMapping(String serviceKey, URL url) {
        return Collections.emptySet();
    }

    boolean shouldReportDefinition();

    boolean shouldReportMetadata();

>>>>>>> origin/3.2
}
