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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
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
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.store.RemoteWritableMetadataService;
import org.apache.dubbo.registry.client.AbstractServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.config.ConfigurationUtils.parseProperties;
import static org.apache.dubbo.common.config.configcenter.DynamicConfiguration.getDynamicConfiguration;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPERATOR;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_DEFAULT;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_REMOTE;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.config.context.ConfigManager.getInstance;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;

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

    private ReferenceConfigCache cache;

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

    public DubboBootstrap metadataReports(List<MetadataReportConfig> metadataReportConfigs) {
        if (CollectionUtils.isEmpty(metadataReportConfigs)) {
            return this;
        }

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
    public DubboBootstrap registries(List<RegistryConfig> registryConfigs) {
        if (CollectionUtils.isEmpty(registryConfigs)) {
            return this;
        }
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
        if (CollectionUtils.isEmpty(protocolConfigs)) {
            return this;
        }
        configManager.addProtocols(protocolConfigs);
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

    public DubboBootstrap services(List<ServiceConfig> serviceConfigs) {
        if (CollectionUtils.isEmpty(serviceConfigs)) {
            return this;
        }
        serviceConfigs.forEach(configManager::addService);
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

    public DubboBootstrap references(List<ReferenceConfig> referenceConfigs) {
        if (CollectionUtils.isEmpty(referenceConfigs)) {
            return this;
        }

        referenceConfigs.forEach(configManager::addReference);
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
        if (CollectionUtils.isEmpty(providerConfigs)) {
            return this;
        }

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
        if (CollectionUtils.isEmpty(consumerConfigs)) {
            return this;
        }

        consumerConfigs.forEach(configManager::addConsumer);
        return this;
    }

    // {@link ConfigCenterConfig} correlative methods
    public DubboBootstrap configCenter(ConfigCenterConfig configCenterConfig) {
        return configCenters(asList(configCenterConfig));
    }

    public DubboBootstrap configCenters(List<ConfigCenterConfig> configCenterConfigs) {
        if (CollectionUtils.isEmpty(configCenterConfigs)) {
            return this;
        }
        configManager.addConfigCenters(configCenterConfigs);
        return this;
    }

    public DubboBootstrap monitor(MonitorConfig monitor) {
        configManager.setMonitor(monitor);
        return this;
    }

    public DubboBootstrap metrics(MetricsConfig metrics) {
        configManager.setMetrics(metrics);
        return this;
    }

    public DubboBootstrap module(ModuleConfig module) {
        configManager.setModule(module);
        return this;
    }

    public DubboBootstrap cache(ReferenceConfigCache cache) {
        this.cache = cache;
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

        configManager.addProtocols(tmpProtocols);
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

        configManager.getDefaultRegistries().stream()
                .filter(registryConfig -> registryConfig.getUseAsConfigCenter() == null || registryConfig.getUseAsConfigCenter())
                .forEach(registryConfig -> {
                    String protocol = registryConfig.getProtocol();
                    String id = "config-center-" + protocol + "-" + registryConfig.getPort();
                    ConfigCenterConfig cc = new ConfigCenterConfig();
                    cc.setId(id);
                    cc.setParameters(registryConfig.getParameters() == null ?
                            new HashMap<>() :
                            new HashMap<>(registryConfig.getParameters()));
                    cc.getParameters().put(CLIENT_KEY, registryConfig.getClient());
                    cc.setProtocol(registryConfig.getProtocol());
                    cc.setAddress(registryConfig.getAddress());
                    cc.setNamespace(registryConfig.getGroup());
                    cc.setHighestPriority(false);
                    configManager.addConfigCenter(cc);
                });
        startConfigCenter();
    }

    private Collection<ServiceDiscovery> getServiceDiscoveries() {
        return AbstractServiceDiscoveryFactory.getDiscoveries();
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
                // TODO, only export to default registry?
                ApplicationConfig applicationConfig = configManager.getApplication().orElseThrow(() -> new IllegalStateException("ApplicationConfig cannot be null"));
                if (!METADATA_REMOTE.equals(applicationConfig.getMetadata())) {
                    exportMetadataService(
                            applicationConfig,
                            configManager.getRegistries(),
                            configManager.getProtocols()
                    );
                }
                /**
                 * Register the local {@link ServiceInstance}
                 */
                registerServiceInstance(applicationConfig);
            }

            referServices();

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
        ApplicationConfig applicationConfig = configManager.getApplication().orElseThrow(
                () -> new IllegalStateException("There's no ApplicationConfig specified.")
        );

        String metadataType = applicationConfig.getMetadata();
        // FIXME, multiple metadata config support.
        Collection<MetadataReportConfig> metadataReportConfigs = configManager.getMetadataConfigs();
        if (CollectionUtils.isEmpty(metadataReportConfigs)) {
            if (METADATA_REMOTE.equals(metadataType)) {
                throw new IllegalStateException("No MetadataConfig found, you must specify the remote Metadata Center address when set 'metadata=remote'.");
            }
            return;
        }
        MetadataReportConfig metadataReportConfig = metadataReportConfigs.iterator().next();
        if (!metadataReportConfig.isValid()) {
            return;
        }

        RemoteWritableMetadataService remoteMetadataService =
                (RemoteWritableMetadataService) WritableMetadataService.getExtension(metadataType);
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
            String configContent = dynamicConfiguration.getProperties(configCenter.getConfigFile(), configCenter.getGroup());

            String appGroup = configManager.getApplication().orElse(new ApplicationConfig()).getName();
            String appConfigContent = null;
            if (isNotEmpty(appGroup)) {
                appConfigContent = dynamicConfiguration.getProperties
                        (isNotEmpty(configCenter.getAppConfigFile()) ? configCenter.getAppConfigFile() : configCenter.getConfigFile(),
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

    private void referServices() {
        if (cache == null) {
            cache = ReferenceConfigCache.getCache();
        }
        configManager.getReferenceConfigs().forEach(cache::get);
    }

    public boolean isOnlyRegisterProvider() {
        return onlyRegisterProvider;
    }

    private void registerServiceInstance(ApplicationConfig applicationConfig) {
        ExtensionLoader<Protocol> loader = ExtensionLoader.getExtensionLoader(Protocol.class);
        Set<String> protocols = loader.getLoadedExtensions();
        if (CollectionUtils.isEmpty(protocols)) {
            throw new IllegalStateException("There should has at least one Protocol specified.");
        }

        String protocol = findOneProtocolForServiceInstance(protocols);

        Protocol protocolInstance = loader.getExtension(protocol);

        String serviceName = applicationConfig.getName();
        // TODO, only support exporting one server
        ProtocolServer server = protocolInstance.getServers().get(0);
        String[] address = server.getAddress().split(GROUP_CHAR_SEPERATOR);
        String host = address[0];
        int port = Integer.parseInt(address[1]);

        ServiceInstance serviceInstance = initServiceInstance(
                serviceName,
                host,
                port,
                applicationConfig.getMetadata() == null ? METADATA_DEFAULT : applicationConfig.getMetadata()
        );

        getServiceDiscoveries().forEach(serviceDiscovery -> serviceDiscovery.register(serviceInstance));
    }

    /**
     * Use rest protocol if there's one, otherwise, choose the first one available.
     *
     * @return
     */
    private String findOneProtocolForServiceInstance(Set<String> protocols) {
        String result = null;
        for (String protocol : protocols) {
            if ("rest".equalsIgnoreCase(protocol)) {
                result = protocol;
                break;
            }
        }

        if (result == null) {
            for (String protocol : protocols) {
                if (!"injvm".equalsIgnoreCase(protocol) && "registry".equalsIgnoreCase(protocol)) {
                    result = protocol;
                    break;
                }
            }
        }
        return result;
    }

    private void unregisterServiceInstance() {

        if (serviceInstance != null) {
            getServiceDiscoveries().forEach(serviceDiscovery -> {
                serviceDiscovery.unregister(serviceInstance);
            });
        }

    }

    private ServiceInstance initServiceInstance(String serviceName, String host, int port, String metadataType) {
        this.serviceInstance = new DefaultServiceInstance(serviceName, host, port);
        this.serviceInstance.getMetadata().put(METADATA_KEY, metadataType);
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
