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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.URLRevisionResolver;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;

import java.util.SortedSet;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

/**
 * The {@link WritableMetadataService} implementation stores the metadata of Dubbo services in metadata center when they
 * exported.
 * It is used by server (provider).
 *
 * @since 2.7.5
 */
public class RemoteWritableMetadataService extends AbstractAbstractWritableMetadataService {

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
    protected void publishConsumerParameters(URL consumerURL) {
        getMetadataReport().storeConsumerMetadata(new MetadataIdentifier(consumerURL.getServiceInterface(),
                consumerURL.getParameter(VERSION_KEY), consumerURL.getParameter(GROUP_KEY), CONSUMER_SIDE,
                consumerURL.getParameter(APPLICATION_KEY)), consumerURL.getParameters());
    }

    @Override
    protected void publishProviderServiceDefinition(URL providerURL) {
        try {
            String interfaceName = providerURL.getParameter(INTERFACE_KEY);
            if (StringUtils.isNotEmpty(interfaceName)) {
                Class interfaceClass = Class.forName(interfaceName);
                FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass,
                        providerURL.getParameters());
                getMetadataReport().storeProviderMetadata(new MetadataIdentifier(providerURL.getServiceInterface(),
                        providerURL.getParameter(VERSION_KEY), providerURL.getParameter(GROUP_KEY),
                        PROVIDER_SIDE, providerURL.getParameter(APPLICATION_KEY)), fullServiceDefinition);
                return;
            }
            logger.error("publishProvider interfaceName is empty . url: " + providerURL.toFullString());
        } catch (ClassNotFoundException e) {
            //ignore error
            logger.error("publishProvider getServiceDescriptor error. url: " + providerURL.toFullString(), e);
        }
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

}
