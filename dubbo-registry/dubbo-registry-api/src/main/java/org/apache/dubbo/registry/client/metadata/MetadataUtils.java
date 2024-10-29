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
import org.apache.dubbo.common.aot.NativeDetector;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataRequest;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceV2;
import org.apache.dubbo.metadata.MetadataServiceV2Detector;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.util.MetadataServiceVersionUtils;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.filter.FilterChainBuilder;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.stub.StubSuppliers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.NATIVE_STUB;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROXY_CLASS_REF;
import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_CREATE_INSTANCE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_LOAD_METADATA;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.metadata.util.MetadataServiceVersionUtils.V2;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URLS_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_VERSION_NAME;
import static org.apache.dubbo.rpc.Constants.AUTH_KEY;
import static org.apache.dubbo.rpc.Constants.PROXY_KEY;

public class MetadataUtils {
    public static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MetadataUtils.class);

    public static void publishServiceDefinition(
            URL url, ServiceDescriptor serviceDescriptor, ApplicationModel applicationModel) {
        if (getMetadataReports(applicationModel).isEmpty()) {
            logger.info("Remote Metadata Report Server is not provided or unavailable, "
                    + "will stop registering service definition to remote center!");
            return;
        }

        try {
            String side = url.getSide();
            if (PROVIDER_SIDE.equalsIgnoreCase(side)) {
                String serviceKey = url.getServiceKey();
                FullServiceDefinition serviceDefinition = serviceDescriptor.getFullServiceDefinition(serviceKey);

                if (StringUtils.isNotEmpty(serviceKey) && serviceDefinition != null) {
                    serviceDefinition.setParameters(url.getParameters());
                    for (Map.Entry<String, MetadataReport> entry :
                            getMetadataReports(applicationModel).entrySet()) {
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
                                        applicationModel.getApplicationName()),
                                serviceDefinition);
                    }
                }
            } else {
                for (Map.Entry<String, MetadataReport> entry :
                        getMetadataReports(applicationModel).entrySet()) {
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
            // ignore error
            logger.error(REGISTRY_FAILED_CREATE_INSTANCE, "", "", "publish service definition metadata error.", e);
        }
    }

    public static RemoteMetadataService referMetadataService(ServiceInstance instance) {
        URL url = buildMetadataUrl(instance);

        // Simply rely on the first metadata url, as stated in MetadataServiceURLBuilder.
        ApplicationModel applicationModel = instance.getApplicationModel();
        ModuleModel internalModel = applicationModel.getInternalModule();

        ConsumerModel consumerModel;

        boolean useV2 = MetadataServiceDelegationV2.VERSION.equals(url.getAttribute(METADATA_SERVICE_VERSION_NAME));
        if (!MetadataServiceV2Detector.support()) {
            useV2 = false;
        }
        boolean inNativeImage = NativeDetector.inNativeImage();

        if (useV2 && !inNativeImage) {
            // If provider supports, we use MetadataServiceV2 in priority
            url = url.addParameter(PROXY_KEY, NATIVE_STUB);
            url = url.setPath(MetadataServiceV2.class.getName());
            url = url.addParameter(VERSION_KEY, V2);

            consumerModel = applicationModel
                    .getInternalModule()
                    .registerInternalConsumer(
                            MetadataServiceV2.class,
                            url,
                            StubSuppliers.getServiceDescriptor(MetadataServiceV2.class.getName()));
        } else {
            consumerModel = applicationModel.getInternalModule().registerInternalConsumer(MetadataService.class, url);
        }

        if (inNativeImage) {
            url = url.addParameter(PROXY_KEY, "jdk");
        }

        Protocol protocol = applicationModel.getExtensionLoader(Protocol.class).getExtension(url.getProtocol(), false);

        url = url.setServiceModel(consumerModel);
        if (url.getParameter(AUTH_KEY, false)) {
            url = url.addParameter(FILTER_KEY, "-default,consumersign");
        }

        RemoteMetadataService remoteMetadataService;
        ProxyFactory proxyFactory =
                applicationModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        if (useV2) {
            Invoker<MetadataServiceV2> invoker = protocol.refer(MetadataServiceV2.class, url);

            if (url.getParameter(AUTH_KEY, false)) {
                FilterChainBuilder filterChainBuilder = ScopeModelUtil.getExtensionLoader(
                                FilterChainBuilder.class, url.getScopeModel())
                        .getDefaultExtension();
                invoker = filterChainBuilder.buildInvokerChain(invoker, REFERENCE_FILTER_KEY, CommonConstants.CONSUMER);
            }

            remoteMetadataService =
                    new RemoteMetadataService(consumerModel, proxyFactory.getProxy(invoker), internalModel);
        } else {
            Invoker<MetadataService> invoker = protocol.refer(MetadataService.class, url);

            if (url.getParameter(AUTH_KEY, false)) {
                FilterChainBuilder filterChainBuilder = ScopeModelUtil.getExtensionLoader(
                                FilterChainBuilder.class, url.getScopeModel())
                        .getDefaultExtension();
                invoker = filterChainBuilder.buildInvokerChain(invoker, REFERENCE_FILTER_KEY, CommonConstants.CONSUMER);
            }

            remoteMetadataService =
                    new RemoteMetadataService(consumerModel, proxyFactory.getProxy(invoker), internalModel);
        }

        Object metadataServiceProxy = remoteMetadataService.getInternalProxy();
        consumerModel.getServiceMetadata().setTarget(metadataServiceProxy);
        consumerModel.getServiceMetadata().addAttribute(PROXY_CLASS_REF, metadataServiceProxy);
        consumerModel.setProxyObject(metadataServiceProxy);
        consumerModel.initMethodModels();

        return remoteMetadataService;
    }

    private static URL buildMetadataUrl(ServiceInstance instance) {
        MetadataServiceURLBuilder builder;
        ExtensionLoader<MetadataServiceURLBuilder> loader =
                instance.getApplicationModel().getExtensionLoader(MetadataServiceURLBuilder.class);

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
            throw new IllegalStateException("Introspection service discovery mode is enabled " + instance
                    + ", but no metadata service can build from it.");
        }
        URL url = urls.get(0);

        String version = metadata.get(METADATA_SERVICE_VERSION_NAME);
        url = url.putAttribute(METADATA_SERVICE_VERSION_NAME, version);
        url = url.addParameter(CHECK_KEY, false);

        return url;
    }

    public static MetadataInfo getRemoteMetadata(
            String revision, List<ServiceInstance> instances, MetadataReport metadataReport) {
        ServiceInstance instance = selectInstance(instances);
        String metadataType = ServiceInstanceMetadataUtils.getMetadataStorageType(instance);
        MetadataInfo metadataInfo;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Instance " + instance.getAddress() + " is using metadata type " + metadataType);
            }
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                metadataInfo = MetadataUtils.getMetadata(revision, instance, metadataReport);
            } else {
                // change the instance used to communicate to avoid all requests route to the same instance
                RemoteMetadataService remoteMetadataService = null;
                try {
                    remoteMetadataService = MetadataUtils.referMetadataService(instance);
                    metadataInfo = remoteMetadataService.getRemoteMetadata(
                            ServiceInstanceMetadataUtils.getExportedServicesRevision(instance));
                } finally {
                    MetadataUtils.destroyProxy(remoteMetadataService);
                }
            }
        } catch (Exception e) {
            logger.error(
                    REGISTRY_FAILED_LOAD_METADATA,
                    "",
                    "",
                    "Failed to get app metadata for revision " + revision + " for type " + metadataType
                            + " from instance " + instance.getAddress(),
                    e);
            metadataInfo = null;
        }

        if (metadataInfo == null) {
            metadataInfo = MetadataInfo.EMPTY;
        }
        return metadataInfo;
    }

    public static void destroyProxy(RemoteMetadataService remoteMetadataService) {
        if (remoteMetadataService != null) {
            remoteMetadataService.destroy();
        }
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
        return applicationModel
                .getBeanFactory()
                .getBean(MetadataReportInstance.class)
                .getMetadataReports(false);
    }

    private static ServiceInstance selectInstance(List<ServiceInstance> instances) {
        if (instances.size() == 1) {
            return instances.get(0);
        }
        return instances.get(ThreadLocalRandom.current().nextInt(0, instances.size()));
    }

    public static class RemoteMetadataService {
        private final ConsumerModel consumerModel;

        @Deprecated
        private MetadataService proxy;

        private MetadataServiceV2 proxyV2;

        private final ModuleModel internalModel;

        public RemoteMetadataService(ConsumerModel consumerModel, MetadataService proxy, ModuleModel internalModel) {
            this.consumerModel = consumerModel;
            this.proxy = proxy;
            this.internalModel = internalModel;
        }

        public RemoteMetadataService(
                ConsumerModel consumerModel, MetadataServiceV2 proxyV2, ModuleModel internalModel) {
            this.consumerModel = consumerModel;
            this.proxyV2 = proxyV2;
            this.internalModel = internalModel;
        }

        public void destroy() {
            if (proxy instanceof Destroyable) {
                ((Destroyable) proxy).$destroy();
            }

            if (proxyV2 instanceof Destroyable) {
                ((Destroyable) proxyV2).$destroy();
            }

            internalModel.getServiceRepository().unregisterConsumer(consumerModel);
        }

        public ConsumerModel getConsumerModel() {
            return consumerModel;
        }

        public Object getInternalProxy() {
            return proxy == null ? proxyV2 : proxy;
        }

        public ModuleModel getInternalModel() {
            return internalModel;
        }

        public MetadataInfo getRemoteMetadata(String revision) {
            Object existProxy = getInternalProxy();
            if (existProxy instanceof MetadataService) {
                return ((MetadataService) existProxy).getMetadataInfo(revision);
            } else {
                return MetadataServiceVersionUtils.toV1(((MetadataServiceV2) existProxy)
                        .getMetadataInfo(MetadataRequest.newBuilder()
                                .setRevision(revision)
                                .build()));
            }
        }
    }
}
