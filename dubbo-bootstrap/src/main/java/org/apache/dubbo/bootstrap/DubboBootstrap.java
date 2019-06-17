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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.builders.AbstractBuilder;
import org.apache.dubbo.config.builders.ApplicationBuilder;
import org.apache.dubbo.config.builders.ProtocolBuilder;
import org.apache.dubbo.config.builders.ReferenceBuilder;
import org.apache.dubbo.config.builders.RegistryBuilder;
import org.apache.dubbo.config.builders.ServiceBuilder;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.support.ServiceOrientedRegistry;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.common.utils.StringUtils.split;
import static org.apache.dubbo.common.utils.StringUtils.trim;
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

    private final MetadataServiceExporter metadataServiceExporter = new ConfigurableMetadataServiceExporter();

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

    private Map<String, RegistryBuilder> registryBuilders = new HashMap<>();

    private Map<String, ProtocolBuilder> protocolBuilders = new HashMap<>();

    private Map<String, ServiceBuilder<?>> serviceBuilders = new HashMap<>();

    private Map<String, ReferenceBuilder<?>> referenceBuilders = new HashMap<>();

    /**
     * The global {@link ApplicationConfig}
     */
    private ApplicationConfig applicationConfig;

    /**
     * the global {@link RegistryConfig registries}
     */
    private Map<String, RegistryConfig> registryConfigs = emptyMap();

    /**
     * the global {@link RegistryConfig registries}
     */
    private Map<String, ProtocolConfig> protocolConfigs = emptyMap();

    /**
     * the global {@link ServiceConfig services}
     */
    private Map<String, ServiceConfig<?>> serviceConfigs = emptyMap();

    /**
     * the global {@link ReferenceConfig references}
     */
    private Map<String, ReferenceConfig<?>> referenceConfigs = new HashMap<>();

    public ApplicationSettings application(String name) {
        return new ApplicationSettings(initApplicationBuilder(name), this);
    }

    public RegistrySettings registry() {
        return registry(DEFAULT_REGISTRY_ID);
    }

    public RegistrySettings registry(String id) {
        return new RegistrySettings(initRegistryBuilder(id), this);
    }

    public ProtocolSettings protocol() {
        return protocol(DEFAULT_PROTOCOL_ID);
    }

    public ProtocolSettings protocol(String id) {
        return new ProtocolSettings(initProtocolBuilder(id), this);
    }

    public <S> ServiceSettings<S> service(String id) {
        return new ServiceSettings(initServiceBuilder(id), this);
    }

    public <S> ReferenceSettings<S> reference(String id) {
        return new ReferenceSettings<>(initReferenceBuilder(id), this);
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

        initApplicationConfig();

        initRegistryConfigs();

        initProtocolConfigs();

        initServiceConfigs();

        initReferenceConfigs();

        clearBuilders();

        initialized = true;

        if (logger.isInfoEnabled()) {
            logger.info(NAME + " has been initialized!");
        }
    }

    /**
     * Get the {@link ServiceConfig} by specified id
     *
     * @param id  The {@link ServiceConfig#getId() id} of {@link ServiceConfig}
     * @param <S> the type of service interface
     * @return <code>null</code> if not found
     */
    public <S> ServiceConfig<S> serviceConfig(String id) {
        return (ServiceConfig<S>) serviceConfigs.get(id);
    }

    /**
     * Get the {@link ReferenceConfig} by specified id
     *
     * @param id  The {@link ReferenceConfig#getId() id} of {@link ReferenceConfig}
     * @param <S> the type of service interface
     * @return <code>null</code> if not found
     */
    public <S> ReferenceConfig<S> referenceConfig(String id) {
        return (ReferenceConfig<S>) referenceConfigs.get(id);
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
            if (!onlyRegisterProvider && !serviceConfigs.isEmpty()) {
                /**
                 * export {@link MetadataService}
                 */
                List<URL> exportedURLs = exportMetadataService(applicationConfig, registryConfigs, protocolConfigs);

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

    private void initApplicationConfig() {
        this.applicationConfig = buildApplicationConfig();
    }

    private void initRegistryConfigs() {
        this.registryConfigs = buildRegistryConfigs();
    }

    private void initProtocolConfigs() {
        this.protocolConfigs = buildProtocolConfigs();
    }

    private void initReferenceConfigs() {
        this.referenceConfigs = buildReferenceConfigs();
        this.referenceConfigs.values().forEach(this::initReferenceConfig);
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

    private void initServiceConfigs() {
        this.serviceConfigs = buildServiceConfigs();
        this.serviceConfigs.values().forEach(this::initServiceConfig);
    }

    private List<URL> exportMetadataService(ApplicationConfig applicationConfig,
                                            Map<String, RegistryConfig> globalRegistryConfigs,
                                            Map<String, ProtocolConfig> globalProtocolConfigs) {
        ConfigurableMetadataServiceExporter exporter = new ConfigurableMetadataServiceExporter();
        exporter.setApplicationConfig(applicationConfig);
        exporter.setRegistries(globalRegistryConfigs.values());
        exporter.setProtocols(globalProtocolConfigs.values());
        return exporter.export();
    }

    private ApplicationConfig buildApplicationConfig() {
        return applicationBuilder.build();
    }

    private Map<String, ProtocolConfig> buildProtocolConfigs() {
        return buildConfigs(protocolBuilders);
    }

    private Map<String, RegistryConfig> buildRegistryConfigs() {
        return buildConfigs(registryBuilders);
    }

    private Map<String, ServiceConfig<?>> buildServiceConfigs() {
        return buildConfigs(serviceBuilders);
    }

    private Map<String, ReferenceConfig<?>> buildReferenceConfigs() {
        return buildConfigs(referenceBuilders);
    }

    private void exportServices() {
        serviceConfigs.values().forEach(this::exportServiceConfig);
    }

    private void initServiceConfig(ServiceConfig<?> serviceConfig) {
        initConfig(serviceConfig);
        initProtocols(serviceConfig);
    }

    private void initReferenceConfig(ReferenceConfig<?> referenceConfig) {
        initConfig(referenceConfig);
    }

    private void initConfig(AbstractInterfaceConfig config) {
        initApplication(config);
        initRegistries(config);
    }

    private void initApplication(AbstractInterfaceConfig config) {
        if (config.getApplication() == null) {
            config.setApplication(applicationConfig);
        }
    }

    private void initRegistries(AbstractInterfaceConfig config) {
        List<RegistryConfig> registries = config.getRegistries();
        if (CollectionUtils.isEmpty(registries)) { // If no registry present
            registries = new LinkedList<>();
            String registerIds = config.getRegistryIds();
            if (!isBlank(registerIds)) {
                for (String id : split(registerIds, ',')) {
                    RegistryConfig registryConfig = registryConfigs.get(trim(id));
                    registries.add(registryConfig);
                }
            }
            if (registries.isEmpty()) { // If empty, add all global registries
                registries.addAll(registryConfigs.values());
            }

            config.setRegistries(registries);
        }
    }

    private void initProtocols(ServiceConfig<?> serviceConfig) {
        List<ProtocolConfig> protocols = serviceConfig.getProtocols();
        if (CollectionUtils.isEmpty(protocols)) { // If no protocols present
            protocols = new LinkedList<>();
            String protocolIds = serviceConfig.getProtocolIds();
            if (!isBlank(protocolIds)) {
                for (String id : split(protocolIds, ',')) {
                    ProtocolConfig protocol = protocolConfigs.get(trim(id));
                    protocols.add(protocol);
                }
            }
            if (protocols.isEmpty()) { // If empty, add all global protocols
                protocols.addAll(protocolConfigs.values());
            }
            serviceConfig.setProtocols(protocols);
        }
    }

    private void exportServiceConfig(ServiceConfig<?> serviceConfig) {
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
        protocolConfigs.values().forEach(ProtocolConfig::destroy);
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ProtocolConfigs have been destroyed.");
        }
    }

    private void destroyReferenceConfigs() {
        referenceConfigs.values().forEach(ReferenceConfig::destroy);
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
        this.applicationConfig = null;
        this.registryConfigs.clear();
        this.protocolConfigs.clear();
        this.serviceConfigs.clear();
        this.referenceConfigs.clear();
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

    private static <C extends AbstractConfig, B extends AbstractBuilder> Map<String, C> buildConfigs(Map<String, B> map) {
        Map<String, C> configs = new HashMap<>();
        map.entrySet().forEach(entry -> {
            configs.put(entry.getKey(), (C) entry.getValue().build());
        });
        return configs;
    }
}
