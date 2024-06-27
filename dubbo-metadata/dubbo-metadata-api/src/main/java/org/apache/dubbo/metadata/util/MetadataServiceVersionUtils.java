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
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataInfo.ServiceInfo;
import org.apache.dubbo.metadata.MetadataInfoV2;
import org.apache.dubbo.metadata.MetadataServiceV2Detector;
import org.apache.dubbo.metadata.ServiceInfoV2;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.dubbo.common.constants.CommonConstants.TRIPLE;

public class MetadataServiceVersionUtils {

    public static final String V1 = "1.0.0";

    public static final String V2 = "2.0.0";

    public static MetadataInfoV2 toV2(MetadataInfo metadataInfo) {
        if (metadataInfo == null) {
            return MetadataInfoV2.newBuilder().build();
        }
        Map<String, ServiceInfoV2> servicesV2 = new HashMap<>();

        metadataInfo.getServices().forEach((name, serviceInfo) -> servicesV2.put(name, toV2(serviceInfo)));
        return MetadataInfoV2.newBuilder()
                .setVersion(ifNullSetEmpty(metadataInfo.getRevision()))
                .setApp(ifNullSetEmpty(metadataInfo.getApp()))
                .putAllServices(servicesV2)
                .build();
    }

    public static ServiceInfoV2 toV2(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return ServiceInfoV2.newBuilder().build();
        }
        return ServiceInfoV2.newBuilder()
                .setVersion(ifNullSetEmpty(serviceInfo.getVersion()))
                .setGroup(ifNullSetEmpty(serviceInfo.getGroup()))
                .setName(ifNullSetEmpty(serviceInfo.getName()))
                .setPort(serviceInfo.getPort())
                .setPath(ifNullSetEmpty(serviceInfo.getPath()))
                .setProtocol(ifNullSetEmpty(serviceInfo.getProtocol()))
                .putAllParams(serviceInfo.getAllParams())
                .build();
    }

    private static String ifNullSetEmpty(String value) {
        return value == null ? "" : value;
    }

    public static MetadataInfo toV1(MetadataInfoV2 metadataInfoV2) {
        Map<String, ServiceInfoV2> servicesV2Map = metadataInfoV2.getServicesMap();
        Map<String, ServiceInfo> serviceMap = new HashMap<>(servicesV2Map.size());
        servicesV2Map.forEach((s, serviceInfoV2) -> serviceMap.put(s, toV1(serviceInfoV2)));
        return new MetadataInfo(metadataInfoV2.getApp(), metadataInfoV2.getVersion(), serviceMap);
    }

    public static ServiceInfo toV1(ServiceInfoV2 serviceInfoV2) {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setGroup(serviceInfoV2.getGroup());
        serviceInfo.setVersion(serviceInfoV2.getVersion());
        serviceInfo.setName(serviceInfoV2.getName());
        serviceInfo.setPort(serviceInfoV2.getPort());
        serviceInfo.setParams(serviceInfoV2.getParamsMap());
        serviceInfo.setProtocol(serviceInfoV2.getProtocol());
        serviceInfo.setPath(serviceInfoV2.getPath());
        return serviceInfo;
    }

    /**
     * check if we should export MetadataService
     */
    public static boolean needExportV1(ApplicationModel applicationModel) {
        return !MetadataServiceV2Detector.support() || !onlyExportV2(applicationModel);
    }

    /**
     * check if we should export MetadataServiceV2
     */
    public static boolean needExportV2(ApplicationModel applicationModel) {
        return MetadataServiceV2Detector.support()
                && (onlyExportV2(applicationModel) || tripleConfigured(applicationModel));
    }

    /**
     * check if we should only export MetadataServiceV2
     */
    public static boolean onlyExportV2(ApplicationModel applicationModel) {
        Optional<ApplicationConfig> applicationConfig = getApplicationConfig(applicationModel);
        return applicationConfig
                .filter(config ->
                        Boolean.TRUE.equals(config.getOnlyUseMetadataV2()) && tripleConfigured(applicationModel))
                .isPresent();
    }

    /**
     * check if we can use triple as MetadataService protocol
     */
    public static boolean tripleConfigured(ApplicationModel applicationModel) {
        Optional<ConfigManager> configManager = Optional.ofNullable(applicationModel.getApplicationConfigManager());

        Optional<ApplicationConfig> appConfig = getApplicationConfig(applicationModel);

        // if user configured MetadataService protocol
        if (appConfig.isPresent() && appConfig.get().getMetadataServiceProtocol() != null) {
            return TRIPLE.equals(appConfig.get().getMetadataServiceProtocol());
        }
        // if not specified, check all protocol configs
        if (configManager.isPresent()
                && CollectionUtils.isNotEmpty(configManager.get().getProtocols())) {
            Collection<ProtocolConfig> protocols = configManager.get().getProtocols();
            for (ProtocolConfig protocolConfig : protocols) {
                if (TRIPLE.equals(protocolConfig.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Optional<ApplicationConfig> getApplicationConfig(ApplicationModel applicationModel) {
        Optional<ConfigManager> configManager = Optional.ofNullable(applicationModel.getApplicationConfigManager());

        if (configManager.isPresent() && configManager.get().getApplication().isPresent()) {
            return configManager.get().getApplication();
        }
        return Optional.empty();
    }
}
