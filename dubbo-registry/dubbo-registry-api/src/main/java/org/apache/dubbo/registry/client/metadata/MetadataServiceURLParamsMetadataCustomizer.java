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

import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;

import java.util.Map;
import java.util.SortedSet;

import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.metadata.MetadataService.toURLs;
import static org.apache.dubbo.metadata.WritableMetadataService.getDefaultExtension;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getMetadataServiceParameter;

public class MetadataServiceURLParamsMetadataCustomizer implements ServiceInstanceCustomizer {

    /**
     * 元数据服务对应的参数
     * dubbo.metadata-service.url-params={"dubbo":{"version":"1.0.0","dubbo":"2.0.2","port":"20881"}}
     * @param serviceInstance {@link ServiceInstance the service instance}
     */
    @Override
    public void customize(ServiceInstance serviceInstance) {

        Map<String, String> metadata = serviceInstance.getMetadata();

        //dubbo.metadata-service.url-params
        String propertyName = resolveMetadataPropertyName(serviceInstance);
        /**
         * 获取元数据服务的特定参数
         * {"dubbo":{"version":"1.0.0","dubbo":"2.0.2","port":"20881"}}
         */
        String propertyValue = resolveMetadataPropertyValue(serviceInstance);

        if (!isBlank(propertyName) && !isBlank(propertyValue)) {
            metadata.put(propertyName, propertyValue);
        }
    }

    private String resolveMetadataPropertyName(ServiceInstance serviceInstance) {
        return METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME;
    }

    /**
     * 获取本地元数据服务的参数
     * @param serviceInstance the instance of {@link ServiceInstance}
     * @return
     */
    private String resolveMetadataPropertyValue(ServiceInstance serviceInstance) {
        // 获取WritableMetadataService默认实现
        WritableMetadataService writableMetadataService = getDefaultExtension();

        String serviceInterface = MetadataService.class.getName();

        String group = serviceInstance.getServiceName();

        String version = MetadataService.VERSION;
        // 获取本地元数据中心中 元数据服务的url
        SortedSet<String> urls = writableMetadataService.getExportedURLs(serviceInterface, group, version);
        //获取元数据服务对应的参数
        return getMetadataServiceParameter(toURLs(urls));
    }
}
