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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;

/**
 * The {@link WritableMetadataService} implementation stores the metadata of Dubbo services in metadata center when they
 * exported.
 * It is used by server (provider).
 *
 * @since 2.7.5
 */
public class RemoteWritableMetadataService implements WritableMetadataService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private volatile String exportedRevision;
    private volatile String subscribedRevision;
    private InMemoryWritableMetadataService writableMetadataService;

    public RemoteWritableMetadataService(InMemoryWritableMetadataService writableMetadataService) {
        this.writableMetadataService = writableMetadataService;
    }

    public MetadataReport getMetadataReport() {
        return MetadataReportInstance.getMetadataReport(true);
    }

    @Override
    public void publishServiceDefinition(URL providerUrl) {
        try {
            String interfaceName = providerUrl.getParameter(INTERFACE_KEY);
            if (StringUtils.isNotEmpty(interfaceName)
                    && !ProtocolUtils.isGeneric(providerUrl.getParameter(GENERIC_KEY))) {
                Class interfaceClass = Class.forName(interfaceName);
                ServiceDefinition serviceDefinition = ServiceDefinitionBuilder.build(interfaceClass);
                getMetadataReport().storeProviderMetadata(new MetadataIdentifier(providerUrl.getServiceInterface(),
                        providerUrl.getParameter(VERSION_KEY), providerUrl.getParameter(GROUP_KEY),
                        null, null), serviceDefinition);
                return;
            }
            logger.error("publishProvider interfaceName is empty . providerUrl: " + providerUrl.toFullString());
        } catch (ClassNotFoundException e) {
            //ignore error
            logger.error("publishProvider getServiceDescriptor error. providerUrl: " + providerUrl.toFullString(), e);
        }

        // backward compatibility
        publishProvider(providerUrl);
    }

    @Deprecated
    public void publishProvider(URL providerUrl) throws RpcException {
        //first add into the list
        // remove the individul param
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

    @Deprecated
    public void publishConsumer(URL consumerURL) throws RpcException {
        consumerURL = consumerURL.removeParameters(PID_KEY, TIMESTAMP_KEY, Constants.BIND_IP_KEY,
                Constants.BIND_PORT_KEY, TIMESTAMP_KEY);
        getMetadataReport().storeConsumerMetadata(new MetadataIdentifier(consumerURL.getServiceInterface(),
                consumerURL.getParameter(VERSION_KEY), consumerURL.getParameter(GROUP_KEY), CONSUMER_SIDE,
                consumerURL.getParameter(APPLICATION_KEY)), consumerURL.getParameters());
    }

    @Override
    public boolean exportURL(URL url) {
        return true;
    }

    @Override
    public boolean unexportURL(URL url) {
        ServiceMetadataIdentifier metadataIdentifier = new ServiceMetadataIdentifier(url);
        metadataIdentifier.setRevision(exportedRevision);
        metadataIdentifier.setProtocol(url.getProtocol());
        return throwableAction(getMetadataReport()::removeServiceMetadata, metadataIdentifier);
    }

    @Override
    public boolean subscribeURL(URL url) {
        return true;
    }

    @Override
    public boolean unsubscribeURL(URL url) {
        return true;
    }

    @Override
    public boolean refreshMetadata(String exportedRevision, String subscribedRevision) {
        boolean result = true;
        if (!StringUtils.isEmpty(exportedRevision) && !exportedRevision.equals(this.exportedRevision)) {
            this.exportedRevision = exportedRevision;
            boolean executeResult = saveServiceMetadata();
            if (!executeResult) {
                result = false;
            }
        }
        if (!StringUtils.isEmpty(subscribedRevision) && !subscribedRevision.equals(this.subscribedRevision)
                && CollectionUtils.isNotEmpty(writableMetadataService.getSubscribedURLs())) {
            this.subscribedRevision = subscribedRevision;
            SubscriberMetadataIdentifier metadataIdentifier = new SubscriberMetadataIdentifier();
            metadataIdentifier.setApplication(serviceName());
            metadataIdentifier.setRevision(subscribedRevision);
            boolean executeResult = throwableAction(getMetadataReport()::saveSubscribedData, metadataIdentifier,
                    writableMetadataService.getSubscribedURLs());
            if (!executeResult) {
                result = false;
            }
        }
        return result;
    }

    private boolean saveServiceMetadata() {
        boolean result = true;
        for (SortedSet<URL> urls : writableMetadataService.exportedServiceURLs.values()) {
            Iterator<URL> iterator = urls.iterator();
            while (iterator.hasNext()) {
                URL url = iterator.next();
                // refresh revision in urls
                ServiceMetadataIdentifier metadataIdentifier = new ServiceMetadataIdentifier(url);
                metadataIdentifier.setRevision(exportedRevision);
                metadataIdentifier.setProtocol(url.getProtocol());

                boolean tmpResult = throwableAction(getMetadataReport()::saveServiceMetadata, metadataIdentifier, url);
                if (!tmpResult) result = tmpResult;
            }
        }
        return result;
    }


    @Override
    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        return null;
    }

    @Override
    public String getServiceDefinition(String interfaceName, String version, String group) {
        return null;
    }

    @Override
    public String getServiceDefinition(String serviceKey) {
        return null;
    }

    boolean throwableAction(BiConsumer<ServiceMetadataIdentifier, URL> consumer,
                            ServiceMetadataIdentifier metadataIdentifier, URL url) {
        try {
            consumer.accept(metadataIdentifier, url);
        } catch (Exception e) {
            logger.error("Failed to execute consumer, url is: " + url);
            return false;
        }
        return true;
    }

    boolean throwableAction(BiConsumer<SubscriberMetadataIdentifier, Set<String>> consumer,
                            SubscriberMetadataIdentifier metadataIdentifier, Set<String> urls) {
        try {
            consumer.accept(metadataIdentifier, urls);
        } catch (Exception e) {
            logger.error("Failed to execute consumer, url is: " + urls);
            return false;
        }
        return true;
    }

    boolean throwableAction(Consumer<ServiceMetadataIdentifier> consumer, ServiceMetadataIdentifier metadataIdentifier) {
        try {
            consumer.accept(metadataIdentifier);
        } catch (Exception e) {
            logger.error("Failed to remove url metadata to remote center, metadataIdentifier is: " + metadataIdentifier);
            return false;
        }
        return true;
    }
}
