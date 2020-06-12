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

package org.apache.dubbo.metadata.store.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.report.identifier.BaseMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReport;
import org.apache.dubbo.metadata.report.support.ConfigCenterBasedMetadataReport;
import org.apache.dubbo.rpc.RpcException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * metadata report impl for nacos
 *
 * @deprecated 2.7.8 This class will be removed in the future, {@link ConfigCenterBasedMetadataReport} as a substitute.
 */
@Deprecated
public class NacosMetadataReport extends AbstractMetadataReport {

    private static final Logger logger = LoggerFactory.getLogger(NacosMetadataReport.class);

    private final DynamicConfiguration dynamicConfiguration;

    /**
     * The group used to store metadata in Nacos
     */
    private String group;

    public NacosMetadataReport(URL url, DynamicConfiguration dynamicConfiguration) {
        super(url);
        this.dynamicConfiguration = dynamicConfiguration;
    }

    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        this.storeMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String value) {
        this.storeMetadata(consumerMetadataIdentifier, value);
    }

    @Override
    protected void doSaveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier, URL url) {
        storeMetadata(serviceMetadataIdentifier, URL.encode(url.toFullString()));
    }

    @Override
    protected void doRemoveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier) {
        deleteMetadata(serviceMetadataIdentifier);
    }

    @Override
    protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        String content = getConfig(metadataIdentifier);
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(Arrays.asList(URL.decode(content)));
    }

    @Override
    protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urlListStr) {
        storeMetadata(subscriberMetadataIdentifier, urlListStr);
    }

    @Override
    protected String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        return getConfig(subscriberMetadataIdentifier);
    }

    @Override
    public String getServiceDefinition(MetadataIdentifier metadataIdentifier) {
        return getConfig(metadataIdentifier);
    }

    @Override
    public boolean saveExportedURLs(String serviceName, String exportedServicesRevision, String exportedURLsContent) {
        return dynamicConfiguration.publishConfig(serviceName, exportedServicesRevision, exportedURLsContent);
    }

    @Override
    public String getExportedURLsContent(String serviceName, String exportedServicesRevision) {
        return dynamicConfiguration.getConfig(serviceName, exportedServicesRevision, SECONDS.toMillis(3));
    }

    private void storeMetadata(BaseMetadataIdentifier identifier, String value) {
        try {
            boolean publishResult = dynamicConfiguration.publishConfig(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), group, value);
            if (!publishResult) {
                throw new RuntimeException("publish nacos metadata failed");
            }
        } catch (Throwable t) {
            logger.error("Failed to put " + identifier + " to nacos " + value + ", cause: " + t.getMessage(), t);
            throw new RpcException("Failed to put " + identifier + " to nacos " + value + ", cause: " + t.getMessage(), t);
        }
    }

    private void deleteMetadata(BaseMetadataIdentifier identifier) {
        try {
            boolean publishResult = dynamicConfiguration.removeConfig(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), group);
            if (!publishResult) {
                throw new RuntimeException("remove nacos metadata failed");
            }
        } catch (Throwable t) {
            logger.error("Failed to remove " + identifier + " from nacos , cause: " + t.getMessage(), t);
            throw new RpcException("Failed to remove " + identifier + " from nacos , cause: " + t.getMessage(), t);
        }
    }

    private String getConfig(BaseMetadataIdentifier identifier) {
        try {
            return dynamicConfiguration.getConfig(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), group, 300);
        } catch (Throwable t) {
            logger.error("Failed to get " + identifier + " from nacos , cause: " + t.getMessage(), t);
            throw new RpcException("Failed to get " + identifier + " from nacos , cause: " + t.getMessage(), t);
        }
    }
}
