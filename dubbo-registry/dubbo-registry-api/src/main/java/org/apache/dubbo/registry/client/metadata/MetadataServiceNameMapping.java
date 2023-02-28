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
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
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
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_PROPERTY_TYPE_MISMATCH;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.registry.Constants.CAS_RETRY_TIMES_KEY;
import static org.apache.dubbo.registry.Constants.CAS_RETRY_WAIT_TIME_KEY;
import static org.apache.dubbo.registry.Constants.DEFAULT_CAS_RETRY_TIMES;
import static org.apache.dubbo.registry.Constants.DEFAULT_CAS_RETRY_WAIT_TIME;

public class MetadataServiceNameMapping extends AbstractServiceNameMapping {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private static final List<String> IGNORED_SERVICE_INTERFACES = Collections.singletonList(MetadataService.class.getName());

    private final int casRetryTimes;
    private final int casRetryWaitTime;
    protected MetadataReportInstance metadataReportInstance;

    public MetadataServiceNameMapping(ApplicationModel applicationModel) {
        super(applicationModel);
        metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
        casRetryTimes = ConfigurationUtils.getGlobalConfiguration(applicationModel).getInt(CAS_RETRY_TIMES_KEY, DEFAULT_CAS_RETRY_TIMES);
        casRetryWaitTime = ConfigurationUtils.getGlobalConfiguration(applicationModel).getInt(CAS_RETRY_WAIT_TIME_KEY, DEFAULT_CAS_RETRY_WAIT_TIME);
    }

    @Override
    public boolean hasValidMetadataCenter() {
        return !CollectionUtils.isEmpty(applicationModel.getApplicationConfigManager().getMetadataConfigs());
    }

    /**
     * Simply register to all metadata center
     */
    @Override
    public boolean map(URL url) {
        if (CollectionUtils.isEmpty(applicationModel.getApplicationConfigManager().getMetadataConfigs())) {
            logger.warn(COMMON_PROPERTY_TYPE_MISMATCH, "", "", "No valid metadata config center found for mapping report.");
            return false;
        }
        String serviceInterface = url.getServiceInterface();
        if (IGNORED_SERVICE_INTERFACES.contains(serviceInterface)) {
            return true;
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

                boolean succeeded = false;
                int currentRetryTimes = 1;
                String newConfigContent = appName;
                do {
                    ConfigItem configItem = metadataReport.getConfigItem(serviceInterface, DEFAULT_MAPPING_GROUP);
                    String oldConfigContent = configItem.getContent();
                    if (StringUtils.isNotEmpty(oldConfigContent)) {
                        String[] oldAppNames = oldConfigContent.split(",");
                        if (oldAppNames.length > 0) {
                            for (String oldAppName : oldAppNames) {
                                if (oldAppName.equals(appName)) {
                                    succeeded = true;
                                    break;
                                }
                            }
                        }
                        if (succeeded) {
                            break;
                        }
                        newConfigContent = oldConfigContent + COMMA_SEPARATOR + appName;
                    }
                    succeeded = metadataReport.registerServiceAppMapping(serviceInterface, DEFAULT_MAPPING_GROUP, newConfigContent, configItem.getTicket());
                    if (!succeeded) {
                        int waitTime = ThreadLocalRandom.current().nextInt(casRetryWaitTime);
                        logger.info("Failed to publish service name mapping to metadata center by cas operation. " +
                            "Times: " + currentRetryTimes + ". " +
                            "Next retry delay: " + waitTime + ". " +
                            "Service Interface: " + serviceInterface + ". " +
                            "Origin Content: " + oldConfigContent + ". " +
                            "Ticket: " + configItem.getTicket() + ". " +
                            "Excepted context: " + newConfigContent);
                        Thread.sleep(waitTime);
                    }
                } while (!succeeded && currentRetryTimes++ <= casRetryTimes);

                if (!succeeded) {
                    result = false;
                }
            } catch (Exception e) {
                result = false;
                logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", "Failed registering mapping to remote." + metadataReport, e);
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
