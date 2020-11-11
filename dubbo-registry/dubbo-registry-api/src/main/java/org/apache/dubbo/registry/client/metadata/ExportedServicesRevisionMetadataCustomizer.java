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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.metadata.URLRevisionResolver;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceMetadataCustomizer;

import java.util.SortedSet;

import static org.apache.dubbo.metadata.WritableMetadataService.getExtension;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getMetadataStorageType;

/**
 * The customizer to a add the metadata that the reversion of Dubbo exported services calculates.
 * <p>
 * The reversion is calculated on the methods that all Dubbo exported interfaces declare
 *
 * @since 2.7.5
 */
public class ExportedServicesRevisionMetadataCustomizer extends ServiceInstanceMetadataCustomizer {

    @Override
    protected String resolveMetadataPropertyName(ServiceInstance serviceInstance) {
        return EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
    }

    @Override
    protected String resolveMetadataPropertyValue(ServiceInstance serviceInstance) {
        // 获取dubbo.metadata.storage-type   默认local
        String metadataStorageType = getMetadataStorageType(serviceInstance);
        // 获取WritableMetadataService对应实现
        WritableMetadataService writableMetadataService = getExtension(metadataStorageType);
        // 获取本地元数据中心缓存的对外服务（过滤元数据服务）
        SortedSet<String> exportedURLs = writableMetadataService.getExportedURLs();

        URLRevisionResolver resolver = new URLRevisionResolver();
        // 计算hashcode
        return resolver.resolve(exportedURLs);
    }
}
