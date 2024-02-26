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
package org.apache.dubbo.metadata.util;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataInfo.ServiceInfo;
import org.apache.dubbo.metadata.MetadataInfoV2;
import org.apache.dubbo.metadata.ServiceInfoV2;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.dubbo.common.constants.CommonConstants.TRIPLE;

public class MetadataReportVersionUtils {

    public static MetadataInfoV2 toV2(MetadataInfo metadataInfo) {
        if (metadataInfo == null) {
            return MetadataInfoV2.newBuilder().build();
        }
        Map<String, ServiceInfoV2> servicesV2 = new HashMap<>();

        metadataInfo.getServices().forEach((name, serviceInfo) -> servicesV2.put(name, toV2(serviceInfo)));
        return MetadataInfoV2.newBuilder()
                .setVersion(metadataInfo.getRevision())
                .setApp(metadataInfo.getApp())
                .putAllServices(servicesV2)
                .build();
    }

    public static ServiceInfoV2 toV2(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return ServiceInfoV2.newBuilder().build();
        }
        return ServiceInfoV2.newBuilder()
                .setVersion(serviceInfo.getVersion())
                .setGroup(serviceInfo.getGroup())
                .setName(serviceInfo.getName())
                .setPort(serviceInfo.getPort())
                .setPath(serviceInfo.getPath())
                .setProtocol(serviceInfo.getProtocol())
                .putAllParams(serviceInfo.getAllParams())
                .build();
    }

    public static boolean needExportV1(ApplicationModel applicationModel) {
        return !onlyExportV2(applicationModel);
    }

    public static boolean needExportV2(ApplicationModel applicationModel) {
        Optional<MetadataReportConfig> metadataConfigManager = getMetadataReportConfig(applicationModel);

        return onlyExportV2(applicationModel)
                || metadataConfigManager.isPresent()
                        && TRIPLE.equals(metadataConfigManager.get().getProtocol());
    }

    public static boolean onlyExportV2(ApplicationModel applicationModel) {
        Optional<MetadataReportConfig> metadataReportConfig = getMetadataReportConfig(applicationModel);

        return metadataReportConfig
                .filter(config -> config.getOnlyUseMetadataV2() && TRIPLE.equals(config.getProtocol()))
                .isPresent();
    }

    public static Optional<MetadataReportConfig> getMetadataReportConfig(ApplicationModel applicationModel) {
        Optional<ConfigManager> configManager = Optional.ofNullable(applicationModel.getApplicationConfigManager());

        if (configManager.isPresent()
                && CollectionUtils.isNotEmpty(configManager.get().getMetadataConfigs())) {
            return Optional.of(
                    configManager.get().getMetadataConfigs().iterator().next());
        }
        return Optional.empty();
    }
}
