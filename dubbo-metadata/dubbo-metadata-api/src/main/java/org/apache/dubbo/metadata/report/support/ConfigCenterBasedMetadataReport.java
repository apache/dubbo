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
package org.apache.dubbo.metadata.report.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.identifier.BaseMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;

import java.util.List;

import static org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory.getDynamicConfigurationFactory;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.metadata.MetadataConstants.EXPORTED_URLS_TAG;

/**
 * The generic implementation of {@link MetadataReport} based on {@link DynamicConfiguration
 * the config-center infrastructure}
 *
 * @see AbstractMetadataReport
 * @since 2.7.8
 */
public class ConfigCenterBasedMetadataReport extends AbstractMetadataReport {

    private final KeyTypeEnum keyType;

    private final String group;

    private final DynamicConfiguration dynamicConfiguration;

    public ConfigCenterBasedMetadataReport(URL reportServerURL, KeyTypeEnum keyTypeEnum) {
        super(reportServerURL);
        this.keyType = keyTypeEnum;
        this.group = reportServerURL.getParameter(GROUP_KEY, DEFAULT_ROOT);
        String extensionName = reportServerURL.getProtocol();
        DynamicConfigurationFactory dynamicConfigurationFactory = getDynamicConfigurationFactory(extensionName);
        dynamicConfiguration = dynamicConfigurationFactory.getDynamicConfiguration(reportServerURL);
    }


    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        saveMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String serviceParameterString) {
        saveMetadata(consumerMetadataIdentifier, serviceParameterString);
    }

    @Override
    protected void doSaveMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url) {
        saveMetadata(metadataIdentifier, URL.encode(url.toFullString()));
    }

    @Override
    protected void doRemoveMetadata(ServiceMetadataIdentifier metadataIdentifier) {
        removeMetadata(metadataIdentifier);
    }

    @Override
    protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        throw new UnsupportedOperationException("doGetExportedURLs method will not be supported!");
    }

    @Override
    protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urlListStr) {
        saveMetadata(subscriberMetadataIdentifier, urlListStr);
    }

    @Override
    protected String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        return getMetadata(subscriberMetadataIdentifier);
    }

    @Override
    public String getServiceDefinition(MetadataIdentifier metadataIdentifier) {
        return getMetadata(metadataIdentifier);
    }

    @Override
    public boolean saveExportedURLs(String serviceName, String exportedServicesRevision, String exportedURLsContent) {
        String key = buildExportedURLsMetadataKey(serviceName, exportedServicesRevision);
        return dynamicConfiguration.publishConfig(key, group, exportedURLsContent);
    }

    @Override
    public String getExportedURLsContent(String serviceName, String exportedServicesRevision) {
        String key = buildExportedURLsMetadataKey(serviceName, exportedServicesRevision);
        return dynamicConfiguration.getConfig(key, group);
    }

    private String buildExportedURLsMetadataKey(String serviceName, String exportedServicesRevision) {
        return keyType.build(EXPORTED_URLS_TAG, serviceName, exportedServicesRevision);
    }

    protected void saveMetadata(BaseMetadataIdentifier metadataIdentifier, String value) {
        String key = getKey(metadataIdentifier);
        dynamicConfiguration.publishConfig(key, group, value);
    }

    protected void saveMetadata(MetadataIdentifier metadataIdentifier, String value) {
        String key = getKey(metadataIdentifier);
        dynamicConfiguration.publishConfig(key, group, value);
    }

    protected String getMetadata(ServiceMetadataIdentifier metadataIdentifier) {
        String key = getKey(metadataIdentifier);
        return dynamicConfiguration.getConfig(key, group);
    }

    protected String getMetadata(MetadataIdentifier metadataIdentifier) {
        String key = getKey(metadataIdentifier);
        return dynamicConfiguration.getConfig(key, group);
    }

    protected String getMetadata(SubscriberMetadataIdentifier metadataIdentifier) {
        String key = getKey(metadataIdentifier);
        return dynamicConfiguration.getConfig(key, group);
    }

    protected void removeMetadata(MetadataIdentifier metadataIdentifier) {
        String key = getKey(metadataIdentifier);
        dynamicConfiguration.removeConfig(key, group);
    }

    protected void removeMetadata(ServiceMetadataIdentifier metadataIdentifier) {
        String key = getKey(metadataIdentifier);
        dynamicConfiguration.removeConfig(key, group);
    }

    protected String getKey(BaseMetadataIdentifier metadataIdentifier) {
        return metadataIdentifier.getUniqueKey(keyType);
    }

    protected String getKey(MetadataIdentifier metadataIdentifier) {
        return metadataIdentifier.getUniqueKey(keyType);
    }

    protected void doClose() throws Exception {
        this.dynamicConfiguration.close();
    }
}
