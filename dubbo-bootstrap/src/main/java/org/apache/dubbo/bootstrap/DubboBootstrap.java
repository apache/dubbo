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
import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.common.config.configcenter.wrapper.CompositeDynamicConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
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
import java.util.HashMap;
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

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.config.ConfigurationUtils.parseProperties;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.registry.support.AbstractRegistryFactory.getRegistries;

/**
 * The bootstrap class of Dubbo
 *
 * @since 2.7.3
 */
public class DubboBootstrap {

    public static final String DEFAULT_REGISTRY_ID = "REGISTRY#DEFAULT";

    public static final String DEFAULT_PROTOCOL_ID = "PROTOCOL#DEFAULT";

    public static final String DEFAULT_SERVICE_ID = "SERVICE#DEFAULT";

    public static final String DEFAULT_REFERENCE_ID = "REFERENCE#DEFAULT";

    private static final String NAME = DubboBootstrap.class.getSimpleName();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean awaited = new AtomicBoolean(false);

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private final ExecutorService executorService = newSingleThreadExecutor();

    private final EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    /**
     * Is provider or not
     */
    private boolean isProvider;

    private boolean initialized = false;

    private boolean started = false;

    /**
     * Only Provider Register
     */
    private boolean onlyRegisterProvider = false;

    private ServiceInstance serviceInstance;

    private ApplicationBuilder applicationBuilder;

    private ConsumerBuilder consumerBuilder;

    private ProviderBuilder providerBuilder;

    private Map<String, RegistryBuilder> registryBuilders = new HashMap<>();

    private Map<String, ProtocolBuilder> protocolBuilders = new HashMap<>();

    private Map<String, ServiceBuilder<?>> serviceBuilders = new HashMap<>();

    private Map<String, ReferenceBuilder<?>> referenceBuilders = new HashMap<>();

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

    /* accept Config instance */
    public DubboBootstrap application(ApplicationConfig applicationConfig) {
        ConfigManager.getInstance().setApplication(applicationConfig);
        return this;
    }

    public DubboBootstrap configCenter(ConfigCenterConfig configCenterConfig) {
        ConfigManager.getInstance().addConfigCenter(configCenterConfig);
        return this;
    }

    public DubboBootstrap configCenter(List<ConfigCenterConfig> configCenterConfigs) {
        ConfigManager.getInstance().addConfigCenter(configCenterConfigs);
        return this;
    }

    public DubboBootstrap metadataReport(MetadataReportConfig metadataReportConfig) {
        ConfigManager.getInstance().addMetadataReport(metadataReportConfig);
        return this;
    }

    public DubboBootstrap metadataReport(List<MetadataReportConfig> metadataReportConfigs) {
        ConfigManager.getInstance().addMetadataReport(metadataReportConfigs);
        return this;
    }

    public DubboBootstrap registry(RegistryConfig registryConfig) {
        ConfigManager.getInstance().addRegistry(registryConfig, true);
        return this;
    }

    public DubboBootstrap registry(List<RegistryConfig> registryConfigs) {
        ConfigManager.getInstance().addRegistries(registryConfigs, true);
        return this;
    }

    public DubboBootstrap protocol(ProtocolConfig protocolConfig) {
        ConfigManager.getInstance().addProtocol(protocolConfig, true);
        return this;
    }

    public DubboBootstrap protocols(List<ProtocolConfig> protocolConfigs) {
        ConfigManager.getInstance().addProtocols(protocolConfigs, true);
        return this;
    }

    public DubboBootstrap consumer(ConsumerConfig consumerConfig) {
        ConfigManager.getInstance().addConsumer(consumerConfig);
        return this;
    }

    public DubboBootstrap provider(ProviderConfig providerConfig) {
        ConfigManager.getInstance().addProvider(providerConfig);
        return this;
    }

    public DubboBootstrap service(ServiceConfig<?> serviceConfig) {
        ConfigManager.getInstance().addService(serviceConfig);
        return this;
    }

    public DubboBootstrap reference(ReferenceConfig<?> referenceConfig) {
        ConfigManager.getInstance().addReference(referenceConfig);
        return this;
    }

    /* accept builder functional interface */
    public DubboBootstrap application(String name, Consumer<ApplicationBuilder> builder) {
        initApplicationBuilder(name);
        builder.accept(applicationBuilder);
        return this;
    }

    public DubboBootstrap registry(String id, Consumer<RegistryBuilder> builder) {
        builder.accept(initRegistryBuilder(id));
        return this;
    }

    public DubboBootstrap protocol(String id, Consumer<ProtocolBuilder> builder) {
        builder.accept(initProtocolBuilder(id));
        return this;
    }

    public <S> DubboBootstrap service(String id, Consumer<ServiceBuilder<S>> builder) {
        builder.accept(initServiceBuilder(id));
        return this;
    }

    public <S> DubboBootstrap reference(String id, Consumer<ReferenceBuilder<S>> builder) {
        builder.accept(initReferenceBuilder(id));
        return this;
    }

    /**
     * Initialize
     */
    public void init() {

        if (isInitialized()) {
            return;
        }

        buildApplicationConfig();

        buildRegistryConfigs();

        buildProtocolConfigs();

        buildServiceConfigs();

        buildReferenceConfigs();

        clearBuilders();

        startConfigCenter();
        startMetadataReport();

        loadRemoteConfigs();
        useRegistryAsConfigCenterIfNecessary();

//        checkApplication();
//        checkProvider();
//        chcckConsumer();
//        checkRegistry();
//        checkProtocol();
//        checkMonitor();

        initialized = true;

        if (logger.isInfoEnabled()) {
            logger.info(NAME + " has been initialized!");
        }
    }

    private void loadRemoteConfigs() {
        ConfigManager configManager = ConfigManager.getInstance();

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

        configManager.addRegistries(tmpRegistries, true);

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
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.getDefaultRegistries().ifPresent(registryConfigs -> {
            for (RegistryConfig registryConfig : registryConfigs) {
                if (registryConfig != null && registryConfig.isZookeeperProtocol()) {
                    // we use the loading status of DynamicConfiguration to decide whether ConfigCenter has been initiated.
                    Environment.getInstance().getDynamicConfiguration().orElseGet(() -> {
                        Set<ConfigCenterConfig> configCenters = configManager.getConfigCenters();
                        if (CollectionUtils.isEmpty(configCenters)) {
                            ConfigCenterConfig cc = new ConfigCenterConfig();
                            cc.setProtocol(registryConfig.getProtocol());
                            cc.setAddress(registryConfig.getAddress());
                            cc.setHighestPriority(false);
                            configManager.addConfigCenter(cc);
                        }
                        return null;
                    });
                }
            }
            startConfigCenter();
        });
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
            if (!onlyRegisterProvider && !ConfigManager.getInstance().getServiceConfigs().isEmpty()) {
                /**
                 * export {@link MetadataService}
                 */
                ConfigManager configManager = ConfigManager.getInstance();
                // TODO, only export to default registry?
                List<URL> exportedURLs = exportMetadataService (
                        configManager.getApplication().orElseThrow(() -> new IllegalStateException("ApplicationConfig cannot be null")),
                        configManager.getDefaultRegistries().orElseThrow(() -> new IllegalStateException("No default RegistryConfig")),
                        configManager.getDefaultProtocols().orElseThrow(() -> new IllegalStateException("No default ProtocolConfig"))
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
    private ApplicationBuilder initApplicationBuilder(String name) {
        applicationBuilder = new ApplicationBuilder().name(name);
        return applicationBuilder;
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

    private RegistryBuilder initRegistryBuilder(String id) {
        return registryBuilders.computeIfAbsent(id, this::createRegistryBuilder);
    }

    private ProtocolBuilder initProtocolBuilder(String id) {
        return protocolBuilders.computeIfAbsent(id, this::createProtocolBuilder);
    }

    private ServiceBuilder initServiceBuilder(String id) {
        return serviceBuilders.computeIfAbsent(id, this::createServiceBuilder);
    }

    private ReferenceBuilder initReferenceBuilder(String id) {
        return referenceBuilders.computeIfAbsent(id, this::createReferenceBuilder);
    }

    /* serve for builder apis, end */

    private void startMetadataReport() {
        ApplicationConfig applicationConfig = ConfigManager.getInstance().getApplication().orElseThrow(() -> new IllegalStateException("There's no ApplicationConfig specified."));

        // FIXME, multiple metadata config support.
        Set<MetadataReportConfig> metadataReportConfigs = ConfigManager.getInstance().getMetadataConfigs();
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
        Set<ConfigCenterConfig> configCenters = ConfigManager.getInstance().getConfigCenters();

        if (CollectionUtils.isNotEmpty(configCenters)) {
            CompositeDynamicConfiguration compositeDynamicConfiguration = new CompositeDynamicConfiguration();
            for (ConfigCenterConfig configCenter : configCenters) {
                configCenter.refresh();
                compositeDynamicConfiguration.addConfiguration(prepareEnvironment(configCenter));
            }
            Environment.getInstance().setDynamicConfiguration(compositeDynamicConfiguration);
        }
        ConfigManager.getInstance().refreshAll();
    }

    private DynamicConfiguration prepareEnvironment(ConfigCenterConfig configCenter) {
        if (configCenter.isValid()) {
            if (!configCenter.checkOrUpdateInited()) {
                return null;
            }
            DynamicConfiguration dynamicConfiguration = getDynamicConfiguration(configCenter.toUrl());
            String configContent = dynamicConfiguration.getConfigs(configCenter.getConfigFile(), configCenter.getGroup());

            String appGroup = ConfigManager.getInstance().getApplication().orElse(new ApplicationConfig()).getName();
            String appConfigContent = null;
            if (StringUtils.isNotEmpty(appGroup)) {
                appConfigContent = dynamicConfiguration.getConfigs
                        (StringUtils.isNotEmpty(configCenter.getAppConfigFile()) ? configCenter.getAppConfigFile() : configCenter.getConfigFile(),
                                appGroup
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

    private DynamicConfiguration getDynamicConfiguration(URL url) {
        DynamicConfigurationFactory factory = ExtensionLoader
                .getExtensionLoader(DynamicConfigurationFactory.class)
                .getExtension(url.getProtocol());
        return factory.getDynamicConfiguration(url);
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
                                            List<RegistryConfig> globalRegistryConfigs,
                                            List<ProtocolConfig> globalProtocolConfigs) {
        ConfigurableMetadataServiceExporter exporter = new ConfigurableMetadataServiceExporter();
        exporter.setApplicationConfig(applicationConfig);
        exporter.setRegistries(globalRegistryConfigs);
        exporter.setProtocols(globalProtocolConfigs);
        return exporter.export();
    }

    private void buildApplicationConfig() {
        ApplicationConfig applicationConfig = null;
        if (applicationBuilder != null) {
            applicationConfig = applicationBuilder.build();
        }
        ConfigManager.getInstance().setApplication(applicationConfig);
    }

    private void buildProtocolConfigs() {
        List<ProtocolConfig> protocolConfigs = buildConfigs(protocolBuilders);
        ConfigManager.getInstance().addProtocols(protocolConfigs, true);
    }

    private void buildRegistryConfigs() {
        List<RegistryConfig> registryConfigs = buildConfigs(registryBuilders);
        ConfigManager.getInstance().addRegistries(registryConfigs, true);
    }

    private void buildServiceConfigs() {
        List<ServiceConfig<?>> serviceConfigs = buildConfigs(serviceBuilders);
        serviceConfigs.forEach(ConfigManager.getInstance()::addService);
    }

    private void buildReferenceConfigs() {
        List<ReferenceConfig<?>> referenceConfigs = buildConfigs(referenceBuilders);
        referenceConfigs.forEach(ConfigManager.getInstance()::addReference);
    }

    private void exportServices() {
        ConfigManager.getInstance().getServiceConfigs().forEach(this::exportServiceConfig);
    }

    public void exportServiceConfig(ServiceConfig<?> serviceConfig) {
        serviceConfig.export();
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
        ConfigManager.getInstance().getProtocols().values().forEach(ProtocolConfig::destroy);
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ProtocolConfigs have been destroyed.");
        }
    }

    private void destroyReferenceConfigs() {
        ConfigManager.getInstance().getReferenceConfigs().forEach(ReferenceConfig::destroy);
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ReferenceConfigs have been destroyed.");
        }
    }

    private void clear() {

        clearBuilders();

        clearConfigs();

        ConfigManager.getInstance().clear();
    }

    private void clearConfigs() {
        ConfigManager.getInstance().clear();
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s configs have been clear.");
        }
    }

    private void clearBuilders() {
        this.applicationBuilder = null;
        this.registryBuilders.clear();
        this.protocolBuilders.clear();
        this.serviceBuilders.clear();
        this.referenceBuilders.clear();
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s builders have been clear.");
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

    private static <C extends AbstractConfig, B extends AbstractBuilder> List<C> buildConfigs(Map<String, B> map) {
        List<C> configs = new ArrayList<>();
        map.entrySet().forEach(entry -> {
            configs.add((C) entry.getValue().build());
        });
        return configs;
    }
}
