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

import org.apache.dubbo.common.Constants;
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
import org.apache.dubbo.rpc.RpcException;

import java.util.function.Supplier;

/**
 * MetadataReportService
 *
 * <p>to publish provider {@link FullServiceDefinition} and consumer define</p>
 *
 * @since 2.7.0
 */
public class MetadataReportService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static volatile MetadataReportService metadataReportService;

    private static final Object LOCK = new Object();

    MetadataReport metadataReport;

    URL metadataReportUrl;

    MetadataReportService(URL metadataReportURL) {
        if (Constants.METADATA_REPORT_KEY.equals(metadataReportURL.getProtocol())) {
            String protocol = metadataReportURL.getParameter(Constants.METADATA_REPORT_KEY, Constants.DEFAULT_DIRECTORY);
            metadataReportURL = URLBuilder.from(metadataReportURL)
                    .setProtocol(protocol)
                    .removeParameter(Constants.METADATA_REPORT_KEY)
                    .build();
        }
        this.metadataReportUrl = metadataReportURL;
        MetadataReportFactory metadataReportFactory = ExtensionLoader.getExtensionLoader(MetadataReportFactory.class).getAdaptiveExtension();
        metadataReport = metadataReportFactory.getMetadataReport(this.metadataReportUrl);

    }


    public static MetadataReportService instance(Supplier<URL> metadataReportUrl) {
        if (metadataReportService == null) {
            synchronized (LOCK) {
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
        // first add into the list
        // remove the individul param
        providerUrl = removeIndividulParameters(providerUrl);

        try {
            String interfaceName = providerUrl.getParameter(Constants.INTERFACE_KEY);
            if (StringUtils.isNotEmpty(interfaceName)) {
                Class interfaceClass = Class.forName(interfaceName);
                FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, providerUrl.getParameters());
                metadataReport.storeProviderMetadata(generateMetadataIdentifierByURL(providerUrl, Constants.PROVIDER_SIDE), fullServiceDefinition);
                return;
            }
            logger.error("publishProvider interfaceName is empty . providerUrl: " + providerUrl.toFullString());
        } catch (ClassNotFoundException e) {
            //ignore error
            logger.error("publishProvider getServiceDescriptor error. providerUrl: " + providerUrl.toFullString(), e);
        }
    }




    public void publishConsumer(URL consumerURL) throws RpcException {
        consumerURL = removeIndividulParameters(consumerURL);
        metadataReport.storeConsumerMetadata(generateMetadataIdentifierByURL(consumerURL, Constants.CONSUMER_SIDE),
                consumerURL.getParameters());
    }

    /**
     * according to {@link URL} to generate {@link MetadataIdentifier}
     *
     * @param url {@link URL}
     * @param side provider/consumer
     * @return MetadataIdentifier
     */
    private MetadataIdentifier generateMetadataIdentifierByURL(URL url, String side) {
        return new MetadataIdentifier(url.getServiceInterface(),
                url.getParameter(Constants.VERSION_KEY), url.getParameter(Constants.GROUP_KEY),
                side, url.getParameter(Constants.APPLICATION_KEY));
    }

    /**
     * remove the individul param
     *
     * <p>such as {pid_key, timestamp, etc..}, those keys do not need to store metadata center,
     * because it can not to help user to know something. </p>
     *
     * @param url {@link URL}
     * @return url
     */
    private URL removeIndividulParameters(URL url) {
        return url.removeParameters(Constants.PID_KEY, Constants.TIMESTAMP_KEY, Constants.BIND_IP_KEY, Constants.BIND_PORT_KEY, Constants.TIMESTAMP_KEY);
    }

}
