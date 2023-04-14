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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;

public class InternalServiceConfigBuilder<T> {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());
    private static final Set<String> UNACCEPTABLE_PROTOCOL = Stream.of("rest", "grpc").collect(Collectors.toSet());

    private final ApplicationModel applicationModel;
    private String  protocol;
    private Integer port;
    private String registryId;
    private Class<T> interfaceClass;
    private Executor executor;
    private T   ref;

    private InternalServiceConfigBuilder(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    public static <T> InternalServiceConfigBuilder<T> newBuilder(ApplicationModel applicationModel) {
        return new InternalServiceConfigBuilder<>(applicationModel);
    }



    public InternalServiceConfigBuilder<T> interfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return getThis();
    }

    public InternalServiceConfigBuilder<T> executor(Executor executor) {
        this.executor = executor;
        return getThis();
    }

    public InternalServiceConfigBuilder<T> ref(T ref) {
        this.ref = ref;
        return getThis();
    }

    public InternalServiceConfigBuilder<T> registryId(String registryId) {
        this.registryId = registryId;
        return getThis();
    }

    public InternalServiceConfigBuilder<T> protocol(String protocol, String key) {
        if (StringUtils.isEmpty(protocol) && StringUtils.isNotBlank(key)) {
            Map<String, String> params = getApplicationConfig().getParameters();

            if (CollectionUtils.isNotEmptyMap(params)) {
                protocol = params.get(key);
            }
        }
        this.protocol = StringUtils.isNotEmpty(protocol) ? protocol : getRelatedOrDefaultProtocol();

        return getThis();
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

    public InternalServiceConfigBuilder<T> protocol(String protocol) {
        this.protocol(protocol, null);
        return getThis();
    }

    public InternalServiceConfigBuilder<T> port(Integer specPort) {
        return port(specPort,null);
    }


    public InternalServiceConfigBuilder<T> port(Integer specPort, String key) {
        Assert.notEmptyString(this.protocol,"export protocol is null");
        Assert.notNull(this.interfaceClass,"export interfaceClass is null");

        if (specPort != null) {
            this.port = specPort;
            return getThis();
        }
        Map<String, String> params = getApplicationConfig().getParameters();
        if (CollectionUtils.isNotEmptyMap(params) && StringUtils.isNotBlank(key)) {
            String rawPort = getApplicationConfig().getParameters().get(key);
            if (StringUtils.isNotEmpty(rawPort)) {
                specPort = Integer.parseInt(rawPort);
            }
        }

        if (specPort == null || specPort < -1) {
            try {
                if (logger.isInfoEnabled()) {
                    logger.info(interfaceClass.getName()+"Service Port hasn't been set will use default protocol defined in protocols.");
                }

                Protocol protocol = applicationModel.getExtensionLoader(Protocol.class).getExtension(this.protocol);
                if (protocol != null && protocol.getServers() != null) {
                    Iterator<ProtocolServer> it = protocol.getServers().iterator();
                    // export service may export before normal service export, it.hasNext() will return false.
                    // so need use specified protocol port.
                    if (it.hasNext()) {
                        ProtocolServer server = it.next();
                        String rawPort = server.getUrl().getParameter(BIND_PORT_KEY);
                        if (rawPort == null) {
                            String addr = server.getAddress();
                            rawPort = addr.substring(addr.indexOf(":") + 1);
                        }
                        this.port = Integer.parseInt(rawPort);
                    } else {
                        ProtocolConfig specifiedProtocolConfig = getProtocolConfig();
                        if (specifiedProtocolConfig != null) {
                            Integer protocolPort = specifiedProtocolConfig.getPort();
                            if (null != protocolPort && protocolPort != -1) {
                                this.port = protocolPort;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(INTERNAL_ERROR, "invalid specified " + port + "  port, error "+e.getMessage(),
                    "", "Failed to find any valid protocol, will use random port to export  service.",e);
            }
        }
        if (this.port == null) {
            this.port = -1;
        }
        return getThis();
    }

    private ProtocolConfig getProtocolConfig() {
        return applicationModel.getApplicationConfigManager().getProtocol(protocol).orElse(null);
    }

    public ServiceConfig<T> build(Consumer<ServiceConfig<T>> configConsumer){
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName(this.protocol);
        protocolConfig.setPort(this.port);

        this.nullAssert();

        logger.info("Using " + this.protocol + " protocol to export "+interfaceClass.getName()+" service on port " + protocolConfig.getPort());

        applicationModel.getApplicationConfigManager().getProtocol(this.protocol)
            .ifPresent(protocolConfig::mergeProtocol);

        ApplicationConfig applicationConfig = getApplicationConfig();

        ServiceConfig<T> serviceConfig = new ServiceConfig<>();
        serviceConfig.setScopeModel(applicationModel.getInternalModule());
        serviceConfig.setApplication(applicationConfig);

        RegistryConfig registryConfig = new RegistryConfig("N/A");
        registryConfig.setId(this.registryId);
        registryConfig.setScopeModel(this.applicationModel);

        serviceConfig.setRegistry(registryConfig);

        serviceConfig.setRegister(false);
        serviceConfig.setProtocol(protocolConfig);
        serviceConfig.setDelay(0);
        serviceConfig.setInterface(interfaceClass);
        serviceConfig.setRef(this.ref);
        serviceConfig.setGroup(applicationConfig.getName());
        serviceConfig.setVersion("1.0.0");

        serviceConfig.setExecutor(executor);

        if (null != configConsumer) {
            configConsumer.accept(serviceConfig);
        }

        return serviceConfig;
    }

    public ServiceConfig<T> build(){
        return build(null);
    }
    private void nullAssert() {
        Assert.notNull(port, "export service port is null");
        Assert.notNull(protocol, "export service protocol is null");
        Assert.notNull(interfaceClass, "export service interfaceClass is null");
        Assert.notNull(ref,"export service ref is null");
        Assert.notNull(registryId,"export service registryId is null");
    }

    protected InternalServiceConfigBuilder<T> getThis() {
        return this;
    }

    private ApplicationConfig getApplicationConfig() {
        return applicationModel.getApplicationConfigManager().getApplicationOrElseThrow();
    }

}
