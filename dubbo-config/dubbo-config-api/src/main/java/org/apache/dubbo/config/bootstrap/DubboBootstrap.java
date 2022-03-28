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

import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.DeployListenerAdapter;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
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
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;

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
public final class DubboBootstrap {

    private static final String NAME = DubboBootstrap.class.getSimpleName();

    private static final Logger logger = LoggerFactory.getLogger(DubboBootstrap.class);

    private static volatile Map<ApplicationModel, DubboBootstrap> instanceMap = new ConcurrentHashMap<>();
    private static volatile DubboBootstrap instance;

    private final AtomicBoolean awaited = new AtomicBoolean(false);

    private volatile BootstrapTakeoverMode takeoverMode = BootstrapTakeoverMode.AUTO;

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private ExecutorRepository executorRepository;

    private final ApplicationModel applicationModel;

    protected final ConfigManager configManager;

    protected final Environment environment;

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
        return instanceMap.computeIfAbsent(applicationModel, _k -> new DubboBootstrap(applicationModel));
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

        ApplicationModel.reset();
    }

    private DubboBootstrap(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        configManager = applicationModel.getApplicationConfigManager();
        environment = applicationModel.getModelEnvironment();

        executorRepository = applicationModel.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
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
     * @return the future object
     */
    public Future asyncStart() {
        return applicationDeployer.start();
    }

    /**
     * Stop dubbo application
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

    public DubboBootstrap metadataReport(String id,Consumer<MetadataReportBuilder> consumerBuilder) {
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
        return this.provider(providerConfig, applicationModel.getDefaultModule());
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
        return this.consumer(consumerConfig, applicationModel.getDefaultModule());
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
        configCenterConfig.setScopeModel(applicationModel);
        configManager.addConfigCenter(configCenterConfig);
        return this;
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


    public class Module {
        private ModuleModel moduleModel;
        private DubboBootstrap bootstrap;

        public Module(ModuleModel moduleModel) {
            this.moduleModel = moduleModel;
            this.bootstrap = DubboBootstrap.this;
        }

        public DubboBootstrap endModule() {
            return this.bootstrap.endModule();
        }

        public ModuleModel getModuleModel() {
            return moduleModel;
        }

        public Module config(ModuleConfig moduleConfig) {
            this.moduleModel.getConfigManager().setModule(moduleConfig);
            return this;
        }

        // {@link ServiceConfig} correlative methods
        public <S> Module service(Consumer<ServiceBuilder<S>> consumerBuilder) {
            return service(null, consumerBuilder);
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

        public <S> Module reference(String id, Consumer<ReferenceBuilder<S>> consumerBuilder) {
            return reference(createReferenceConfig(id, consumerBuilder));
        }

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
            }
            return this;
        }

        // {@link ProviderConfig} correlative methods
        public Module provider(Consumer<ProviderBuilder> builderConsumer) {
            return provider(null, builderConsumer);
        }

        public Module provider(String id, Consumer<ProviderBuilder> builderConsumer) {
            return provider(createProviderConfig(id, builderConsumer));
        }

        public Module provider(ProviderConfig providerConfig) {
            DubboBootstrap.this.provider(providerConfig, moduleModel);
            return this;
        }

        public Module providers(List<ProviderConfig> providerConfigs) {
            if (CollectionUtils.isEmpty(providerConfigs)) {
                return this;
            }
            for (ProviderConfig providerConfig : providerConfigs) {
                DubboBootstrap.this.provider(providerConfig, moduleModel);
            }
            return this;
        }

        // {@link ConsumerConfig} correlative methods
        public Module consumer(Consumer<ConsumerBuilder> builderConsumer) {
            return consumer(null, builderConsumer);
        }

        public Module consumer(String id, Consumer<ConsumerBuilder> builderConsumer) {
            return consumer(createConsumerConfig(id, builderConsumer));
        }

        public Module consumer(ConsumerConfig consumerConfig) {
            DubboBootstrap.this.consumer(consumerConfig, moduleModel);
            return this;
        }

        public Module consumers(List<ConsumerConfig> consumerConfigs) {
            if (CollectionUtils.isEmpty(consumerConfigs)) {
                return this;
            }
            for (ConsumerConfig consumerConfig : consumerConfigs) {
                DubboBootstrap.this.consumer(consumerConfig, moduleModel);
            }
            return this;
        }
    }

}
