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
package org.apache.dubbo.metadata.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.store.MetadataReport;
import org.apache.dubbo.metadata.store.MetadataReportFactory;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.RpcException;

import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_DIRECTORY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.metadata.support.Constants.METADATA_REPORT_KEY;

/**
 * @since 2.7.0
 */
public class MetadataReportService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static volatile MetadataReportService metadataReportService;
    private static Object lock = new Object();

    private MetadataReportFactory metadataReportFactory = ExtensionLoader.getExtensionLoader(MetadataReportFactory.class).getAdaptiveExtension();
    MetadataReport metadataReport;
    URL metadataReportUrl;

    MetadataReportService(URL metadataReportURL) {
        if (METADATA_REPORT_KEY.equals(metadataReportURL.getProtocol())) {
            String protocol = metadataReportURL.getParameter(METADATA_REPORT_KEY, DEFAULT_DIRECTORY);
            metadataReportURL = URLBuilder.from(metadataReportURL)
                    .setProtocol(protocol)
                    .removeParameter(METADATA_REPORT_KEY)
                    .build();
        }
        this.metadataReportUrl = metadataReportURL;
        metadataReport = metadataReportFactory.getMetadataReport(this.metadataReportUrl);

    }


    public static MetadataReportService instance(Supplier<URL> metadataReportUrl) {
        if (metadataReportService == null) {
            synchronized (lock) {
                if (metadataReportService == null) {
                    URL metadataReportURLTmp = metadataReportUrl.get();
                    if (metadataReportURLTmp == null) {
                        return null;
                    }
                    metadataReportService = new MetadataReportService(metadataReportURLTmp);
                }
            }
        }
        return metadataReportService;
    }

    public void publishProvider(URL providerUrl) throws RpcException {
        //first add into the list
        // remove the individul param
        providerUrl = providerUrl.removeParameters(PID_KEY, TIMESTAMP_KEY, Constants.BIND_IP_KEY, Constants.BIND_PORT_KEY, TIMESTAMP_KEY);

        try {
            String interfaceName = providerUrl.getParameter(INTERFACE_KEY);
            if (StringUtils.isNotEmpty(interfaceName)) {
                Class interfaceClass = Class.forName(interfaceName);
                FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, providerUrl.getParameters());
                metadataReport.storeProviderMetadata(new MetadataIdentifier(providerUrl.getServiceInterface(),
                        providerUrl.getParameter(VERSION_KEY), providerUrl.getParameter(GROUP_KEY),
                        PROVIDER_SIDE, providerUrl.getParameter(APPLICATION_KEY)), fullServiceDefinition);
                return;
            }
            logger.error("publishProvider interfaceName is empty . providerUrl: " + providerUrl.toFullString());
        } catch (ClassNotFoundException e) {
            //ignore error
            logger.error("publishProvider getServiceDescriptor error. providerUrl: " + providerUrl.toFullString(), e);
        }
    }

    public void publishConsumer(URL consumerURL) throws RpcException {
        consumerURL = consumerURL.removeParameters(PID_KEY, TIMESTAMP_KEY, Constants.BIND_IP_KEY, Constants.BIND_PORT_KEY, TIMESTAMP_KEY);
        metadataReport.storeConsumerMetadata(new MetadataIdentifier(consumerURL.getServiceInterface(),
                consumerURL.getParameter(VERSION_KEY), consumerURL.getParameter(GROUP_KEY), CONSUMER_SIDE,
                consumerURL.getParameter(APPLICATION_KEY)), consumerURL.getParameters());
    }

}
