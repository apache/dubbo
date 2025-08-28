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
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.registry.client.RegistryClusterIdentifier;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
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

    private final int casRetryTimes;
    private final int casRetryWaitTime;

    public MetadataServiceNameMapping(ApplicationModel applicationModel) {
        super(applicationModel);
        casRetryTimes = ConfigurationUtils.getGlobalConfiguration(applicationModel)
                .getInt(CAS_RETRY_TIMES_KEY, DEFAULT_CAS_RETRY_TIMES);
        casRetryWaitTime = ConfigurationUtils.getGlobalConfiguration(applicationModel)
                .getInt(CAS_RETRY_WAIT_TIME_KEY, DEFAULT_CAS_RETRY_WAIT_TIME);
    }

    @Override
    public boolean hasValidMetadataCenter() {
        return !CollectionUtils.isEmpty(
                applicationModel.getApplicationConfigManager().getMetadataConfigs());
    }

    @Override
    protected boolean doMap(MetadataReport metadataReport, URL url) {
        boolean succeeded = false;
        int currentRetryTimes = 1;
        try {
            do {
                succeeded = registerServiceAppMapping(metadataReport, url);
                if (succeeded) {
                    logger.info(
                            "[METADATA_REGISTER] [SERVICE_NAME_MAPPING] Successfully registered interface application mapping for service "
                                    + url.getServiceKey());
                    break;
                } else {
                    int waitTime = ThreadLocalRandom.current().nextInt(casRetryWaitTime);
                    logger.info("Failed to publish service name mapping to metadata center by cas operation. "
                            + "Times: " + currentRetryTimes + ". "
                            + "Next retry delay: " + waitTime + ". "
                            + "Service Interface: " + url.getServiceInterface() + ". ");
                    Thread.sleep(waitTime);
                }
            } while (currentRetryTimes++ <= casRetryTimes);
        } catch (Exception e) {
            logger.warn(
                    INTERNAL_ERROR,
                    "unknown error in registry module",
                    "",
                    "Failed registering mapping to remote." + metadataReport,
                    e);
        }
        return succeeded;
    }

    @Override
    public Set<String> get(URL url) {
        String serviceInterface = url.getServiceInterface();
        String registryCluster = getRegistryCluster(url);
        MetadataReport metadataReport = metadataReportInstance.getMetadataReport(registryCluster);
        if (metadataReport == null || !metadataReport.isAvailable()) {
            return Collections.emptySet();
        }
        return metadataReport.getServiceAppMapping(serviceInterface, url);
    }

    @Override
    public Set<String> getAndListen(URL url, MappingListener mappingListener) {
        String serviceInterface = url.getServiceInterface();
        // randomly pick one metadata report is ok for it's guaranteed all metadata report will have the same mapping
        // data.
        String registryCluster = getRegistryCluster(url);
        MetadataReport metadataReport = metadataReportInstance.getMetadataReport(registryCluster);
        if (metadataReport == null || !metadataReport.isAvailable()) {
            return Collections.emptySet();
        }
        return metadataReport.getServiceAppMapping(serviceInterface, mappingListener, url);
    }

    @Override
    protected void removeListener(URL url, MappingListener mappingListener) {
        String serviceInterface = url.getServiceInterface();
        // randomly pick one metadata report is ok for it's guaranteed each metadata report will have the same mapping
        // content.
        String registryCluster = getRegistryCluster(url);
        MetadataReport metadataReport = metadataReportInstance.getMetadataReport(registryCluster);
        if (metadataReport == null || !metadataReport.isAvailable()) {
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
