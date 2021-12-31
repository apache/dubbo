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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;

public class MetadataServiceNameMapping extends AbstractServiceNameMapping {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final List<String> IGNORED_SERVICE_INTERFACES = Collections.singletonList(MetadataService.class.getName());

    private static final int CAS_RETRY_TIMES = 6;
    protected MetadataReportInstance metadataReportInstance;

    public MetadataServiceNameMapping(ApplicationModel applicationModel) {
        metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
    }

    /**
     * Simply register to all metadata center
     */
    @Override
    public boolean map(URL url) {
        if (CollectionUtils.isEmpty(applicationModel.getApplicationConfigManager().getMetadataConfigs())) {
            return false;
        }
        String serviceInterface = url.getServiceInterface();
        if (IGNORED_SERVICE_INTERFACES.contains(serviceInterface)) {
            return false;
        }

        boolean result = true;
        for (Map.Entry<String, MetadataReport> entry : metadataReportInstance.getMetadataReports(true).entrySet()) {
            MetadataReport metadataReport = entry.getValue();
            String appName = applicationModel.getApplicationName();
            try {
                if (metadataReport.registerServiceAppMapping(serviceInterface, appName, url)) {
                    // MetadataReport support directly register service-app mapping
                    continue;
                }

                boolean succeeded;
                int currentRetryTimes = 1;
                String newConfigContent = appName;
                do {
                    ConfigItem configItem = metadataReport.getConfigItem(serviceInterface, DEFAULT_MAPPING_GROUP);
                    String oldConfigContent = configItem.getContent();
                    if (StringUtils.isNotEmpty(oldConfigContent)) {
                        boolean contains = StringUtils.isContains(oldConfigContent, appName);
                        if (contains) {
                            // From the user's perspective, it means successful when the oldConfigContent has contained the current appName. So we should not throw an Exception to user, it will confuse the user.
                            succeeded = true;
                            break;
                        }
                        newConfigContent = oldConfigContent + COMMA_SEPARATOR + appName;
                    }
                    succeeded = metadataReport.registerServiceAppMapping(serviceInterface, DEFAULT_MAPPING_GROUP, newConfigContent, configItem.getTicket());
                } while (!succeeded && currentRetryTimes++ <= CAS_RETRY_TIMES);

                if (!succeeded) {
                    result = false;
                }
            } catch (Exception e) {
                result = false;
                logger.warn("Failed registering mapping to remote." + metadataReport, e);
            }
        }

        return result;
    }

    @Override
    public Set<String> get(URL url) {
        String serviceInterface = url.getServiceInterface();
        String registryCluster = getRegistryCluster(url);
        MetadataReport metadataReport = metadataReportInstance.getMetadataReport(registryCluster);
        if (metadataReport == null) {
            return Collections.emptySet();
        }
        return metadataReport.getServiceAppMapping(serviceInterface, url);
    }

    @Override
    public Set<String> getAndListen(URL url, MappingListener mappingListener) {
        String serviceInterface = url.getServiceInterface();
        // randomly pick one metadata report is ok for it's guaranteed all metadata report will have the same mapping data.
        String registryCluster = getRegistryCluster(url);
        MetadataReport metadataReport = metadataReportInstance.getMetadataReport(registryCluster);
        if (metadataReport == null) {
            return Collections.emptySet();
        }
        return metadataReport.getServiceAppMapping(serviceInterface, mappingListener, url);
    }

    @Override
    protected void removeListener(URL url, MappingListener mappingListener) {
        String serviceInterface = url.getServiceInterface();
        // randomly pick one metadata report is ok for it's guaranteed each metadata report will have the same mapping content.
        String registryCluster = getRegistryCluster(url);
        MetadataReport metadataReport = metadataReportInstance.getMetadataReport(registryCluster);
        if (metadataReport == null) {
            return;
        }
        metadataReport.removeServiceAppMappingListener(serviceInterface, mappingListener);
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
}
