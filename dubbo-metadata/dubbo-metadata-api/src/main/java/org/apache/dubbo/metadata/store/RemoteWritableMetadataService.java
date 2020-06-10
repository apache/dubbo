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
package org.apache.dubbo.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.URLRevisionResolver;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.RpcException;

import java.util.SortedSet;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

/**
 * The {@link WritableMetadataService} implementation stores the metadata of Dubbo services in metadata center when they
 * exported.
 * It is used by server (provider).
 *
 * @since 2.7.5
 */
public class RemoteWritableMetadataService implements WritableMetadataService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final InMemoryWritableMetadataService writableMetadataServiceDelegate;

    private final URLRevisionResolver urlRevisionResolver;

    public RemoteWritableMetadataService() {
        this.writableMetadataServiceDelegate = (InMemoryWritableMetadataService) WritableMetadataService.getDefaultExtension();
        urlRevisionResolver = URLRevisionResolver.INSTANCE;
    }

    public MetadataReport getMetadataReport() {
        return MetadataReportInstance.getMetadataReport(true);
    }

    @Override
    public void publishServiceDefinition(URL url) {
        String side = url.getParameter(SIDE_KEY);
        if (PROVIDER_SIDE.equalsIgnoreCase(side)) {
            //TODO, the params part is duplicate with that stored by exportURL(url), can be further optimized in the future.
            publishProvider(url);
        } else {
            //TODO, only useful for ops showing the url parameters, this is duplicate with subscribeURL(url), can be removed in the future.
            publishConsumer(url);
        }
    }

    private void publishProvider(URL providerUrl) throws RpcException {
        //first add into the list
        // remove the individual param
        providerUrl = providerUrl.removeParameters(PID_KEY, TIMESTAMP_KEY, Constants.BIND_IP_KEY,
                Constants.BIND_PORT_KEY, TIMESTAMP_KEY);

        try {
            String interfaceName = providerUrl.getParameter(INTERFACE_KEY);
            if (StringUtils.isNotEmpty(interfaceName)) {
                Class interfaceClass = Class.forName(interfaceName);
                FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass,
                        providerUrl.getParameters());
                getMetadataReport().storeProviderMetadata(new MetadataIdentifier(providerUrl.getServiceInterface(),
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

    private void publishConsumer(URL consumerURL) throws RpcException {
        consumerURL = consumerURL.removeParameters(PID_KEY, TIMESTAMP_KEY, Constants.BIND_IP_KEY,
                Constants.BIND_PORT_KEY, TIMESTAMP_KEY);
        getMetadataReport().storeConsumerMetadata(new MetadataIdentifier(consumerURL.getServiceInterface(),
                consumerURL.getParameter(VERSION_KEY), consumerURL.getParameter(GROUP_KEY), CONSUMER_SIDE,
                consumerURL.getParameter(APPLICATION_KEY)), consumerURL.getParameters());
    }

    @Override
    public boolean exportURL(URL url) {
        return writableMetadataServiceDelegate.exportURL(url);
    }

    @Override
    public boolean unexportURL(URL url) {
        return writableMetadataServiceDelegate.unexportURL(url);
    }

    @Override
    public boolean subscribeURL(URL url) {
        return writableMetadataServiceDelegate.subscribeURL(url);
    }

    @Override
    public boolean unsubscribeURL(URL url) {
        return writableMetadataServiceDelegate.unsubscribeURL(url);
    }

    @Override
    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        return writableMetadataServiceDelegate.getExportedURLs(serviceInterface, group, version, protocol);
    }

    @Override
    public String getServiceDefinition(String serviceKey) {
        return writableMetadataServiceDelegate.getServiceDefinition(serviceKey);
    }

    @Override
    public SortedSet<String> getSubscribedURLs() {
        return writableMetadataServiceDelegate.getSubscribedURLs();
    }

    @Override
    public boolean publishMetadata() {
        String serviceName = writableMetadataServiceDelegate.serviceName();
        if (publishServiceMetadata(serviceName)) {
            return publicConsumerMetadata(serviceName);
        }
        return false;
    }

    protected boolean publishServiceMetadata(String serviceName) {
        SortedSet<String> exportedURLs = writableMetadataServiceDelegate.getExportedURLs();
        String revision = urlRevisionResolver.resolve(exportedURLs);
        return getMetadataReport().saveExportedURLs(serviceName, revision, exportedURLs);
    }

    protected boolean publicConsumerMetadata(String serviceName) {
        SortedSet<String> subscribedURLs = writableMetadataServiceDelegate.getSubscribedURLs();
        String revision = urlRevisionResolver.resolve(subscribedURLs);
        getMetadataReport().saveSubscribedData(new SubscriberMetadataIdentifier(serviceName, revision), subscribedURLs);
        return true;
    }
}
