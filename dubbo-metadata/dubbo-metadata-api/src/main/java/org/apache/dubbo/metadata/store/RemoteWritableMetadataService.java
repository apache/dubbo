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
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportFactory;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.RpcException;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_DIRECTORY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.metadata.report.support.Constants.METADATA_REPORT_KEY;

/**
 * @since 2.7.0
 */
public class RemoteWritableMetadataService extends InMemoryWritableMetadataService implements WritableMetadataService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private MetadataReportFactory metadataReportFactory = ExtensionLoader.getExtensionLoader(MetadataReportFactory.class).getAdaptiveExtension();
    private MetadataReport metadataReport;
    private AtomicBoolean init;

    public RemoteWritableMetadataService() {
    }

    public void initMetadataReport(URL metadataReportURL) {
        if (!init.compareAndSet(false, true)) {
            return;
        }
        if (METADATA_REPORT_KEY.equals(metadataReportURL.getProtocol())) {
            String protocol = metadataReportURL.getParameter(METADATA_REPORT_KEY, DEFAULT_DIRECTORY);
            metadataReportURL = URLBuilder.from(metadataReportURL)
                    .setProtocol(protocol)
                    .removeParameter(METADATA_REPORT_KEY)
                    .build();
        }
        metadataReport = metadataReportFactory.getMetadataReport(metadataReportURL);
    }

    public MetadataReport getMetadataReport() {
        return metadataReport;
    }

    @Override
    public void publishServiceDefinition(URL providerUrl) {
        try {
            String interfaceName = providerUrl.getParameter(INTERFACE_KEY);
            if (StringUtils.isNotEmpty(interfaceName)) {
                Class interfaceClass = Class.forName(interfaceName);
                ServiceDefinition serviceDefinition = ServiceDefinitionBuilder.build(interfaceClass);
                metadataReport.storeProviderMetadata(new MetadataIdentifier(providerUrl.getServiceInterface(),
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

    @Deprecated
    public void publishConsumer(URL consumerURL) throws RpcException {
        consumerURL = consumerURL.removeParameters(PID_KEY, TIMESTAMP_KEY, Constants.BIND_IP_KEY, Constants.BIND_PORT_KEY, TIMESTAMP_KEY);
        metadataReport.storeConsumerMetadata(new MetadataIdentifier(consumerURL.getServiceInterface(),
                consumerURL.getParameter(VERSION_KEY), consumerURL.getParameter(GROUP_KEY), CONSUMER_SIDE,
                consumerURL.getParameter(APPLICATION_KEY)), consumerURL.getParameters());
    }

    @Override
    public boolean exportURL(URL url) {
        return super.exportURL(url);
    }

    @Override
    public boolean unexportURL(URL url) {
        super.unexportURL(url);
        return throwableAction(metadataReport::removeMetadata, url);
    }

    @Override
    public boolean subscribeURL(URL url) {
        super.subscribeURL(url);
        return throwableAction(metadataReport::saveMetadata, url);
    }

    @Override
    public boolean unsubscribeURL(URL url) {
        super.unsubscribeURL(url);
        return throwableAction(metadataReport::removeMetadata, url);
    }

    @Override
    protected boolean finishRefreshExportedMetadata(URL url) {
        return throwableAction(metadataReport::saveMetadata, url);
    }

    @Override
    public SortedSet<String> getSubscribedURLs() {
        return toSortedStrings(metadataReport.getSubscribedURLs());
    }

    // TODO, protocol should be used
    @Override
    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        return toSortedStrings(metadataReport.getExportedURLs(new MetadataIdentifier(serviceInterface, group, version, null, null)));
    }

    private static SortedSet<String> toSortedStrings(Collection<String> values) {
        return Collections.unmodifiableSortedSet(new TreeSet<>(values));
    }

    @Override
    public String getServiceDefinition(String interfaceName, String version, String group) {
        return metadataReport.getServiceDefinition(new MetadataIdentifier(interfaceName,
                version, group, null, null));
    }

    @Override
    public String getServiceDefinition(String serviceKey) {
        String[] services = UrlUtils.parseServiceKey(serviceKey);
        return metadataReport.getServiceDefinition(new MetadataIdentifier(services[1],
                services[0], services[2], null, null));
    }

    private boolean throwableAction(Consumer<URL> consumer, URL url) {
        try {
            consumer.accept(url);
        } catch (Exception e) {
            logger.error("Failed to remove url metadata to remote center, url is: " + url);
            return false;
        }
        return true;
    }

}
