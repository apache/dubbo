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
package org.apache.dubbo.metadata.report;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataPublisher;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_CREATE_INSTANCE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_LOAD_METADATA;

public class DefaultMetadataPublisher implements ApplicationDeployListener , MetadataPublisher {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DefaultMetadataPublisher.class);

    @Override
    public void onStarting(ApplicationModel scopeModel) {
        scopeModel.getBeanFactory().registerBean(this);
    }

    @Override
    public void publishServiceDefinition(URL url, ServiceDescriptor serviceDescriptor, ApplicationModel applicationModel) {
        if (getMetadataReports(applicationModel).isEmpty()) {
            String msg = "Remote Metadata Report Server is not provided or unavailable, will stop registering service definition to remote center!";
            logger.warn(REGISTRY_FAILED_LOAD_METADATA, "", "", msg);
            return;
        }

        try {
            String side = url.getSide();
            if (PROVIDER_SIDE.equalsIgnoreCase(side)) {
                String serviceKey = url.getServiceKey();
                FullServiceDefinition serviceDefinition = serviceDescriptor.getFullServiceDefinition(serviceKey);

                if (StringUtils.isNotEmpty(serviceKey) && serviceDefinition != null) {
                    serviceDefinition.setParameters(url.getParameters());
                    for (Map.Entry<String, MetadataReport> entry : getMetadataReports(applicationModel).entrySet()) {
                        MetadataReport metadataReport = entry.getValue();
                        if (!metadataReport.shouldReportDefinition()) {
                            logger.info("Report of service definition is disabled for " + entry.getKey());
                            continue;
                        }
                        metadataReport.storeProviderMetadata(
                                new MetadataIdentifier(
                                        url.getServiceInterface(),
                                        url.getVersion() == null ? "" : url.getVersion(),
                                        url.getGroup() == null ? "" : url.getGroup(),
                                        PROVIDER_SIDE,
                                        applicationModel.getApplicationName())
                                , serviceDefinition);
                    }
                }
            } else {
                for (Map.Entry<String, MetadataReport> entry : getMetadataReports(applicationModel).entrySet()) {
                    MetadataReport metadataReport = entry.getValue();
                    if (!metadataReport.shouldReportDefinition()) {
                        logger.info("Report of service definition is disabled for " + entry.getKey());
                        continue;
                    }
                    metadataReport.storeConsumerMetadata(
                            new MetadataIdentifier(
                                    url.getServiceInterface(),
                                    url.getVersion() == null ? "" : url.getVersion(),
                                    url.getGroup() == null ? "" : url.getGroup(),
                                    CONSUMER_SIDE,
                                    applicationModel.getApplicationName()),
                            url.getParameters());
                }
            }
        } catch (Exception e) {
            //ignore error
            logger.error(REGISTRY_FAILED_CREATE_INSTANCE, "", "", "publish service definition metadata error.", e);
        }
    }


    private static Map<String, MetadataReport> getMetadataReports(ApplicationModel applicationModel) {
        return applicationModel.getBeanFactory().getBean(MetadataReportInstance.class).getMetadataReports(false);
    }

    @Override
    public void onInitialize(ApplicationModel scopeModel) {}

    @Override
    public void onStarted(ApplicationModel scopeModel) {}

    @Override
    public void onStopping(ApplicationModel scopeModel) {}

    @Override
    public void onStopped(ApplicationModel scopeModel) {}

    @Override
    public void onFailure(ApplicationModel scopeModel, Throwable cause) {}

}
