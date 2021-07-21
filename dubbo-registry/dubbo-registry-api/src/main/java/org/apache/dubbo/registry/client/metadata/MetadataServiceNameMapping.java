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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.AbstractServiceNameMapping;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.registry.client.RegistryClusterIdentifier;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.rpc.model.ApplicationModel.getName;

public class MetadataServiceNameMapping extends AbstractServiceNameMapping {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final List<String> IGNORED_SERVICE_INTERFACES = Collections.singletonList(MetadataService.class.getName());

    private static final int CAS_RETRY_TIMES = 6;

    @Override
    public void map(URL url) {
        execute(() -> {
            if (CollectionUtils.isEmpty(ApplicationModel.getConfigManager().getMetadataConfigs())) {
                return;
            }
            String serviceInterface = url.getServiceInterface();
            if (IGNORED_SERVICE_INTERFACES.contains(serviceInterface)) {
                return;
            }
            String registryCluster = getRegistryCluster(url);
            MetadataReport metadataReport = MetadataReportInstance.getMetadataReport(registryCluster);

            if (metadataReport.registerServiceAppMapping(serviceInterface, getName(), url)) {
                // MetadataReport support directly register service-app mapping
                return;
            }

            int currentRetryTimes = 1;
            boolean success;
            String newConfigContent = getName();
            do {
                ConfigItem configItem = metadataReport.getConfigItem(serviceInterface, DEFAULT_MAPPING_GROUP);
                String oldConfigContent = configItem.getContent();
                if (StringUtils.isNotEmpty(oldConfigContent)) {
                    boolean contains = StringUtils.isContains(oldConfigContent, getName());
                    if (contains) {
                        break;
                    }
                    newConfigContent = oldConfigContent + COMMA_SEPARATOR + getName();
                }
                success = metadataReport.registerServiceAppMapping(serviceInterface, DEFAULT_MAPPING_GROUP, newConfigContent, configItem.getTicket());
            } while (!success && currentRetryTimes++ <= CAS_RETRY_TIMES);
        });
    }

    @Override
    public Set<String> get(URL url) {
        Set<String> serviceNames = new LinkedHashSet<>();
        execute(() -> {
            String serviceInterface = url.getServiceInterface();
            String registryCluster = getRegistryCluster(url);
            MetadataReport metadataReport = MetadataReportInstance.getMetadataReport(registryCluster);
            Set<String> apps = metadataReport.getServiceAppMapping(serviceInterface, url);
            serviceNames.addAll(apps);
        });
        return serviceNames;
    }

    @Override
    public Set<String> getAndListen(URL url, MappingListener mappingListener) {
        Set<String> serviceNames = new LinkedHashSet<>();
        execute(() -> {
            String serviceInterface = url.getServiceInterface();
            String registryCluster = getRegistryCluster(url);
            MetadataReport metadataReport = MetadataReportInstance.getMetadataReport(registryCluster);
            Set<String> apps = metadataReport.getServiceAppMapping(serviceInterface, mappingListener, url);
            serviceNames.addAll(apps);
        });
        return serviceNames;
    }

    protected String getRegistryCluster(URL url) {
        String registryCluster = RegistryClusterIdentifier.getExtension(url).providerKey(url);
        if (registryCluster == null) {
            registryCluster = DEFAULT_KEY;
        }
        int i = registryCluster.indexOf(",");
        if (i > 0) {
            registryCluster = registryCluster.substring(0, i);
        }
        return registryCluster;
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage(), e);
            }
        }
    }
}
