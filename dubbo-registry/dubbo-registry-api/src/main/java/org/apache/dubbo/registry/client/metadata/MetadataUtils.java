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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URLS_PROPERTY_NAME;

public class MetadataUtils {
    public static final Logger logger = LoggerFactory.getLogger(MetadataUtils.class);

    public static ConcurrentMap<String, MetadataService> metadataServiceProxies = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, Invoker<?>> metadataServiceInvokers = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, Invoker<?>> getMetadataServiceInvokers() {
        return metadataServiceInvokers;
    }

    public static synchronized MetadataService getMetadataServiceProxy(ServiceInstance instance) {
        return metadataServiceProxies.computeIfAbsent(computeKey(instance), k -> referProxy(k, instance));
    }

    public static void publishServiceDefinition(ServiceDescriptor serviceDescriptor, ApplicationModel applicationModel) {
        if (getMetadataReports(applicationModel).size() == 0) {
            String msg = "Remote Metadata Report Server not hasn't been configured or unavailable . Unable to get Metadata from remote!";
            logger.warn(msg);
        }

        String serviceName = serviceDescriptor.getServiceName();
        FullServiceDefinition serviceDefinition = serviceDescriptor.getServiceDefinition(serviceName);

        try {
            if (StringUtils.isNotEmpty(serviceName)) {
                for (Map.Entry<String, MetadataReport> entry : getMetadataReports(applicationModel).entrySet()) {
                    MetadataReport metadataReport = entry.getValue();
                    metadataReport.storeProviderMetadata(new MetadataIdentifier(serviceName,
                        "", "",
                        PROVIDER_SIDE, applicationModel.getApplicationName()), serviceDefinition);
                }
                return;
            }
            logger.error("publishProvider interfaceName is empty.");
        } catch (Exception e) {
            //ignore error
            logger.error("publishProvider getServiceDescriptor error.", e);
        }
    }

    public static String computeKey(ServiceInstance serviceInstance) {
        return serviceInstance.getServiceName() + "##" + serviceInstance.getAddress() + "##" +
                ServiceInstanceMetadataUtils.getExportedServicesRevision(serviceInstance);
    }

    public static synchronized void destroyMetadataServiceProxy(ServiceInstance instance) {
        String key = computeKey(instance);
        if (metadataServiceProxies.containsKey(key)) {
            metadataServiceProxies.remove(key);
            Invoker<?> invoker = metadataServiceInvokers.remove(key);
            invoker.destroy();
        }
    }

    private static MetadataService referProxy(String key, ServiceInstance instance) {
        MetadataServiceURLBuilder builder;
        ExtensionLoader<MetadataServiceURLBuilder> loader = instance.getApplicationModel()
            .getExtensionLoader(MetadataServiceURLBuilder.class);

        Map<String, String> metadata = instance.getMetadata();
        // METADATA_SERVICE_URLS_PROPERTY_NAME is a unique key exists only on instances of spring-cloud-alibaba.
        String dubboUrlsForJson = metadata.get(METADATA_SERVICE_URLS_PROPERTY_NAME);
        if (metadata.isEmpty() || StringUtils.isEmpty(dubboUrlsForJson)) {
            builder = loader.getExtension(StandardMetadataServiceURLBuilder.NAME);
        } else {
            builder = loader.getExtension(SpringCloudMetadataServiceURLBuilder.NAME);
        }

        List<URL> urls = builder.build(instance);
        if (CollectionUtils.isEmpty(urls)) {
            throw new IllegalStateException("Introspection service discovery mode is enabled "
                    + instance + ", but no metadata service can build from it.");
        }

        // Simply rely on the first metadata url, as stated in MetadataServiceURLBuilder.
        ScopeModel scopeModel = instance.getApplicationModel();
        Protocol protocol = scopeModel.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        Invoker<MetadataService> invoker = protocol.refer(MetadataService.class, urls.get(0));
        metadataServiceInvokers.put(key, invoker);

        ProxyFactory proxyFactory = scopeModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        return proxyFactory.getProxy(invoker);
    }

    public static ConcurrentMap<String, MetadataService> getMetadataServiceProxies() {
        return metadataServiceProxies;
    }

    public static MetadataInfo getRemoteMetadata(String revision, ServiceInstance instance, MetadataReport metadataReport) {
        String metadataType = ServiceInstanceMetadataUtils.getMetadataStorageType(instance);
        MetadataInfo metadataInfo = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Instance " + instance.getAddress() + " is using metadata type " + metadataType);
            }
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                metadataInfo = MetadataUtils.getMetadata(revision, instance, metadataReport);
            } else {
                // change the instance used to communicate to avoid all requests route to the same instance
                MetadataService metadataServiceProxy = MetadataUtils.getMetadataServiceProxy(instance);
                metadataInfo = metadataServiceProxy.getMetadataInfo(ServiceInstanceMetadataUtils.getExportedServicesRevision(instance));
                MetadataUtils.destroyMetadataServiceProxy(instance);
            }
        } catch (Exception e) {
            metadataInfo = null;
        }

        if (metadataInfo == null) {
            metadataInfo = MetadataInfo.EMPTY;
        }
        return metadataInfo;
    }

    public static MetadataInfo getMetadata(String revision, ServiceInstance instance, MetadataReport metadataReport) {
        SubscriberMetadataIdentifier identifier = new SubscriberMetadataIdentifier(instance.getServiceName(), revision);

        if (metadataReport == null) {
            throw new IllegalStateException("No valid remote metadata report specified.");
        }

        String registryCluster = instance.getRegistryCluster();
        Map<String, String> params = new HashMap<>(instance.getExtendParams());
        if (registryCluster != null && !registryCluster.equalsIgnoreCase(params.get(REGISTRY_CLUSTER_KEY))) {
            params.put(REGISTRY_CLUSTER_KEY, registryCluster);
        }

        return metadataReport.getAppMetadata(identifier, params);
    }

    private static Map<String, MetadataReport> getMetadataReports(ApplicationModel applicationModel) {
        return applicationModel.getBeanFactory().getBean(MetadataReportInstance.class).getMetadataReports(false);
    }

}
