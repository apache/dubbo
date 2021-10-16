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
package org.apache.dubbo.registry.client.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;

public class RemoteMetadataServiceImpl implements ScopeModelAware {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private WritableMetadataService localMetadataService;
    private MetadataReportInstance metadataReportInstance;

    @Override
    public void setScopeModel(ScopeModel scopeModel) {
        metadataReportInstance = scopeModel.getBeanFactory().getBean(MetadataReportInstance.class);
        localMetadataService = scopeModel.getDefaultExtension(WritableMetadataService.class);
    }

    public Map<String, MetadataReport> getMetadataReports() {
        return metadataReportInstance.getMetadataReports(false);
    }

    public void publishMetadata(String serviceName) {
        Map<String, MetadataInfo> metadataInfos = localMetadataService.getMetadataInfos();
        metadataInfos.forEach((registryCluster, metadataInfo) -> {
            if (!metadataInfo.hasReported()) {
                SubscriberMetadataIdentifier identifier = new SubscriberMetadataIdentifier(serviceName, metadataInfo.calAndGetRevision());
                metadataInfo.getExtendParams().put(REGISTRY_CLUSTER_KEY, registryCluster);
                if (getMetadataReports().size() > 0) {
                    MetadataReport metadataReport = getMetadataReports().get(registryCluster);
                    if (metadataReport == null) {
                        metadataReport = getMetadataReports().entrySet().iterator().next().getValue();
                    }
                    logger.info("Publishing metadata to " + metadataReport.getClass().getSimpleName());
                    if (logger.isDebugEnabled()) {
                        logger.debug(metadataInfo.toString());
                    }
                    metadataReport.publishAppMetadata(identifier, metadataInfo);
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("Remote Metadata Report Server not hasn't been configured. Only publish Metadata to local.");
                    }
                }
                metadataInfo.markReported();
            }
        });
    }

    public MetadataInfo getMetadata(ServiceInstance instance) {
        SubscriberMetadataIdentifier identifier = new SubscriberMetadataIdentifier(instance.getServiceName(),
            ServiceInstanceMetadataUtils.getExportedServicesRevision(instance));

        String registryCluster = instance.getRegistryCluster();

        checkRemoteConfigured();

        MetadataReport metadataReport = getMetadataReports().get(registryCluster);
        if (metadataReport == null) {
            metadataReport = getMetadataReports().entrySet().iterator().next().getValue();
        }
        Map<String, String> params = new HashMap<>(instance.getExtendParams());
        if (registryCluster != null && !registryCluster.equalsIgnoreCase(params.get(REGISTRY_CLUSTER_KEY))) {
            params.put(REGISTRY_CLUSTER_KEY, registryCluster);
        }
        return metadataReport.getAppMetadata(identifier, params);
    }

    private void checkRemoteConfigured() {
        if (getMetadataReports().size() == 0) {
            String msg = "Remote Metadata Report Server not hasn't been configured or unavailable . Unable to get Metadata from remote!";
            logger.error(msg);
            throw new IllegalStateException(msg);
        }
    }

    public void publishServiceDefinition(URL url) {
        checkRemoteConfigured();

        String side = url.getSide();

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
        providerUrl = providerUrl.removeParameters(PID_KEY, TIMESTAMP_KEY, BIND_IP_KEY, BIND_PORT_KEY);

        try {
            String interfaceName = providerUrl.getServiceInterface();
            if (StringUtils.isNotEmpty(interfaceName)) {
                ClassLoader classLoader = providerUrl.getServiceModel() != null ?
                    providerUrl.getServiceModel().getClassLoader() :
                    ClassUtils.getClassLoader();
                Class interfaceClass = Class.forName(interfaceName, false, classLoader);
                FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass,
                    providerUrl.getParameters());
                for (Map.Entry<String, MetadataReport> entry : getMetadataReports().entrySet()) {
                    MetadataReport metadataReport = entry.getValue();
                    metadataReport.storeProviderMetadata(new MetadataIdentifier(providerUrl.getServiceInterface(),
                        providerUrl.getVersion(), providerUrl.getGroup(),
                        PROVIDER_SIDE, providerUrl.getApplication()), fullServiceDefinition);
                }
                return;
            }
            logger.error("publishProvider interfaceName is empty. providerUrl: " + providerUrl.toFullString());
        } catch (ClassNotFoundException e) {
            //ignore error
            logger.error("publishProvider getServiceDescriptor error. providerUrl: " + providerUrl.toFullString(), e);
        }
    }

    private void publishConsumer(URL consumerURL) throws RpcException {
        final URL url = consumerURL.removeParameters(PID_KEY, TIMESTAMP_KEY, BIND_IP_KEY, BIND_PORT_KEY, REGISTER_IP_KEY);
        getMetadataReports().forEach((registryKey, config) -> {
            config.storeConsumerMetadata(new MetadataIdentifier(url.getServiceInterface(),
                url.getVersion(), url.getGroup(), CONSUMER_SIDE,
                url.getApplication()), url.getParameters());
        });
    }

}
