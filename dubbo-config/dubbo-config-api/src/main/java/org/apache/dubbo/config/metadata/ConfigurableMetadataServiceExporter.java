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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.metadata.MetadataServiceDelegation;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_FIND_PROTOCOL;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_METADATA_SERVICE_EXPORTED;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;

/**
 * Export metadata service
 */
public class ConfigurableMetadataServiceExporter {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());
    private final ApplicationModel applicationModel;
    private MetadataServiceDelegation metadataService;
    private volatile ServiceConfig<MetadataService> serviceConfig;
    private static final Set<String> UNACCEPTABLE_PROTOCOL = Stream.of("rest", "grpc").collect(Collectors.toSet());

    public ConfigurableMetadataServiceExporter(ApplicationModel applicationModel, MetadataServiceDelegation metadataService) {
        this.applicationModel = applicationModel;
        this.metadataService = metadataService;
    }

    public synchronized ConfigurableMetadataServiceExporter export() {
        if (serviceConfig == null || !isExported()) {
            this.serviceConfig = buildServiceConfig();
            // export
            serviceConfig.export();
            metadataService.setMetadataURL(serviceConfig.getExportedUrls().get(0));
            if (logger.isInfoEnabled()) {
                logger.info("The MetadataService exports urls : " + serviceConfig.getExportedUrls());
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn(CONFIG_METADATA_SERVICE_EXPORTED, "", "", "The MetadataService has been exported : " + serviceConfig.getExportedUrls());
            }
        }

        return this;
    }

    public ConfigurableMetadataServiceExporter unexport() {
        if (isExported()) {
            serviceConfig.unexport();
            metadataService.setMetadataURL(null);
        }
        return this;
    }

    public boolean isExported() {
        return serviceConfig != null && serviceConfig.isExported() && !serviceConfig.isUnexported();
    }

    private ApplicationConfig getApplicationConfig() {
        return applicationModel.getApplicationConfigManager().getApplication().get();
    }

    private ProtocolConfig getProtocolConfig(String protocol) {
        return applicationModel.getApplicationConfigManager().getProtocol(protocol).orElse(null);
    }

    private ProtocolConfig generateMetadataProtocol() {
        // protocol always defaults to dubbo if not specified and no related
        String specifiedProtocol = getSpecifiedProtocol();
        // port can not being determined here if not specified
        Integer port = getSpecifiedPort();

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName(specifiedProtocol);
        if (port == null || port < -1) {
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("Metadata Service Port hasn't been set will use default protocol defined in protocols.");
                }

                Protocol protocol = applicationModel.getExtensionLoader(Protocol.class).getExtension(specifiedProtocol);
                if (protocol != null && protocol.getServers() != null) {
                    Iterator<ProtocolServer> it = protocol.getServers().iterator();
                    // metadata service may export before normal service export, it.hasNext() will return false.
                    // so need use specified protocol port.
                    if (it.hasNext()) {
                        ProtocolServer server = it.next();
                        String rawPort = server.getUrl().getParameter(BIND_PORT_KEY);
                        if (rawPort == null) {
                            String addr = server.getAddress();
                            rawPort = addr.substring(addr.indexOf(":") + 1);
                        }
                        protocolConfig.setPort(Integer.parseInt(rawPort));
                    } else {
                        ProtocolConfig specifiedProtocolConfig = getProtocolConfig(specifiedProtocol);
                        if (specifiedProtocolConfig != null) {
                            Integer protocolPort = specifiedProtocolConfig.getPort();
                            if (null != protocolPort && protocolPort != -1) {
                                protocolConfig.setPort(protocolPort);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(CONFIG_FAILED_FIND_PROTOCOL, "invalid specified " + specifiedProtocol + "  protocol", "", "Failed to find any valid protocol, will use random port to export metadata service.", e);
            }
        } else {
            protocolConfig.setPort(port);
        }

        applicationModel.getApplicationConfigManager().getProtocol(specifiedProtocol)
            .ifPresent(protocolConfig::mergeProtocol);

        if (protocolConfig.getPort() == null) {
            protocolConfig.setPort(-1);
        }

        logger.info("Using " + specifiedProtocol + " protocol to export metadata service on port " + protocolConfig.getPort());

        return protocolConfig;
    }

    private Integer getSpecifiedPort() {
        Integer port = getApplicationConfig().getMetadataServicePort();
        if (port == null) {
            Map<String, String> params = getApplicationConfig().getParameters();
            if (CollectionUtils.isNotEmptyMap(params)) {
                String rawPort = getApplicationConfig().getParameters().get(METADATA_SERVICE_PORT_KEY);
                if (StringUtils.isNotEmpty(rawPort)) {
                    port = Integer.parseInt(rawPort);
                }
            }
        }
        return port;
    }

    private String getSpecifiedProtocol() {
        String protocol = getApplicationConfig().getMetadataServiceProtocol();
        if (StringUtils.isEmpty(protocol)) {
            Map<String, String> params = getApplicationConfig().getParameters();
            if (CollectionUtils.isNotEmptyMap(params)) {
                protocol = params.get(METADATA_SERVICE_PROTOCOL_KEY);
            }
        }

        return StringUtils.isNotEmpty(protocol) ? protocol : getRelatedOrDefaultProtocol();
    }

    /**
     * Get other configured protocol from environment in priority order. If get nothing, use default dubbo.
     *
     * @return
     */
    private String getRelatedOrDefaultProtocol() {
        String protocol = "";
        // <dubbo:consumer/>
        List<ModuleModel> moduleModels = applicationModel.getPubModuleModels();
        protocol = moduleModels.stream()
            .map(ModuleModel::getConfigManager)
            .map(ModuleConfigManager::getConsumers)
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(Collection::stream)
            .map(ConsumerConfig::getProtocol)
            .filter(StringUtils::isNotEmpty)
            .filter(p -> !UNACCEPTABLE_PROTOCOL.contains(p))
            .findFirst()
            .orElse("");
        // <dubbo:provider/>
        if (StringUtils.isEmpty(protocol)) {
            Stream<ProviderConfig> providerConfigStream = moduleModels.stream()
                .map(ModuleModel::getConfigManager)
                .map(ModuleConfigManager::getProviders)
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream);
            protocol = providerConfigStream
                .filter((providerConfig) -> providerConfig.getProtocol() != null || CollectionUtils.isNotEmpty(providerConfig.getProtocols()))
                .map(providerConfig -> {
                    if (providerConfig.getProtocol() != null && StringUtils.isNotEmpty(providerConfig.getProtocol().getName())) {
                        return providerConfig.getProtocol().getName();
                    } else {
                        return providerConfig.getProtocols().stream()
                            .map(ProtocolConfig::getName)
                            .filter(StringUtils::isNotEmpty)
                            .findFirst()
                            .orElse("");
                    }
                })
                .filter(StringUtils::isNotEmpty)
                .filter(p -> !UNACCEPTABLE_PROTOCOL.contains(p))
                .findFirst()
                .orElse("");
        }
        // <dubbo:protocol/>
        if (StringUtils.isEmpty(protocol)) {
            Collection<ProtocolConfig> protocols = applicationModel.getApplicationConfigManager().getProtocols();
            if (CollectionUtils.isNotEmpty(protocols)) {
                protocol = protocols.stream()
                    .map(ProtocolConfig::getName)
                    .filter(StringUtils::isNotEmpty)
                    .filter(p -> !UNACCEPTABLE_PROTOCOL.contains(p))
                    .findFirst()
                    .orElse("");
            }
        }
        // <dubbo:application/>
        if (StringUtils.isEmpty(protocol)) {
            protocol = getApplicationConfig().getProtocol();
            if (StringUtils.isEmpty(protocol)) {
                Map<String, String> params = getApplicationConfig().getParameters();
                if (CollectionUtils.isNotEmptyMap(params)) {
                    protocol = params.get(APPLICATION_PROTOCOL_KEY);
                }
            }
        }
        return StringUtils.isNotEmpty(protocol) && !UNACCEPTABLE_PROTOCOL.contains(protocol) ? protocol : DUBBO_PROTOCOL;
    }

    private ServiceConfig<MetadataService> buildServiceConfig() {
        ApplicationConfig applicationConfig = getApplicationConfig();
        ServiceConfig<MetadataService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setScopeModel(applicationModel.getInternalModule());
        serviceConfig.setApplication(applicationConfig);
        RegistryConfig registryConfig = new RegistryConfig("N/A");
        registryConfig.setId("internal-metadata-registry");
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setRegister(false);
        serviceConfig.setProtocol(generateMetadataProtocol());
        serviceConfig.setInterface(MetadataService.class);
        serviceConfig.setDelay(0);
        serviceConfig.setRef(metadataService);
        serviceConfig.setGroup(applicationConfig.getName());
        serviceConfig.setVersion(MetadataService.VERSION);
        serviceConfig.setMethods(generateMethodConfig());
        serviceConfig.setConnections(1); // separate connection
        serviceConfig.setExecutes(100); // max tasks running at the same time

        return serviceConfig;
    }

    /**
     * Generate Method Config for Service Discovery Metadata <p/>
     * <p>
     * Make {@link MetadataService} support argument callback,
     * used to notify {@link org.apache.dubbo.registry.client.ServiceInstance}'s
     * metadata change event
     *
     * @since 3.0
     */
    private List<MethodConfig> generateMethodConfig() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("getAndListenInstanceMetadata");

        ArgumentConfig argumentConfig = new ArgumentConfig();
        argumentConfig.setIndex(1);
        argumentConfig.setCallback(true);

        methodConfig.setArguments(Collections.singletonList(argumentConfig));

        return Collections.singletonList(methodConfig);
    }

    // for unit test
    public void setMetadataService(MetadataServiceDelegation metadataService) {
        this.metadataService = metadataService;
    }

    // for unit test
    public List<URL> getExportedURLs() {
        return serviceConfig != null ? serviceConfig.getExportedUrls() : emptyList();
    }

}
