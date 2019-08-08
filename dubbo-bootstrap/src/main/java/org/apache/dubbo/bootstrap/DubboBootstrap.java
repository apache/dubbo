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
package org.apache.dubbo.bootstrap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.wrapper.CompositeDynamicConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.builders.AbstractBuilder;
import org.apache.dubbo.config.builders.ApplicationBuilder;
import org.apache.dubbo.config.builders.ConsumerBuilder;
import org.apache.dubbo.config.builders.ProtocolBuilder;
import org.apache.dubbo.config.builders.ProviderBuilder;
import org.apache.dubbo.config.builders.ReferenceBuilder;
import org.apache.dubbo.config.builders.RegistryBuilder;
import org.apache.dubbo.config.builders.ServiceBuilder;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.store.RemoteWritableMetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.support.ServiceOrientedRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.config.ConfigurationUtils.parseProperties;
import static org.apache.dubbo.common.config.configcenter.DynamicConfiguration.getDynamicConfiguration;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.config.context.ConfigManager.getInstance;
import static org.apache.dubbo.registry.support.AbstractRegistryFactory.getRegistries;

/**
 * The bootstrap class of Dubbo
 *
 * @since 2.7.4
 */
public class DubboBootstrap {

    public static final String DEFAULT_REGISTRY_ID = "REGISTRY#DEFAULT";

    public static final String DEFAULT_PROTOCOL_ID = "PROTOCOL#DEFAULT";

    public static final String DEFAULT_SERVICE_ID = "SERVICE#DEFAULT";

    public static final String DEFAULT_REFERENCE_ID = "REFERENCE#DEFAULT";

    public static final String DEFAULT_PROVIDER_ID = "PROVIDER#DEFAULT";

    public static final String DEFAULT_CONSUMER_ID = "CONSUMER#DEFAULT";

    private static final String NAME = DubboBootstrap.class.getSimpleName();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean awaited = new AtomicBoolean(false);

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private final ExecutorService executorService = newSingleThreadExecutor();

    private final EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    private final ConfigManager configManager = getInstance();

    private volatile boolean initialized = false;

    private volatile boolean started = false;

    /**
     * Only Provider Register
     */
    private volatile boolean onlyRegisterProvider = false;

    private ServiceInstance serviceInstance;

    public DubboBootstrap() {
        DubboShutdownHook.getDubboShutdownHook().register();
    }

    /**
     * Set only register provider or not
     *
     * @param onlyRegisterProvider if <code>true</code>, only register the provider and reduce the registries' load.
     * @return {@link DubboBootstrap}
     */
    public DubboBootstrap onlyRegisterProvider(boolean onlyRegisterProvider) {
        this.onlyRegisterProvider = onlyRegisterProvider;
        return this;
    }

    public DubboBootstrap metadataReport(MetadataReportConfig metadataReportConfig) {
        configManager.addMetadataReport(metadataReportConfig);
        return this;
    }

    public DubboBootstrap metadataReport(List<MetadataReportConfig> metadataReportConfigs) {
        configManager.addMetadataReports(metadataReportConfigs);
        return this;
    }


    // {@link ApplicationConfig} correlative methods

    /**
     * Set the name of application
     *
     * @param name the name of application
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap application(String name) {
        return application(name, builder -> {
            // DO NOTHING
        });
    }

    /**
     * Set the name of application and it's future build
     *
     * @param name            the name of application
     * @param consumerBuilder {@link ApplicationBuilder}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap application(String name, Consumer<ApplicationBuilder> consumerBuilder) {
        ApplicationBuilder builder = createApplicationBuilder(name);
        consumerBuilder.accept(builder);
        return application(builder.build());
    }

    /**
     * Set the {@link ApplicationConfig}
     *
     * @param applicationConfig the {@link ApplicationConfig}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap application(ApplicationConfig applicationConfig) {
        configManager.setApplication(applicationConfig);
        return this;
    }


    // {@link RegistryConfig} correlative methods

    /**
     * Add an instance of {@link RegistryConfig} with {@link #DEFAULT_REGISTRY_ID default ID}
     *
     * @param consumerBuilder the {@link Consumer} of {@link RegistryBuilder}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap registry(Consumer<RegistryBuilder> consumerBuilder) {
        return registry(DEFAULT_REGISTRY_ID, consumerBuilder);
    }

    /**
     * Add an instance of {@link RegistryConfig} with the specified ID
     *
     * @param id              the {@link RegistryConfig#getId() id}  of {@link RegistryConfig}
     * @param consumerBuilder the {@link Consumer} of {@link RegistryBuilder}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap registry(String id, Consumer<RegistryBuilder> consumerBuilder) {
        RegistryBuilder builder = createRegistryBuilder(id);
        consumerBuilder.accept(builder);
        return registry(builder.build());
    }

    /**
     * Add an instance of {@link RegistryConfig}
     *
     * @param registryConfig an instance of {@link RegistryConfig}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap registry(RegistryConfig registryConfig) {
        configManager.addRegistry(registryConfig);
        return this;
    }

    /**
     * Add an instance of {@link RegistryConfig}
     *
     * @param registryConfigs the multiple instances of {@link RegistryConfig}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap registries(Iterable<RegistryConfig> registryConfigs) {
        registryConfigs.forEach(this::registry);
        return this;
    }


    // {@link ProtocolConfig} correlative methods
    public DubboBootstrap protocol(Consumer<ProtocolBuilder> consumerBuilder) {
        return protocol(DEFAULT_PROTOCOL_ID, consumerBuilder);
    }

    public DubboBootstrap protocol(String id, Consumer<ProtocolBuilder> consumerBuilder) {
        ProtocolBuilder builder = createProtocolBuilder(id);
        consumerBuilder.accept(builder);
        return protocol(builder.build());
    }

    public DubboBootstrap protocol(ProtocolConfig protocolConfig) {
        return protocols(asList(protocolConfig));
    }

    public DubboBootstrap protocols(List<ProtocolConfig> protocolConfigs) {
        configManager.addProtocols(protocolConfigs, true);
        return this;
    }


    // {@link ServiceConfig} correlative methods
    public <S> DubboBootstrap service(Consumer<ServiceBuilder<S>> consumerBuilder) {
        return service(DEFAULT_SERVICE_ID, consumerBuilder);
    }

    public <S> DubboBootstrap service(String id, Consumer<ServiceBuilder<S>> consumerBuilder) {
        ServiceBuilder builder = createServiceBuilder(id);
        consumerBuilder.accept(builder);
        return service(builder.build());
    }

    public DubboBootstrap service(ServiceConfig<?> serviceConfig) {
        configManager.addService(serviceConfig);
        return this;
    }


    // {@link Reference} correlative methods
    public <S> DubboBootstrap reference(Consumer<ReferenceBuilder<S>> consumerBuilder) {
        return reference(DEFAULT_REFERENCE_ID, consumerBuilder);
    }

    public <S> DubboBootstrap reference(String id, Consumer<ReferenceBuilder<S>> consumerBuilder) {
        ReferenceBuilder builder = createReferenceBuilder(id);
        consumerBuilder.accept(builder);
        return reference(builder.build());
    }

    public DubboBootstrap reference(ReferenceConfig<?> referenceConfig) {
        configManager.addReference(referenceConfig);
        return this;
    }


    // {@link ProviderConfig} correlative methods
    public DubboBootstrap provider(Consumer<ProviderBuilder> builderConsumer) {
        return provider(DEFAULT_PROVIDER_ID, builderConsumer);
    }

    public DubboBootstrap provider(String id, Consumer<ProviderBuilder> builderConsumer) {
        ProviderBuilder builder = createProviderBuilder(id);
        builderConsumer.accept(builder);
        return provider(builder.build());
    }

    public DubboBootstrap provider(ProviderConfig providerConfig) {
        return providers(asList(providerConfig));
    }

    public DubboBootstrap providers(List<ProviderConfig> providerConfigs) {
        providerConfigs.forEach(configManager::addProvider);
        return this;
    }


    // {@link ConsumerConfig} correlative methods
    public DubboBootstrap consumer(Consumer<ConsumerBuilder> builderConsumer) {
        return consumer(DEFAULT_CONSUMER_ID, builderConsumer);
    }

    public DubboBootstrap consumer(String id, Consumer<ConsumerBuilder> builderConsumer) {
        ConsumerBuilder builder = createConsumerBuilder(id);
        builderConsumer.accept(builder);
        return consumer(builder.build());
    }

    public DubboBootstrap consumer(ConsumerConfig consumerConfig) {
        return consumers(asList(consumerConfig));
    }

    public DubboBootstrap consumers(List<ConsumerConfig> consumerConfigs) {
        consumerConfigs.forEach(configManager::addConsumer);
        return this;
    }

    // {@link ConfigCenterConfig} correlative methods

    public DubboBootstrap configCenter(ConfigCenterConfig configCenterConfig) {
        return configCenter(asList(configCenterConfig));
    }

    public DubboBootstrap configCenter(List<ConfigCenterConfig> configCenterConfigs) {
        configManager.addConfigCenters(configCenterConfigs);
        return this;
    }

    /**
     * Initialize
     */
    public void init() {

        if (isInitialized()) {
            return;
        }

        startConfigCenter();

        startMetadataReport();

        loadRemoteConfigs();

        useRegistryAsConfigCenterIfNecessary();

        initialized = true;

        if (logger.isInfoEnabled()) {
            logger.info(NAME + " has been initialized!");
        }
    }

    private void loadRemoteConfigs() {
        // registry ids to registry configs
        List<RegistryConfig> tmpRegistries = new ArrayList<>();
        Set<String> registryIds = configManager.getRegistryIds();
        registryIds.forEach(id -> {
            if (tmpRegistries.stream().noneMatch(reg -> reg.getId().equals(id))) {
                tmpRegistries.add(configManager.getRegistry(id).orElseGet(() -> {
                    RegistryConfig registryConfig = new RegistryConfig();
                    registryConfig.setId(id);
                    registryConfig.refresh();
                    return registryConfig;
                }));
            }
        });

        configManager.addRegistries(tmpRegistries);

        // protocol ids to protocol configs
        List<ProtocolConfig> tmpProtocols = new ArrayList<>();
        Set<String> protocolIds = configManager.getProtocolIds();
        protocolIds.forEach(id -> {
            if (tmpProtocols.stream().noneMatch(prot -> prot.getId().equals(id))) {
                tmpProtocols.add(configManager.getProtocol(id).orElseGet(() -> {
                    ProtocolConfig protocolConfig = new ProtocolConfig();
                    protocolConfig.setId(id);
                    protocolConfig.refresh();
                    return protocolConfig;
                }));
            }
        });

        configManager.addProtocols(tmpProtocols, true);
    }

    /**
     * For compatibility purpose, use registry as the default config center when the registry protocol is zookeeper and
     * there's no config center specified explicitly.
     */
    private void useRegistryAsConfigCenterIfNecessary() {
        // we use the loading status of DynamicConfiguration to decide whether ConfigCenter has been initiated.
        if (Environment.getInstance().getDynamicConfiguration().isPresent()) {
            return;
        }

        if (CollectionUtils.isNotEmpty(configManager.getConfigCenters())) {
            return;
        }

        configManager.getRegistries().forEach(registryConfig -> {
            String protocol = registryConfig.getProtocol();
            String id = "config-center-" + protocol + "-" + registryConfig.getPort();
            ConfigCenterConfig cc = new ConfigCenterConfig();
            cc.setId(id);
            cc.setProtocol(protocol);
            cc.setAddress(registryConfig.getAddress());
            cc.setHighestPriority(false);
            configManager.addConfigCenter(cc);
        });
        startConfigCenter();
    }

    private List<ServiceDiscovery> getServiceDiscoveries() {
        return getRegistries()
                .stream()
                .filter(registry -> ServiceOrientedRegistry.class.isInstance(registry))
                .map(registry -> ServiceOrientedRegistry.class.cast(registry))
                .map(ServiceOrientedRegistry::getServiceDiscovery)
                .collect(Collectors.toList());
    }

    /**
     * Start the bootstrap
     */
    public DubboBootstrap start() {

        if (!isStarted()) {
            if (!isInitialized()) {
                init();
            }

            exportServices();

            // Not only provider register and some services are exported
            if (!onlyRegisterProvider && !configManager.getServiceConfigs().isEmpty()) {
                /**
                 * export {@link MetadataService}
                 */
                List<URL> exportedURLs = exportMetadataService(
                        configManager.getApplication().orElseThrow(() -> new IllegalStateException("ApplicationConfig cannot be null")),
                        configManager.getRegistries(),
                        configManager.getProtocols()
                );

                /**
                 * Register the local {@link ServiceInstance}
                 */
                registerServiceInstance(exportedURLs);
            }

            started = true;

            if (logger.isInfoEnabled()) {
                logger.info(NAME + " is starting...");
            }
        }
        return this;
    }

    /**
     * Block current thread to be await.
     *
     * @return {@link DubboBootstrap}
     */
    public DubboBootstrap await() {
        // has been waited, return immediately
        if (!awaited.get()) {
            if (!executorService.isShutdown()) {
                executorService.execute(() -> executeMutually(() -> {
                    while (!awaited.get()) {
                        if (logger.isInfoEnabled()) {
                            logger.info(NAME + " is awaiting...");
                        }
                        try {
                            condition.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }));
            }
        }
        return this;
    }

    /**
     * Stop the bootstrap
     */
    public void stop() {

        if (!isInitialized() || !isStarted()) {
            return;
        }

        unregisterServiceInstance();

        destroy();

        clear();

        release();

        shutdown();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isStarted() {
        return started;
    }


    /* serve for builder apis, begin */

    private ApplicationBuilder createApplicationBuilder(String name) {
        return new ApplicationBuilder().name(name);
    }

    private RegistryBuilder createRegistryBuilder(String id) {
        return new RegistryBuilder().id(id);
    }

    private ProtocolBuilder createProtocolBuilder(String id) {
        return new ProtocolBuilder().id(id);
    }

    private ServiceBuilder createServiceBuilder(String id) {
        return new ServiceBuilder().id(id);
    }

    private ReferenceBuilder createReferenceBuilder(String id) {
        return new ReferenceBuilder().id(id);
    }

    private ProviderBuilder createProviderBuilder(String id) {
        return new ProviderBuilder().id(id);
    }

    private ConsumerBuilder createConsumerBuilder(String id) {
        return new ConsumerBuilder().id(id);
    }


    /* serve for builder apis, end */
    private void startMetadataReport() {
        ApplicationConfig applicationConfig = configManager.getApplication().orElseThrow(() -> new IllegalStateException("There's no ApplicationConfig specified."));

        // FIXME, multiple metadata config support.
        Collection<MetadataReportConfig> metadataReportConfigs = configManager.getMetadataConfigs();
        if (CollectionUtils.isEmpty(metadataReportConfigs)) {
            if (CommonConstants.METADATA_REMOTE.equals(applicationConfig.getMetadata())) {
                throw new IllegalStateException("No MetadataConfig found, you must specify the remote Metadata Center address when set 'metadata=remote'.");
            }
            return;
        }
        MetadataReportConfig metadataReportConfig = metadataReportConfigs.iterator().next();
        if (!metadataReportConfig.isValid()) {
            return;
        }

        RemoteWritableMetadataService remoteMetadataService =
                (RemoteWritableMetadataService) WritableMetadataService.getExtension(applicationConfig.getMetadata());
        remoteMetadataService.initMetadataReport(metadataReportConfig.toUrl());
    }

    private void startConfigCenter() {
        Collection<ConfigCenterConfig> configCenters = configManager.getConfigCenters();

        if (CollectionUtils.isNotEmpty(configCenters)) {
            CompositeDynamicConfiguration compositeDynamicConfiguration = new CompositeDynamicConfiguration();
            for (ConfigCenterConfig configCenter : configCenters) {
                configCenter.refresh();
                compositeDynamicConfiguration.addConfiguration(prepareEnvironment(configCenter));
            }
            Environment.getInstance().setDynamicConfiguration(compositeDynamicConfiguration);
        }
        configManager.refreshAll();
    }

    private DynamicConfiguration prepareEnvironment(ConfigCenterConfig configCenter) {
        if (configCenter.isValid()) {
            if (!configCenter.checkOrUpdateInited()) {
                return null;
            }
            DynamicConfiguration dynamicConfiguration = getDynamicConfiguration(configCenter.toUrl());
            String configContent = dynamicConfiguration.getRule(configCenter.getConfigFile(), configCenter.getGroup());

            String appGroup = configManager.getApplication().orElse(new ApplicationConfig()).getName();
            String appConfigContent = null;
            if (isNotEmpty(appGroup)) {
                appConfigContent = dynamicConfiguration.getConfig(isNotEmpty(configCenter.getAppConfigFile()) ?
                        configCenter.getAppConfigFile() : configCenter.getConfigFile(), appGroup
                );
            }
            try {
                Environment.getInstance().setConfigCenterFirst(configCenter.isHighestPriority());
                Environment.getInstance().updateExternalConfigurationMap(parseProperties(configContent));
                Environment.getInstance().updateAppExternalConfigurationMap(parseProperties(appConfigContent));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse configurations from Config Center.", e);
            }
            return dynamicConfiguration;
        }
        return null;
    }

    /**
     * Add an instance of {@link EventListener}
     *
     * @param listener {@link EventListener}
     * @return {@link DubboBootstrap}
     */
    public DubboBootstrap addEventListener(EventListener<?> listener) {
        eventDispatcher.addEventListener(listener);
        return this;
    }

    private List<URL> exportMetadataService(ApplicationConfig applicationConfig,
                                            Collection<RegistryConfig> globalRegistryConfigs,
                                            Collection<ProtocolConfig> globalProtocolConfigs) {
        ConfigurableMetadataServiceExporter exporter = new ConfigurableMetadataServiceExporter();
        exporter.setApplicationConfig(applicationConfig);
        exporter.setRegistries(globalRegistryConfigs);
        exporter.setProtocols(globalProtocolConfigs);
        return exporter.export();
    }

    private void exportServices() {
        configManager.getServiceConfigs().forEach(this::exportServiceConfig);
    }

    public void exportServiceConfig(ServiceConfig<?> serviceConfig) {
        serviceConfig.export();
    }

    public boolean isOnlyRegisterProvider() {
        return onlyRegisterProvider;
    }

    private void registerServiceInstance(List<URL> exportedURLs) {

        exportedURLs
                .stream()
                .findFirst()
                .ifPresent(url -> {
                    String serviceName = url.getParameter(APPLICATION_KEY);
                    String host = url.getHost();
                    int port = url.getPort();

                    ServiceInstance serviceInstance = initServiceInstance(serviceName, host, port);

                    getServiceDiscoveries().forEach(serviceDiscovery -> serviceDiscovery.register(serviceInstance));

                });
    }

    private void unregisterServiceInstance() {

        if (serviceInstance != null) {
            getServiceDiscoveries().forEach(serviceDiscovery -> {
                serviceDiscovery.unregister(serviceInstance);
            });
        }

    }

    private ServiceInstance initServiceInstance(String serviceName, String host, int port) {
        this.serviceInstance = new DefaultServiceInstance(serviceName, host, port);
        return this.serviceInstance;
    }

    private void destroy() {

        destroyProtocolConfigs();

        destroyReferenceConfigs();

    }

    private void destroyProtocolConfigs() {
        configManager.getProtocols().forEach(ProtocolConfig::destroy);
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ProtocolConfigs have been destroyed.");
        }
    }

    private void destroyReferenceConfigs() {
        configManager.getReferenceConfigs().forEach(ReferenceConfig::destroy);
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ReferenceConfigs have been destroyed.");
        }
    }

    private void clear() {
        clearConfigs();
    }

    private void clearConfigs() {
        configManager.clear();
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s configs have been clear.");
        }
    }

    private void release() {
        executeMutually(() -> {
            while (awaited.compareAndSet(false, true)) {
                if (logger.isInfoEnabled()) {
                    logger.info(NAME + " is about to shutdown...");
                }
                condition.signalAll();
            }
        });
    }

    private void shutdown() {
        if (!executorService.isShutdown()) {
            // Shutdown executorService
            executorService.shutdown();
        }
    }

    private void executeMutually(Runnable runnable) {
        try {
            lock.lock();
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    private static <C extends AbstractConfig, B extends
            AbstractBuilder> List<C> buildConfigs(Map<String, B> map) {
        List<C> configs = new ArrayList<>();
        map.entrySet().forEach(entry -> {
            configs.add((C) entry.getValue().build());
        });
        return configs;
    }
}
