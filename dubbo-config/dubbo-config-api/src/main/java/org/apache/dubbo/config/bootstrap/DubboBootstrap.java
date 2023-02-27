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
package org.apache.dubbo.config.bootstrap;

<<<<<<< HEAD
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
=======
>>>>>>> origin/3.2
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.DeployListenerAdapter;
import org.apache.dubbo.common.extension.ExtensionLoader;
<<<<<<< HEAD
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
=======
>>>>>>> origin/3.2
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.bootstrap.builders.ApplicationBuilder;
import org.apache.dubbo.config.bootstrap.builders.ConfigCenterBuilder;
import org.apache.dubbo.config.bootstrap.builders.ConsumerBuilder;
import org.apache.dubbo.config.bootstrap.builders.MetadataReportBuilder;
import org.apache.dubbo.config.bootstrap.builders.ProtocolBuilder;
import org.apache.dubbo.config.bootstrap.builders.ProviderBuilder;
import org.apache.dubbo.config.bootstrap.builders.ReferenceBuilder;
import org.apache.dubbo.config.bootstrap.builders.RegistryBuilder;
import org.apache.dubbo.config.bootstrap.builders.ServiceBuilder;
import org.apache.dubbo.config.context.ConfigManager;
<<<<<<< HEAD
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.report.MetadataReportFactory;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReportFactory;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.store.InMemoryWritableMetadataService;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
=======
>>>>>>> origin/3.2
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;
<<<<<<< HEAD
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
=======
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
>>>>>>> origin/3.2
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
<<<<<<< HEAD
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.config.ConfigurationUtils.parseProperties;
import static org.apache.dubbo.common.config.configcenter.DynamicConfiguration.getDynamicConfiguration;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.function.ThrowableAction.execute;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_METADATA_PUBLISH_DELAY;
import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PUBLISH_DELAY_KEY;
import static org.apache.dubbo.metadata.WritableMetadataService.getDefaultExtension;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.calInstanceRevision;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setMetadataStorageType;
import static org.apache.dubbo.registry.support.AbstractRegistryFactory.getServiceDiscoveries;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;
=======

import static java.util.Collections.singletonList;
>>>>>>> origin/3.2

/**
 * See {@link ApplicationModel} and {@link ExtensionLoader} for why this class is designed to be singleton.
 * <p>
 * The bootstrap class of Dubbo
 * <p>
 * Get singleton instance by calling static method {@link #getInstance()}.
 * Designed as singleton because some classes inside Dubbo, such as ExtensionLoader, are designed only for one instance per process.
 *
 * @since 2.7.5
 */
<<<<<<< HEAD
public class DubboBootstrap {

    public static final String DEFAULT_REGISTRY_ID = "REGISTRY#DEFAULT";

    public static final String DEFAULT_PROTOCOL_ID = "PROTOCOL#DEFAULT";

    public static final String DEFAULT_SERVICE_ID = "SERVICE#DEFAULT";

    public static final String DEFAULT_REFERENCE_ID = "REFERENCE#DEFAULT";

    public static final String DEFAULT_PROVIDER_ID = "PROVIDER#DEFAULT";

    public static final String DEFAULT_CONSUMER_ID = "CONSUMER#DEFAULT";
=======
public final class DubboBootstrap {
>>>>>>> origin/3.2

    private static final String NAME = DubboBootstrap.class.getSimpleName();

    private static final Logger logger = LoggerFactory.getLogger(DubboBootstrap.class);

    private static volatile ConcurrentMap<ApplicationModel, DubboBootstrap> instanceMap = new ConcurrentHashMap<>();
    private static volatile DubboBootstrap instance;

    private final AtomicBoolean awaited = new AtomicBoolean(false);

    private volatile BootstrapTakeoverMode takeoverMode = BootstrapTakeoverMode.AUTO;

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private ExecutorRepository executorRepository;

    private final ApplicationModel applicationModel;

<<<<<<< HEAD
    private final ExecutorRepository executorRepository = getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
=======
    protected final ConfigManager configManager;

    protected final Environment environment;
>>>>>>> origin/3.2

    private ApplicationDeployer applicationDeployer;

    /**
     * See {@link ApplicationModel} and {@link ExtensionLoader} for why DubboBootstrap is designed to be singleton.
     */
    public static DubboBootstrap getInstance() {
        if (instance == null) {
            synchronized (DubboBootstrap.class) {
                if (instance == null) {
                    instance = DubboBootstrap.getInstance(ApplicationModel.defaultModel());
                }
            }
        }
        return instance;
    }

    public static DubboBootstrap getInstance(ApplicationModel applicationModel) {
        return ConcurrentHashMapUtils.computeIfAbsent(instanceMap, applicationModel, _k -> new DubboBootstrap(applicationModel));
    }

    public static DubboBootstrap newInstance() {
        return getInstance(FrameworkModel.defaultModel().newApplication());
    }

    public static DubboBootstrap newInstance(FrameworkModel frameworkModel) {
        return getInstance(frameworkModel.newApplication());
    }

    /**
     * Try reset dubbo status for new instance.
     *
     * @deprecated For testing purposes only
     */
    @Deprecated
    public static void reset() {
        reset(true);
    }

    /**
     * Try reset dubbo status for new instance.
     *
     * @deprecated For testing purposes only
     */
    @Deprecated
    public static void reset(boolean destroy) {
        if (destroy) {
            if (instance != null) {
                instance.destroy();
                instance = null;
            }
            FrameworkModel.destroyAll();
        } else {
            instance = null;
        }

<<<<<<< HEAD
    private AtomicBoolean ready = new AtomicBoolean(false);
=======
        ApplicationModel.reset();
    }
>>>>>>> origin/3.2

    private DubboBootstrap(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        configManager = applicationModel.getApplicationConfigManager();
        environment = applicationModel.getModelEnvironment();

        executorRepository = ExecutorRepository.getInstance(applicationModel);
        applicationDeployer = applicationModel.getDeployer();
        // listen deploy events
        applicationDeployer.addDeployListener(new DeployListenerAdapter<ApplicationModel>() {
            @Override
            public void onStarted(ApplicationModel scopeModel) {
                notifyStarted(applicationModel);
            }

            @Override
            public void onStopped(ApplicationModel scopeModel) {
                notifyStopped(applicationModel);
            }

<<<<<<< HEAD
    private volatile MetadataServiceExporter metadataServiceExporter;

    private Map<String, ServiceConfigBase<?>> exportedServices = new ConcurrentHashMap<>();
=======
            @Override
            public void onFailure(ApplicationModel scopeModel, Throwable cause) {
                notifyStopped(applicationModel);
            }
        });
        // register DubboBootstrap bean
        applicationModel.getBeanFactory().registerBean(this);
    }

    private void notifyStarted(ApplicationModel applicationModel) {
        ExtensionLoader<DubboBootstrapStartStopListener> exts = applicationModel.getExtensionLoader(DubboBootstrapStartStopListener.class);
        exts.getSupportedExtensionInstances().forEach(ext -> ext.onStart(DubboBootstrap.this));
    }
>>>>>>> origin/3.2

    private void notifyStopped(ApplicationModel applicationModel) {
        ExtensionLoader<DubboBootstrapStartStopListener> exts = applicationModel.getExtensionLoader(DubboBootstrapStartStopListener.class);
        exts.getSupportedExtensionInstances().forEach(ext -> ext.onStop(DubboBootstrap.this));
        executeMutually(() -> {
            awaited.set(true);
            condition.signalAll();
        });
        instanceMap.remove(applicationModel);
    }

    /**
     * Initialize
     */
    public void initialize() {
        applicationDeployer.initialize();
    }

    /**
     * Start dubbo application and wait for finish
     */
    public DubboBootstrap start() {
        this.start(true);
        return this;
    }

    /**
     * Start dubbo application
     *
     * @param wait If true, wait for startup to complete, or else no waiting.
     * @return
     */
    public DubboBootstrap start(boolean wait) {
        Future future = applicationDeployer.start();
        if (wait) {
            try {
                future.get();
            } catch (Exception e) {
                throw new IllegalStateException("await dubbo application start finish failure", e);
            }
        }
        return this;
    }

    /**
     * Start dubbo application but no wait for finish.
     *
     * @return the future object
     */
    public Future asyncStart() {
        return applicationDeployer.start();
    }

<<<<<<< HEAD
        DubboShutdownHook.getDubboShutdownHook().register();
        ShutdownHookCallbacks.INSTANCE.addCallback(DubboBootstrap.this::destroy);
=======
    /**
     * Stop dubbo application
     *
     * @return
     * @throws IllegalStateException
     */
    public DubboBootstrap stop() throws IllegalStateException {
        destroy();
        return this;
    }

    public void destroy() {
        applicationModel.destroy();
    }

    public boolean isInitialized() {
        return applicationDeployer.isInitialized();
    }

    public boolean isPending() {
        return applicationDeployer.isPending();
    }

    /**
     * @return true if the dubbo application is starting or has been started.
     */
    public boolean isRunning() {
        return applicationDeployer.isRunning();
>>>>>>> origin/3.2
    }

    /**
     * @return true if the dubbo application is starting.
     * @see #isStarted()
     */
    public boolean isStarting() {
        return applicationDeployer.isStarting();
    }

    /**
     * @return true if the dubbo application has been started.
     * @see #start()
     * @see #isStarting()
     */
    public boolean isStarted() {
        return applicationDeployer.isStarted();
    }

    /**
     * @return true if the dubbo application is stopping.
     * @see #isStopped()
     */
    public boolean isStopping() {
        return applicationDeployer.isStopping();
    }

    /**
     * @return true if the dubbo application is stopping.
     * @see #isStopped()
     */
    public boolean isStopped() {
        return applicationDeployer.isStopped();
    }

    /**
     * Block current thread to be await.
     *
     * @return {@link DubboBootstrap}
     */
    public DubboBootstrap await() {
        // if has been waited, no need to wait again, return immediately
        if (!awaited.get()) {
            if (!isStopped()) {
                executeMutually(() -> {
                    while (!awaited.get()) {
                        if (logger.isInfoEnabled()) {
                            logger.info(NAME + " awaiting ...");
                        }
                        try {
                            condition.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
        }
        return this;
    }

    public ReferenceCache getCache() {
        return applicationDeployer.getReferenceCache();
    }

    private void executeMutually(Runnable runnable) {
        try {
            lock.lock();
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    public ApplicationConfig getApplication() {
        return configManager.getApplicationOrElseThrow();
    }

    public void setTakeoverMode(BootstrapTakeoverMode takeoverMode) {
        //TODO this.started.set(false);
        this.takeoverMode = takeoverMode;
    }

    public BootstrapTakeoverMode getTakeoverMode() {
        return takeoverMode;
    }

    public ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }


    // MetadataReportConfig correlative methods

    public DubboBootstrap metadataReport(Consumer<MetadataReportBuilder> consumerBuilder) {
        return metadataReport(null, consumerBuilder);
    }

    public DubboBootstrap metadataReport(String id, Consumer<MetadataReportBuilder> consumerBuilder) {
        MetadataReportBuilder metadataReportBuilder = createMetadataReportBuilder(id);
        consumerBuilder.accept(metadataReportBuilder);
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
        applicationConfig.setScopeModel(applicationModel);
        configManager.setApplication(applicationConfig);
        return this;
    }


    // {@link RegistryConfig} correlative methods

    /**
     * Add an instance of {@link RegistryConfig}
     *
     * @param consumerBuilder the {@link Consumer} of {@link RegistryBuilder}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap registry(Consumer<RegistryBuilder> consumerBuilder) {
        return registry(null, consumerBuilder);
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
        registryConfig.setScopeModel(applicationModel);
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
        return protocol(null, consumerBuilder);
    }

    public DubboBootstrap protocol(String id, Consumer<ProtocolBuilder> consumerBuilder) {
        ProtocolBuilder builder = createProtocolBuilder(id);
        consumerBuilder.accept(builder);
        return protocol(builder.build());
    }

    public DubboBootstrap protocol(ProtocolConfig protocolConfig) {
        return protocols(singletonList(protocolConfig));
    }

    public DubboBootstrap protocols(List<ProtocolConfig> protocolConfigs) {
        if (CollectionUtils.isEmpty(protocolConfigs)) {
            return this;
        }
        for (ProtocolConfig protocolConfig : protocolConfigs) {
            protocolConfig.setScopeModel(applicationModel);
            configManager.addProtocol(protocolConfig);
        }
        return this;
    }

    // {@link ServiceConfig} correlative methods
    public <S> DubboBootstrap service(Consumer<ServiceBuilder<S>> consumerBuilder) {
        return service(null, consumerBuilder);
    }

    public <S> DubboBootstrap service(String id, Consumer<ServiceBuilder<S>> consumerBuilder) {
        return service(createServiceConfig(id, consumerBuilder));
    }

    private <S> ServiceConfig createServiceConfig(String id, Consumer<ServiceBuilder<S>> consumerBuilder) {
        ServiceBuilder builder = createServiceBuilder(id);
        consumerBuilder.accept(builder);
        ServiceConfig serviceConfig = builder.build();
        return serviceConfig;
    }

    public DubboBootstrap services(List<ServiceConfig> serviceConfigs) {
        if (CollectionUtils.isEmpty(serviceConfigs)) {
            return this;
        }
        for (ServiceConfig serviceConfig : serviceConfigs) {
            this.service(serviceConfig);
        }
        return this;
    }

    public DubboBootstrap service(ServiceConfig<?> serviceConfig) {
        this.service(serviceConfig, applicationModel.getDefaultModule());
        return this;
    }

    public DubboBootstrap service(ServiceConfig<?> serviceConfig, ModuleModel moduleModel) {
        serviceConfig.setScopeModel(moduleModel);
        moduleModel.getConfigManager().addService(serviceConfig);
        return this;
    }

    // {@link Reference} correlative methods
    public <S> DubboBootstrap reference(Consumer<ReferenceBuilder<S>> consumerBuilder) {
        return reference(null, consumerBuilder);
    }

    public <S> DubboBootstrap reference(String id, Consumer<ReferenceBuilder<S>> consumerBuilder) {
        return reference(createReferenceConfig(id, consumerBuilder));
    }

    private <S> ReferenceConfig createReferenceConfig(String id, Consumer<ReferenceBuilder<S>> consumerBuilder) {
        ReferenceBuilder builder = createReferenceBuilder(id);
        consumerBuilder.accept(builder);
        ReferenceConfig referenceConfig = builder.build();
        return referenceConfig;
    }

    public DubboBootstrap references(List<ReferenceConfig> referenceConfigs) {
        if (CollectionUtils.isEmpty(referenceConfigs)) {
            return this;
        }
        for (ReferenceConfig referenceConfig : referenceConfigs) {
            this.reference(referenceConfig);
        }
        return this;
    }

    public DubboBootstrap reference(ReferenceConfig<?> referenceConfig) {
        return reference(referenceConfig, applicationModel.getDefaultModule());
    }

    public DubboBootstrap reference(ReferenceConfig<?> referenceConfig, ModuleModel moduleModel) {
        referenceConfig.setScopeModel(moduleModel);
        moduleModel.getConfigManager().addReference(referenceConfig);
        return this;
    }

    // {@link ProviderConfig} correlative methods
    public DubboBootstrap provider(Consumer<ProviderBuilder> builderConsumer) {
        provider(null, builderConsumer);
        return this;
    }

    public DubboBootstrap provider(String id, Consumer<ProviderBuilder> builderConsumer) {
        this.provider(createProviderConfig(id, builderConsumer));
        return this;
    }

    private ProviderConfig createProviderConfig(String id, Consumer<ProviderBuilder> builderConsumer) {
        ProviderBuilder builder = createProviderBuilder(id);
        builderConsumer.accept(builder);
        ProviderConfig providerConfig = builder.build();
        return providerConfig;
    }

    public DubboBootstrap provider(ProviderConfig providerConfig) {
<<<<<<< HEAD
        return providers(singletonList(providerConfig));
=======
        return this.provider(providerConfig, applicationModel.getDefaultModule());
>>>>>>> origin/3.2
    }

    public DubboBootstrap providers(List<ProviderConfig> providerConfigs) {
        for (ProviderConfig providerConfig : providerConfigs) {
            this.provider(providerConfig, applicationModel.getDefaultModule());
        }
        return this;
    }

    public DubboBootstrap provider(ProviderConfig providerConfig, ModuleModel moduleModel) {
        providerConfig.setScopeModel(moduleModel);
        moduleModel.getConfigManager().addProvider(providerConfig);
        return this;
    }

    // {@link ConsumerConfig} correlative methods
    public DubboBootstrap consumer(Consumer<ConsumerBuilder> builderConsumer) {
        return consumer(null, builderConsumer);
    }

    public DubboBootstrap consumer(String id, Consumer<ConsumerBuilder> builderConsumer) {
        return consumer(createConsumerConfig(id, builderConsumer));
    }

    private ConsumerConfig createConsumerConfig(String id, Consumer<ConsumerBuilder> builderConsumer) {
        ConsumerBuilder builder = createConsumerBuilder(id);
        builderConsumer.accept(builder);
        ConsumerConfig consumerConfig = builder.build();
        return consumerConfig;
    }

    public DubboBootstrap consumer(ConsumerConfig consumerConfig) {
<<<<<<< HEAD
        return consumers(singletonList(consumerConfig));
=======
        return this.consumer(consumerConfig, applicationModel.getDefaultModule());
>>>>>>> origin/3.2
    }

    public DubboBootstrap consumers(List<ConsumerConfig> consumerConfigs) {
        for (ConsumerConfig consumerConfig : consumerConfigs) {
            this.consumer(consumerConfig, applicationModel.getDefaultModule());
        }
        return this;
    }

    public DubboBootstrap consumer(ConsumerConfig consumerConfig, ModuleModel moduleModel) {
        consumerConfig.setScopeModel(moduleModel);
        moduleModel.getConfigManager().addConsumer(consumerConfig);
        return this;
    }

    public DubboBootstrap module(ModuleConfig moduleConfig) {
        this.module(moduleConfig, applicationModel.getDefaultModule());
        return this;
    }

    public DubboBootstrap module(ModuleConfig moduleConfig, ModuleModel moduleModel) {
        moduleConfig.setScopeModel(moduleModel);
        moduleModel.getConfigManager().setModule(moduleConfig);
        return this;
    }
    // module configs end

    // {@link ConfigCenterConfig} correlative methods
    public DubboBootstrap configCenter(Consumer<ConfigCenterBuilder> consumerBuilder) {
        return configCenter(null, consumerBuilder);
    }

    public DubboBootstrap configCenter(String id, Consumer<ConfigCenterBuilder> consumerBuilder) {
        ConfigCenterBuilder configCenterBuilder = createConfigCenterBuilder(id);
        consumerBuilder.accept(configCenterBuilder);
        return this;
    }

    public DubboBootstrap configCenter(ConfigCenterConfig configCenterConfig) {
<<<<<<< HEAD
        return configCenters(singletonList(configCenterConfig));
=======
        configCenterConfig.setScopeModel(applicationModel);
        configManager.addConfigCenter(configCenterConfig);
        return this;
>>>>>>> origin/3.2
    }

    public DubboBootstrap configCenters(List<ConfigCenterConfig> configCenterConfigs) {
        if (CollectionUtils.isEmpty(configCenterConfigs)) {
            return this;
        }
        for (ConfigCenterConfig configCenterConfig : configCenterConfigs) {
            this.configCenter(configCenterConfig);
        }
        return this;
    }

    public DubboBootstrap monitor(MonitorConfig monitor) {
        monitor.setScopeModel(applicationModel);
        configManager.setMonitor(monitor);
        return this;
    }

    public DubboBootstrap metrics(MetricsConfig metrics) {
        metrics.setScopeModel(applicationModel);
        configManager.setMetrics(metrics);
        return this;
    }

    public DubboBootstrap ssl(SslConfig sslConfig) {
        sslConfig.setScopeModel(applicationModel);
        configManager.setSsl(sslConfig);
        return this;
    }

    /* serve for builder apis, begin */

    private ApplicationBuilder createApplicationBuilder(String name) {
        return new ApplicationBuilder().name(name);
    }

    private RegistryBuilder createRegistryBuilder(String id) {
        return new RegistryBuilder().id(id);
    }

    private MetadataReportBuilder createMetadataReportBuilder(String id) {
        return new MetadataReportBuilder().id(id);
    }

    private ConfigCenterBuilder createConfigCenterBuilder(String id) {
        return new ConfigCenterBuilder().id(id);
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

    public Module newModule() {
        return new Module(applicationModel.newModule());
    }

    public Module newModule(ModuleConfig moduleConfig) {
        ModuleModel moduleModel = applicationModel.newModule();
        moduleConfig.setScopeModel(moduleModel);
        moduleModel.getConfigManager().setModule(moduleConfig);
        return new Module(moduleModel);
    }

    public DubboBootstrap endModule() {
        return this;
    }

<<<<<<< HEAD
        if (logger.isInfoEnabled()) {
            logger.info(NAME + " has been initialized!");
=======

    public class Module {
        private ModuleModel moduleModel;
        private DubboBootstrap bootstrap;

        public Module(ModuleModel moduleModel) {
            this.moduleModel = moduleModel;
            this.bootstrap = DubboBootstrap.this;
>>>>>>> origin/3.2
        }

        public DubboBootstrap endModule() {
            return this.bootstrap.endModule();
        }

        public ModuleModel getModuleModel() {
            return moduleModel;
        }

<<<<<<< HEAD
        ApplicationConfig applicationConfig = getApplication();

        String metadataType = applicationConfig.getMetadataType();
        // FIXME, multiple metadata config support.
        Collection<MetadataReportConfig> metadataReportConfigs = configManager.getMetadataConfigs();
        if (CollectionUtils.isEmpty(metadataReportConfigs)) {
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                throw new IllegalStateException("No MetadataConfig found, Metadata Center address is required when 'metadata=remote' is enabled.");
            }
            return;
        }

        for (MetadataReportConfig metadataReportConfig : metadataReportConfigs) {
            ConfigValidationUtils.validateMetadataConfig(metadataReportConfig);
            if (!metadataReportConfig.isValid()) {
                continue;
            }
            MetadataReportInstance.init(metadataReportConfig);
        }
    }

    /**
     * For compatibility purpose, use registry as the default config center when
     * there's no config center specified explicitly and
     * useAsConfigCenter of registryConfig is null or true
     */
    private void useRegistryAsConfigCenterIfNecessary() {
        // we use the loading status of DynamicConfiguration to decide whether ConfigCenter has been initiated.
        if (environment.getDynamicConfiguration().isPresent()) {
            return;
        }

        if (CollectionUtils.isNotEmpty(configManager.getConfigCenters())) {
            return;
        }

        configManager
                .getDefaultRegistries()
                .stream()
                .filter(this::isUsedRegistryAsConfigCenter)
                .map(this::registryAsConfigCenter)
                .forEach(configManager::addConfigCenter);
    }

    private boolean isUsedRegistryAsConfigCenter(RegistryConfig registryConfig) {
        return isUsedRegistryAsCenter(registryConfig, registryConfig::getUseAsConfigCenter, "config",
                DynamicConfigurationFactory.class);
    }

    private ConfigCenterConfig registryAsConfigCenter(RegistryConfig registryConfig) {
        String protocol = registryConfig.getProtocol();
        Integer port = registryConfig.getPort();
        String id = "config-center-" + protocol + "-" + port;
        if (configManager.getConfigCenter(id) != null) {
            return null;
        }

        ConfigCenterConfig cc = new ConfigCenterConfig();
        cc.setId(id);
        if (cc.getParameters() == null) {
            cc.setParameters(new HashMap<>());
        }
        if (registryConfig.getParameters() != null) {
            cc.getParameters().putAll(registryConfig.getParameters()); // copy the parameters
        }
        cc.getParameters().put(CLIENT_KEY, registryConfig.getClient());
        cc.setProtocol(protocol);
        cc.setPort(port);
        if (StringUtils.isNotEmpty(registryConfig.getGroup())) {
            cc.setGroup(registryConfig.getGroup());
        }
        cc.setAddress(getRegistryCompatibleAddress(registryConfig));
        cc.setNamespace(registryConfig.getGroup());
        cc.setUsername(registryConfig.getUsername());
        cc.setPassword(registryConfig.getPassword());
        if (registryConfig.getTimeout() != null) {
            cc.setTimeout(registryConfig.getTimeout().longValue());
=======
        public Module config(ModuleConfig moduleConfig) {
            this.moduleModel.getConfigManager().setModule(moduleConfig);
            return this;
        }

        // {@link ServiceConfig} correlative methods
        public <S> Module service(Consumer<ServiceBuilder<S>> consumerBuilder) {
            return service(null, consumerBuilder);
>>>>>>> origin/3.2
        }

        public <S> Module service(String id, Consumer<ServiceBuilder<S>> consumerBuilder) {
            return service(createServiceConfig(id, consumerBuilder));
        }

        public Module services(List<ServiceConfig> serviceConfigs) {
            if (CollectionUtils.isEmpty(serviceConfigs)) {
                return this;
            }
            for (ServiceConfig serviceConfig : serviceConfigs) {
                this.service(serviceConfig);
            }
            return this;
        }

        public Module service(ServiceConfig<?> serviceConfig) {
            DubboBootstrap.this.service(serviceConfig, moduleModel);
            return this;
        }

        // {@link Reference} correlative methods
        public <S> Module reference(Consumer<ReferenceBuilder<S>> consumerBuilder) {
            return reference(null, consumerBuilder);
        }

<<<<<<< HEAD
    private MetadataReportConfig registryAsMetadataCenter(RegistryConfig registryConfig) {
        String protocol = registryConfig.getProtocol();
        Integer port = registryConfig.getPort();
        String id = "metadata-center-" + protocol + "-" + port;
        if (configManager.getMetadataConfig(id) != null) {
            return null;
        }

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setId(id);
        if (metadataReportConfig.getParameters() == null) {
            metadataReportConfig.setParameters(new HashMap<>());
        }
        if (registryConfig.getParameters() != null) {
            metadataReportConfig.getParameters().putAll(registryConfig.getParameters()); // copy the parameters
        }
        metadataReportConfig.getParameters().put(CLIENT_KEY, registryConfig.getClient());
        metadataReportConfig.setGroup(registryConfig.getGroup());
        metadataReportConfig.setAddress(getRegistryCompatibleAddress(registryConfig));
        metadataReportConfig.setUsername(registryConfig.getUsername());
        metadataReportConfig.setPassword(registryConfig.getPassword());
        metadataReportConfig.setTimeout(registryConfig.getTimeout());
        metadataReportConfig.setRegistry(registryConfig.getId());
        return metadataReportConfig;
    }

    private String getRegistryCompatibleAddress(RegistryConfig registryConfig) {
        String registryAddress = registryConfig.getAddress();
        String[] addresses = REGISTRY_SPLIT_PATTERN.split(registryAddress);
        if (ArrayUtils.isEmpty(addresses)) {
            throw new IllegalStateException("Invalid registry address found.");
=======
        public <S> Module reference(String id, Consumer<ReferenceBuilder<S>> consumerBuilder) {
            return reference(createReferenceConfig(id, consumerBuilder));
>>>>>>> origin/3.2
        }

<<<<<<< HEAD
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
     * Initialize {@link MetadataService} from {@link WritableMetadataService}'s extension
     */
    private void initMetadataService() {
        this.metadataService = getDefaultExtension();
        this.metadataServiceExporter = new ConfigurableMetadataServiceExporter(metadataService);
    }

    /**
     * Start the bootstrap
     */
    public DubboBootstrap start() {
        if (started.compareAndSet(false, true)) {
            destroyed.set(false);
            ready.set(false);
            initialize();
            if (logger.isInfoEnabled()) {
                logger.info(NAME + " is starting...");
            }
            // 1. export Dubbo Services
            exportServices();

            // Not only provider register
            if (!isOnlyRegisterProvider() || hasExportedServices()) {
                // 2. export MetadataService
                exportMetadataService();
                //3. Register the local ServiceInstance if required
                registerServiceInstance();
            }

            referServices();
            if (asyncExportingFutures.size() > 0) {
                new Thread(() -> {
                    try {
                        this.awaitFinish();
                    } catch (Exception e) {
                        logger.warn(NAME + " exportAsync occurred an exception.");
                    }
                    ready.set(true);
                    if (logger.isInfoEnabled()) {
                        logger.info(NAME + " is ready.");
                    }
                    ExtensionLoader<DubboBootstrapStartStopListener> exts = getExtensionLoader(DubboBootstrapStartStopListener.class);
                    exts.getSupportedExtensionInstances().forEach(ext -> ext.onStart(this));
                }).start();
            } else {
                ready.set(true);
                if (logger.isInfoEnabled()) {
                    logger.info(NAME + " is ready.");
                }
                ExtensionLoader<DubboBootstrapStartStopListener> exts = getExtensionLoader(DubboBootstrapStartStopListener.class);
                exts.getSupportedExtensionInstances().forEach(ext -> ext.onStart(this));
            }
            if (logger.isInfoEnabled()) {
                logger.info(NAME + " has started.");
            }
        }
        return this;
    }

    private boolean hasExportedServices() {
        return !metadataService.getExportedURLs().isEmpty();
    }

    /**
     * Block current thread to be await.
     *
     * @return {@link DubboBootstrap}
     */
    public DubboBootstrap await() {
        // if has been waited, no need to wait again, return immediately
        if (!awaited.get()) {
            if (!executorService.isShutdown()) {
                executeMutually(() -> {
                    while (!awaited.get()) {
                        if (logger.isInfoEnabled()) {
                            logger.info(NAME + " awaiting ...");
                        }
                        try {
                            condition.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
        }
        return this;
    }

    public DubboBootstrap awaitFinish() throws Exception {
        logger.info(NAME + " waiting services exporting / referring ...");
        if (exportAsync && asyncExportingFutures.size() > 0) {
            CompletableFuture future = CompletableFuture.allOf(asyncExportingFutures.toArray(new CompletableFuture[0]));
            future.get();
        }
        if (referAsync && asyncReferringFutures.size() > 0) {
            CompletableFuture future = CompletableFuture.allOf(asyncReferringFutures.toArray(new CompletableFuture[0]));
            future.get();
        }

        logger.info("Service export / refer finished.");
        return this;
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public boolean isStarted() {
        return started.get();
    }

    public boolean isReady() {
        return ready.get();
    }


    public DubboBootstrap stop() throws IllegalStateException {
        destroy();
        return this;
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

    private DynamicConfiguration prepareEnvironment(ConfigCenterConfig configCenter) {
        if (configCenter.isValid()) {
            if (!configCenter.checkOrUpdateInited()) {
                return null;
            }
            DynamicConfiguration dynamicConfiguration = getDynamicConfiguration(configCenter.toUrl());
            String configContent = dynamicConfiguration.getProperties(configCenter.getConfigFile(), configCenter.getGroup());

            String appGroup = getApplication().getName();
            String appConfigContent = null;
            if (isNotEmpty(appGroup)) {
                appConfigContent = dynamicConfiguration.getProperties
                        (isNotEmpty(configCenter.getAppConfigFile()) ? configCenter.getAppConfigFile() : configCenter.getConfigFile(),
                                appGroup
                        );
            }
            try {
                environment.setConfigCenterFirst(configCenter.isHighestPriority());
                Map<String, String> globalRemoteProperties = parseProperties(configContent);
                if (CollectionUtils.isEmptyMap(globalRemoteProperties)) {
                    logger.info("No global configuration in config center");
                }
                environment.updateExternalConfigurationMap(globalRemoteProperties);

                Map<String, String> appRemoteProperties = parseProperties(appConfigContent);
                if (CollectionUtils.isEmptyMap(appRemoteProperties)) {
                    logger.info("No application level configuration in config center");
                }
                environment.updateAppExternalConfigurationMap(appRemoteProperties);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse configurations from Config Center.", e);
=======
        public Module reference(ReferenceConfig<?> referenceConfig) {
            DubboBootstrap.this.reference(referenceConfig, moduleModel);
            return this;
        }

        public Module references(List<ReferenceConfig> referenceConfigs) {
            if (CollectionUtils.isEmpty(referenceConfigs)) {
                return this;
            }
            for (ReferenceConfig referenceConfig : referenceConfigs) {
                this.reference(referenceConfig);
>>>>>>> origin/3.2
            }
            return this;
        }
<<<<<<< HEAD
        return null;
    }

    /**
     * export {@link MetadataService}
     */
    private void exportMetadataService() {
        metadataServiceExporter.export();
    }

    private void unexportMetadataService() {
        if (metadataServiceExporter != null && metadataServiceExporter.isExported()) {
            metadataServiceExporter.unexport();
        }
    }

    private void exportServices() {
        configManager.getServices().forEach(sc -> {
            // TODO, compatible with ServiceConfig.export()
            ServiceConfig serviceConfig = (ServiceConfig) sc;
            serviceConfig.setBootstrap(this);

            if (exportAsync) {
                ExecutorService executor = executorRepository.getServiceExporterExecutor();
                Future<?> future = executor.submit(() -> {
                    try {
                        exportService(serviceConfig);
                    } catch (Throwable t) {
                        logger.error("export async catch error : " + t.getMessage(), t);
                    }
                });
                asyncExportingFutures.add(future);
            } else {
                exportService(serviceConfig);
            }
        });
    }

    private void exportService(ServiceConfig sc) {
        if (exportedServices.containsKey(sc.getServiceName())) {
            throw new IllegalStateException("There are multiple ServiceBean instances with the same service name: [" +
                    sc.getServiceName() + "], instances: [" +
                    exportedServices.get(sc.getServiceName()).toString() + ", " +
                    sc.toString() + "]. Only one service can be exported for the same triple (group, interface, version), " +
                    "please modify the group or version if you really need to export multiple services of the same interface.");
        }
        sc.export();
        exportedServices.put(sc.getServiceName(), sc);
    }

    private void unexportServices() {
        exportedServices.forEach((serviceName, sc) -> {
            configManager.removeConfig(sc);
            sc.unexport();
        });

        asyncExportingFutures.forEach(future -> {
            if (!future.isDone()) {
                future.cancel(true);
            }
        });
        asyncExportingFutures.clear();
        exportedServices.clear();
    }

    private void referServices() {
        if (cache == null) {
            cache = ReferenceConfigCache.getCache();
=======

        // {@link ProviderConfig} correlative methods
        public Module provider(Consumer<ProviderBuilder> builderConsumer) {
            return provider(null, builderConsumer);
>>>>>>> origin/3.2
        }

        public Module provider(String id, Consumer<ProviderBuilder> builderConsumer) {
            return provider(createProviderConfig(id, builderConsumer));
        }

        public Module provider(ProviderConfig providerConfig) {
            DubboBootstrap.this.provider(providerConfig, moduleModel);
            return this;
        }

<<<<<<< HEAD
        ApplicationConfig application = getApplication();

        String serviceName = application.getName();

        URL exportedURL = selectMetadataServiceExportedURL();

        String host = exportedURL.getHost();

        int port = exportedURL.getPort();

        ServiceInstance serviceInstance = createServiceInstance(serviceName, host, port);

        doRegisterServiceInstance(serviceInstance);

        // scheduled task for updating Metadata and ServiceInstance
        executorRepository.nextScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                InMemoryWritableMetadataService localMetadataService = (InMemoryWritableMetadataService) WritableMetadataService.getDefaultExtension();
                localMetadataService.blockUntilUpdated();
                ServiceInstanceMetadataUtils.refreshMetadataAndInstance();
            } catch (Throwable e) {
                logger.error("refresh metadata and instance failed", e);
            }
        }, 0, ConfigurationUtils.get(METADATA_PUBLISH_DELAY_KEY, DEFAULT_METADATA_PUBLISH_DELAY), TimeUnit.MILLISECONDS);
    }

    private void doRegisterServiceInstance(ServiceInstance serviceInstance) {
        //FIXME
        if (logger.isInfoEnabled()) {
            logger.info("Start publishing metadata to remote center, this only makes sense for applications enabled remote metadata center.");
        }
        publishMetadataToRemote(serviceInstance);

        logger.info("Start registering instance address to registry.");
        getServiceDiscoveries().forEach(serviceDiscovery ->
        {
            calInstanceRevision(serviceDiscovery, serviceInstance);
            if (logger.isDebugEnabled()) {
                logger.debug("Start registering instance address to registry" + serviceDiscovery.getUrl() + ", instance " + serviceInstance);
            }
            // register metadata
            serviceDiscovery.register(serviceInstance);
        });
    }

    private void publishMetadataToRemote(ServiceInstance serviceInstance) {
//        InMemoryWritableMetadataService localMetadataService = (InMemoryWritableMetadataService)WritableMetadataService.getDefaultExtension();
//        localMetadataService.blockUntilUpdated();
        RemoteMetadataServiceImpl remoteMetadataService = MetadataUtils.getRemoteMetadataService();
        remoteMetadataService.publishMetadata(serviceInstance.getServiceName());
    }

    private URL selectMetadataServiceExportedURL() {

        URL selectedURL = null;

        SortedSet<String> urlValues = metadataService.getExportedURLs();

        for (String urlValue : urlValues) {
            URL url = URL.valueOf(urlValue);
            if ("rest".equals(url.getProtocol())) { // REST first
                selectedURL = url;
                break;
            } else {
                selectedURL = url; // If not found, take any one
=======
        public Module providers(List<ProviderConfig> providerConfigs) {
            if (CollectionUtils.isEmpty(providerConfigs)) {
                return this;
            }
            for (ProviderConfig providerConfig : providerConfigs) {
                DubboBootstrap.this.provider(providerConfig, moduleModel);
>>>>>>> origin/3.2
            }
            return this;
        }

<<<<<<< HEAD
        if (selectedURL == null && CollectionUtils.isNotEmpty(urlValues)) {
            selectedURL = URL.valueOf(urlValues.iterator().next());
        }

        return selectedURL;
    }

    private void unregisterServiceInstance() {
        if (serviceInstance != null) {
            getServiceDiscoveries().forEach(serviceDiscovery -> serviceDiscovery.unregister(serviceInstance));
        }
    }

    private ServiceInstance createServiceInstance(String serviceName, String host, int port) {
        this.serviceInstance = new DefaultServiceInstance(serviceName, host, port);
        setMetadataStorageType(serviceInstance, getMetadataType());

        ExtensionLoader<ServiceInstanceCustomizer> loader =
                ExtensionLoader.getExtensionLoader(ServiceInstanceCustomizer.class);
        // FIXME, sort customizer before apply
        loader.getSupportedExtensionInstances().forEach(customizer -> {
            // customizes
            customizer.customize(this.serviceInstance);
        });

        return this.serviceInstance;
    }

    public void destroy() {
        if (destroyLock.tryLock()) {
            try {
                if (destroyed.compareAndSet(false, true)) {
                    if (started.compareAndSet(true, false)) {
                        unregisterServiceInstance();
                        unexportMetadataService();
                        unexportServices();
                        unreferServices();
                    }
                    destroyServiceDiscoveries();
                    destroyExecutorRepository();
                    DubboShutdownHook.destroyAll();
                    clearConfigManager();
                    shutdownExecutor();
                    release();
                    ExtensionLoader<DubboBootstrapStartStopListener> exts = getExtensionLoader(DubboBootstrapStartStopListener.class);
                    exts.getSupportedExtensionInstances().forEach(ext -> ext.onStop(this));
                }
            } finally {
                initialized.set(false);
                destroyLock.unlock();
            }
=======
        // {@link ConsumerConfig} correlative methods
        public Module consumer(Consumer<ConsumerBuilder> builderConsumer) {
            return consumer(null, builderConsumer);
>>>>>>> origin/3.2
        }

<<<<<<< HEAD
    private void destroyExecutorRepository() {
        ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension().destroyAll();
    }

    private void destroyRegistries() {
        AbstractRegistryFactory.destroyAll();
    }

    private void destroyServiceDiscoveries() {
        getServiceDiscoveries().forEach(serviceDiscovery -> execute(serviceDiscovery::destroy));
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ServiceDiscoveries have been destroyed.");
        }
    }

    private void clearConfigManager() {
        clearConfigs();
    }
=======
        public Module consumer(String id, Consumer<ConsumerBuilder> builderConsumer) {
            return consumer(createConsumerConfig(id, builderConsumer));
        }
>>>>>>> origin/3.2

        public Module consumer(ConsumerConfig consumerConfig) {
            DubboBootstrap.this.consumer(consumerConfig, moduleModel);
            return this;
        }

        public Module consumers(List<ConsumerConfig> consumerConfigs) {
            if (CollectionUtils.isEmpty(consumerConfigs)) {
                return this;
            }
<<<<<<< HEAD
        });
    }

    private void shutdownExecutor() {
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

    public ApplicationConfig getApplication() {
        ApplicationConfig application = configManager
                .getApplication()
                .orElseGet(() -> {
                    ApplicationConfig applicationConfig = new ApplicationConfig();
                    configManager.setApplication(applicationConfig);
                    return applicationConfig;
                });

        application.refresh();
        return application;
    }

    private MonitorConfig getMonitor() {
        MonitorConfig monitor = configManager
                .getMonitor()
                .orElseGet(() -> {
                    MonitorConfig monitorConfig = new MonitorConfig();
                    configManager.setMonitor(monitorConfig);
                    return monitorConfig;
                });

        monitor.refresh();
        return monitor;
    }

    private MetricsConfig getMetrics() {
        MetricsConfig metrics = configManager
                .getMetrics()
                .orElseGet(() -> {
                    MetricsConfig metricsConfig = new MetricsConfig();
                    configManager.setMetrics(metricsConfig);
                    return metricsConfig;
                });
        metrics.refresh();
        return metrics;
    }

    private ModuleConfig getModule() {
        ModuleConfig module = configManager
                .getModule()
                .orElseGet(() -> {
                    ModuleConfig moduleConfig = new ModuleConfig();
                    configManager.setModule(moduleConfig);
                    return moduleConfig;
                });

        module.refresh();
        return module;
    }

    private SslConfig getSsl() {
        SslConfig ssl = configManager
                .getSsl()
                .orElseGet(() -> {
                    SslConfig sslConfig = new SslConfig();
                    configManager.setSsl(sslConfig);
                    return sslConfig;
                });

        ssl.refresh();
        return ssl;
    }

    public void setReady(boolean ready) {
        this.ready.set(ready);
    }


    /**
     * Try reset dubbo status for new instance.
     * @deprecated  For testing purposes only
     */
    @Deprecated
    public static void reset() {
        reset(true);
    }

    /**
     * Try reset dubbo status for new instance.
     * @deprecated For testing purposes only
     */
    @Deprecated
    public static void reset(boolean destroy) {
        DubboShutdownHook.reset();
        if (destroy) {
            if (instance != null) {
                instance.destroy();
                instance = null;
            }
            ApplicationModel.reset();
            AbstractRegistryFactory.reset();
            MetadataReportInstance.destroy();
            AbstractMetadataReportFactory.clear();
            ExtensionLoader.resetExtensionLoader(DynamicConfigurationFactory.class);
            ExtensionLoader.resetExtensionLoader(MetadataReportFactory.class);
            ExtensionLoader.destroyAll();
        } else {
            instance = null;
            ApplicationModel.reset();
        }
    }
=======
            for (ConsumerConfig consumerConfig : consumerConfigs) {
                DubboBootstrap.this.consumer(consumerConfig, moduleModel);
            }
            return this;
        }
    }

>>>>>>> origin/3.2
}
