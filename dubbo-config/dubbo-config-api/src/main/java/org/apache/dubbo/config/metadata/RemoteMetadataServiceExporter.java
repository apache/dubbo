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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.metadata.MetadataServiceType;
import org.apache.dubbo.metadata.URLRevisionResolver;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;

import java.util.SortedSet;

import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;

/**
 * The implementation of {@link MetadataServiceExporter} for
 * {@link CommonConstants#REMOTE_METADATA_STORAGE_TYPE "remote" metadata storage type}
 *
 * @see MetadataServiceExporter
 * @since 2.7.8
 */
public class RemoteMetadataServiceExporter extends AbstractMetadataServiceExporter {

    private final URLRevisionResolver urlRevisionResolver;

    public RemoteMetadataServiceExporter() {
        super(REMOTE_METADATA_STORAGE_TYPE, MIN_PRIORITY, MetadataServiceType.REMOTE, MetadataServiceType.COMPOSITE);
        this.urlRevisionResolver = URLRevisionResolver.INSTANCE;
    }

    @Override
    protected void doExport() throws Exception {
        // WritableMetadataService的默认实现  local   InMemoryWritableMetadataService
        WritableMetadataService metadataServiceDelegate = WritableMetadataService.getDefaultExtension();

        /**
         * 向配置中心写入本地元数据中心中  对外暴露的服务url集合
         * exported-urls:dubbo-demo-annotation-provider:3b46c4664c6d5a
         * 其中3b46c4664c6d5a为导出服务url得hashcode
         * 对应配置中心得内容为导出服务的url列表
         */
        if (publishServiceMetadata(metadataServiceDelegate)) {
            /**
             * 向配置中心写入本地元数据中心中  订阅的服务url集合
             * dubbo-nacos-provider-demo:X
             * 其中X为订阅服务url对应的hashcode【当前没有订阅服务所以为X】
             * 对应配置中心得内容为订阅服务的url列表
             */
            publicConsumerMetadata(metadataServiceDelegate);
        }
    }

    /**
     * 向配置中心写入本地元数据中心中  对外暴露的服务hashcode
     * @param metadataServiceDelegate
     * @return
     */
    private boolean publishServiceMetadata(WritableMetadataService metadataServiceDelegate) {
        String serviceName = metadataServiceDelegate.serviceName();
        /**
         * 本地元数据中心   对外暴露的服务  不包含元数据服务
         */
        SortedSet<String> exportedURLs = metadataServiceDelegate.getExportedURLs();
        // 计算hashcode
        String revision = urlRevisionResolver.resolve(exportedURLs);
        // 写入配置中心
        return getMetadataReport().saveExportedURLs(serviceName, revision, exportedURLs);
    }

    /**
     * 向配置中心写入本地元数据中心中  订阅的服务hashcode
     * @param metadataServiceDelegate
     * @return
     */
    private boolean publicConsumerMetadata(WritableMetadataService metadataServiceDelegate) {
        String serviceName = metadataServiceDelegate.serviceName();
        /**
         * 向配置中心写入本地元数据中心中  订阅的服务
         */
        SortedSet<String> subscribedURLs = metadataServiceDelegate.getSubscribedURLs();

        // 计算hashcode
        String revision = urlRevisionResolver.resolve(subscribedURLs);
        // 写入配置中心  AbstractMetadataReport
        getMetadataReport().saveSubscribedData(new SubscriberMetadataIdentifier(serviceName, revision), subscribedURLs);
        return true;
    }

    private MetadataReport getMetadataReport() {
        return MetadataReportInstance.getMetadataReport(true);
    }

    @Override
    protected void doUnexport() throws Exception {
        // DOES NOTHING
    }
}
