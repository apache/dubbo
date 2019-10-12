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

import org.apache.dubbo.bootstrap.builders.ApplicationBuilder;
import org.apache.dubbo.bootstrap.builders.ConsumerBuilder;
import org.apache.dubbo.bootstrap.builders.ProtocolBuilder;
import org.apache.dubbo.bootstrap.builders.ProviderBuilder;
import org.apache.dubbo.bootstrap.builders.ReferenceBuilder;
import org.apache.dubbo.bootstrap.builders.RegistryBuilder;
import org.apache.dubbo.bootstrap.builders.ServiceBuilder;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.wrapper.CompositeDynamicConfiguration;
import org.apache.dubbo.common.context.Lifecycle;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.lang.ShutdownHookCallback;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.event.ServiceConfigExportedEvent;
import org.apache.dubbo.config.invoker.DelegateProviderMetaDataInvoker;
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.event.Event;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.event.GenericEventListener;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryInitializingEvent;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.ConfiguratorFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.config.ConfigurationUtils.parseProperties;
import static org.apache.dubbo.common.config.configcenter.DynamicConfiguration.getDynamicConfiguration;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_IP_TO_BIND;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;
import static org.apache.dubbo.common.function.ThrowableAction.execute;
import static org.apache.dubbo.common.utils.NetUtils.getAvailablePort;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidPort;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.config.Constants.DUBBO_IP_TO_REGISTRY;
import static org.apache.dubbo.config.Constants.DUBBO_PORT_TO_BIND;
import static org.apache.dubbo.config.Constants.DUBBO_PORT_TO_REGISTRY;
import static org.apache.dubbo.config.Constants.MULTICAST;
import static org.apache.dubbo.config.Constants.SCOPE_NONE;
import static org.apache.dubbo.config.context.ConfigManager.getInstance;
import static org.apache.dubbo.metadata.WritableMetadataService.getExtension;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setMetadataStorageType;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.Constants.PROXY_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;

/**
 * The bootstrap class of Dubbo
 *
 * @since 2.7.4
 */
public class DubboBootstrap extends GenericEventListener implements Lifecycle {

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

    private volatile ServiceInstance serviceInstance;

    private volatile MetadataService metadataService;

    private volatile MetadataServiceExporter metadataServiceExporter;

    private volatile List<ServiceDiscovery> serviceDiscoveries = new LinkedList<>();

    private ConcurrentMap<ServiceConfig<?>, List<Exporter<?>>> exporters = new ConcurrentHashMap<>();

    public DubboBootstrap() {
        ShutdownHookCallbacks.INSTANCE.addCallback(new ShutdownHookCallback() {
            @Override
            public void callback() throws Throwable {
                DubboBootstrap.this.destroy();
            }
        });
    }

    public void registerShutdownHook() {
        DubboShutdownHook.getDubboShutdownHook().register();
    }

    /**
     * Store the {@link ServiceDiscovery} instances into {@link ServiceDiscoveryInitializingEvent}
     *
     * @param event {@link ServiceDiscoveryInitializingEvent}
     * @see {@linkplan org.apache.dubbo.registry.client.EventPublishingServiceDiscovery}
     */
    public void onServiceDiscoveryInitializing(ServiceDiscoveryInitializingEvent event) {
        executeMutually(() -> {
            serviceDiscoveries.add(event.getSource());
            sort(serviceDiscoveries);
        });
    }

    private boolean isOnlyRegisterProvider() {
        Boolean registerConsumer = configManager.getApplicationOrElseThrow().getRegisterConsumer();
        return registerConsumer == null || !registerConsumer;
    }

    private String getMetadataType() {
        String type = configManager.getApplicationOrElseThrow().getMetadataType();
        if (StringUtils.isEmpty(type)) {
            type = DEFAULT_METADATA_STORAGE_TYPE;
        }
        return type;
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
    public DubboBootstrap initialize() {
        if (!isInitialized()) {

            startConfigCenter();

            startMetadataReport();

            loadServiceRepository();

            loadRemoteConfigs();

            useRegistryAsConfigCenterIfNecessary();

            initMetadataService();

            initMetadataServiceExporter();

            initEventListener();

            initialized = true;

            if (logger.isInfoEnabled()) {
                logger.info(NAME + " has been initialized!");
            }

        }

        return this;
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

    private void startMetadataReport() {
        ApplicationConfig applicationConfig = configManager.getApplicationOrElseThrow();

        String metadataType = applicationConfig.getMetadataType();
        // FIXME, multiple metadata config support.
        Collection<MetadataReportConfig> metadataReportConfigs = configManager.getMetadataConfigs();
        if (CollectionUtils.isEmpty(metadataReportConfigs)) {
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                throw new IllegalStateException("No MetadataConfig found, you must specify the remote Metadata Center address when 'metadata=remote' is enabled.");
            }
            return;
        }
        MetadataReportConfig metadataReportConfig = metadataReportConfigs.iterator().next();
        if (!metadataReportConfig.isValid()) {
            return;
        }

        MetadataReportInstance.init(metadataReportConfig.toUrl());
    }

    public void loadServiceRepository() {
        ServiceRepository.init(getApplication().getRepository());
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
     * Initialize {@link MetadataService} from {@link WritableMetadataService}'s extension
     */
    private void initMetadataService() {
        this.metadataService = getExtension(getMetadataType());
    }

    /**
     * Initialize {@link MetadataServiceExporter}
     */
    private void initMetadataServiceExporter() {
        this.metadataServiceExporter = new ConfigurableMetadataServiceExporter(metadataService);
    }

    /**
     * Initialize {@link EventListener}
     */
    private void initEventListener() {
        // Add current instance into listeners
        addEventListener(this);
    }

    private Collection<ServiceDiscovery> getServiceDiscoveries() {
        return serviceDiscoveries;
    }

    /**
     * Start the bootstrap
     */
    @Override
    public DubboBootstrap start() {

        if (!shouldStart()) {
            return this;
        }

        if (!isInitialized()) {
            initialize();
        }
        if (!isStarted()) {
            if (logger.isInfoEnabled()) {
                logger.info(NAME + " is starting...");
            }
            // 1. export Dubbo Services
            exportServices();

            // 2. export MetadataService
            exportMetadataService();

            // Not only provider register
            if (!isOnlyRegisterProvider() || hasExportedServices()) {
                //3. Register the local ServiceInstance if required
                registerServiceInstance();
            }

            referServices();

            started = true;

            if (logger.isInfoEnabled()) {
                logger.info(NAME + " has started.");
            }
        }
        return this;
    }

    /**
     * Should Start current bootstrap
     *
     * @return If there is not any service discovery registry in the {@link ConfigManager#getRegistries()}, it will not
     * start current bootstrap
     */
    private boolean shouldStart() {
        return configManager.getRegistries()
                .stream()
                .map(RegistryConfig::getAddress)
                .map(URL::valueOf)
                .filter(UrlUtils::isServiceDiscoveryRegistryType)
                .count() > 0;
    }


    private boolean hasExportedServices() {
        return !metadataService.getExportedURLs().isEmpty();
    }

    private ApplicationConfig getApplication() {
        return configManager.getApplication().orElseThrow(() -> new IllegalStateException("ApplicationConfig cannot be null"));
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
    @Override
    public DubboBootstrap stop() {
        if (isInitialized() && isStarted()) {
            unregisterServiceInstance();
            unexportMetadataService();
            unexportServices();
            started = false;
        }
        return this;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
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

    /**
     * export {@link MetadataService}
     */
    private void exportMetadataService() {
        metadataServiceExporter.export();
    }

    private void unexportMetadataService() {
        metadataServiceExporter.unexport();
    }

    private void exportServices() {
        configManager.getServices().forEach(sc -> {
            exporters.computeIfAbsent(sc, _k -> {
                List<Exporter<?>> exportersOfService = new ArrayList<>();
                Helper.export(sc, exportersOfService);
                return exportersOfService;
            });
        });
    }

    private void unexportServices() {
        exporters.forEach((sc, subExporters) -> {
            configManager.removeConfig(sc);
            subExporters.forEach(Exporter::unexport);
        });
        exporters.clear();
    }

    private void referServices() {
        if (cache == null) {
            cache = ReferenceConfigCache.getCache();
        }
        configManager.getReferences().forEach((rc) -> {
            // check eager init or not.
            if (rc.shouldInit()) {
                cache.get(rc);
            }
        });
    }

    private void registerServiceInstance() {

        ApplicationConfig application = getApplication();

        String serviceName = application.getName();

        URL exportedURL = selectMetadataServiceExportedURL();

        String host = exportedURL.getHost();

        int port = exportedURL.getPort();

        ServiceInstance serviceInstance = createServiceInstance(serviceName, host, port);

        getServiceDiscoveries().forEach(serviceDiscovery -> serviceDiscovery.register(serviceInstance));
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
            }
        }

        return selectedURL;
    }

    private void unregisterServiceInstance() {
        if (serviceInstance != null) {
            getServiceDiscoveries().forEach(serviceDiscovery -> {
                serviceDiscovery.unregister(serviceInstance);
            });
        }
    }

    private ServiceInstance createServiceInstance(String serviceName, String host, int port) {
        this.serviceInstance = new DefaultServiceInstance(serviceName, host, port);
        setMetadataStorageType(serviceInstance, getMetadataType());
        return this.serviceInstance;
    }

    public void destroy() {

        stop();

        destroyRegistries();

        destroyProtocols();

        destroyReferences();

        destroyServiceDiscoveries();

        clear();

        release();

        shutdown();
    }

    private void destroyProtocols() {
        configManager.getProtocols().forEach(protocolConfig -> {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolConfig.getName()).destroy();
        });
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ProtocolConfigs have been destroyed.");
        }
    }

    private void destroyRegistries() {
        AbstractRegistryFactory.destroyAll();
    }

    private void destroyReferences() {
        cache.destroyAll();
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ReferenceConfigs have been destroyed.");
        }
    }

    private void destroyServiceDiscoveries() {
        getServiceDiscoveries().forEach(serviceDiscovery -> {
            execute(() -> {
                serviceDiscovery.destroy();
            });
        });
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ServiceDiscoveries have been destroyed.");
        }
    }

    private void clear() {
        clearConfigs();
        clearServiceDiscoveries();
    }

    private void clearConfigs() {
        configManager.clear();
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s configs have been clear.");
        }
    }

    private void clearServiceDiscoveries() {
        serviceDiscoveries.clear();
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s serviceDiscoveries have been clear.");
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

    public static class Helper {

        public static final Logger logger = LoggerFactory.getLogger(Helper.class);

        /**
         * A random port cache, the different protocols who has no port specified have different random port
         */
        private static final Map<String, Integer> RANDOM_PORT_MAP = new HashMap<String, Integer>();

        /**
         * A delayed exposure service timer
         */
        private static final ScheduledExecutorService DELAY_EXPORT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboServiceDelayExporter", true));

        private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

        /**
         * A {@link ProxyFactory} implementation that will generate a exported service proxy,the JavassistProxyFactory is its
         * default implementation
         */
        private static final ProxyFactory PROXY_FACTORY = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

        public static void checkAndUpdateSubConfigs(ServiceConfig<?> sc) {
            // Use default configs defined explicitly on global scope
            sc.completeCompoundConfigs();
            sc.checkDefault();
            sc.checkProtocol();
            sc.checkApplication();
            // if protocol is not injvm checkRegistry
            if (!isOnlyInJvm(sc)) {
                sc.checkRegistry();
            }
            sc.refresh();
            sc.checkMetadataReport();

            String interfaceName = sc.getInterface();
            Class<?> interfaceClass = sc.getInterfaceClass();
            if (StringUtils.isEmpty(interfaceName)) {
                throw new IllegalStateException("<dubbo:service interface=\"\" /> interface not allow null!");
            }

            if (sc.getRef() instanceof GenericService) {
                interfaceClass = GenericService.class;
                if (StringUtils.isEmpty(sc.getGeneric())) {
                    sc.setGeneric(Boolean.TRUE.toString());
                }
            } else {
                try {
                    interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                            .getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                sc.checkInterfaceAndMethods(interfaceClass, sc.getMethods());
                sc.checkRef();
                sc.setGeneric(Boolean.FALSE.toString());
            }
            if (sc.getLocal() != null) {
                if ("true".equals(sc.getLocal())) {
                    sc.setLocal(interfaceName + "Local");
                }
                Class<?> localClass;
                try {
                    localClass = ClassUtils.forNameWithThreadContextClassLoader(sc.getLocal());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                if (!interfaceClass.isAssignableFrom(localClass)) {
                    throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceName);
                }
            }
            if (sc.getStub() != null) {
                if ("true".equals(sc.getStub())) {
                    sc.setStub(interfaceName + "Stub");
                }
                Class<?> stubClass;
                try {
                    stubClass = ClassUtils.forNameWithThreadContextClassLoader(sc.getStub());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                if (!interfaceClass.isAssignableFrom(stubClass)) {
                    throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interface " + interfaceName);
                }
            }
            sc.setInterface(interfaceClass);
            sc.checkStubAndLocal(interfaceClass);
            BootstrapUtils.checkMock(interfaceClass, sc);
            sc.appendParameters();
        }

        public static List<Exporter<?>> export(ServiceConfig<?> sc) {
            List<Exporter<?>> exporters = new ArrayList<>();
            export(sc, exporters);
            return exporters;
        }

        public static void export(ServiceConfig<?> sc, List<Exporter<?>> exporters) {
            checkAndUpdateSubConfigs(sc);

            ServiceMetadata serviceMetadata = sc.getServiceMetadata();
            //init serviceMetadata
            serviceMetadata.setVersion(sc.getVersion());
            serviceMetadata.setGroup(sc.getGroup());
            serviceMetadata.setDefaultGroup(sc.getGroup());
            serviceMetadata.setServiceType(sc.getInterfaceClass());
            serviceMetadata.setServiceInterfaceName(sc.getInterface());
            serviceMetadata.setTarget(sc.getRef());

            if (!sc.shouldExport()) {
                return;
            }

            if (sc.shouldDelay()) {
                DELAY_EXPORT_EXECUTOR.schedule(() -> {
                    doExport(sc, exporters);
                }, sc.getDelay(), TimeUnit.MILLISECONDS);
            } else {
                doExport(sc, exporters);
            }
        }

        protected static synchronized void doExport(ServiceConfig<?> sc, List<Exporter<?>> exporters) {
            if (StringUtils.isEmpty(sc.getPath())) {
                sc.setPath(sc.getInterface());
            }
            doExportUrls(sc, exporters);

            // dispatch a ServiceConfigExportedEvent since 2.7.4
            dispatch(new ServiceConfigExportedEvent(sc));

        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void doExportUrls(ServiceConfig<?> sc, List<Exporter<?>> exporters) {
            List<URL> registryURLs = BootstrapUtils.loadRegistries(sc, true);
            for (ProtocolConfig protocolConfig : sc.getProtocols()) {
                String pathKey = URL.buildKey(sc.getContextPath(protocolConfig).map(p -> p + "/" + sc.getPath()).orElse(sc.getPath()), sc.getGroup(), sc.getVersion());
                sc.getServiceMetadata().setServiceKey(pathKey);
                ProviderModel providerModel = new ProviderModel(
                        pathKey,
                        sc.getRef(),
                        ApplicationModel.registerServiceModel(sc.getInterfaceClass()),
                        sc,
                        sc.getServiceMetadata()
                );
                ApplicationModel.initProviderModel(pathKey, providerModel);
                doExportUrlsFor1Protocol(sc, protocolConfig, registryURLs, exporters);
            }
        }

        private static void doExportUrlsFor1Protocol(ServiceConfig<?> sc,
                                                     ProtocolConfig protocolConfig,
                                                     List<URL> registryURLs,
                                                     List<Exporter<?>> exporters) {
            String name = protocolConfig.getName();
            if (StringUtils.isEmpty(name)) {
                name = DUBBO;
            }

            Class<?> interfaceClass = sc.getInterfaceClass();

            Map<String, String> map = new HashMap<String, String>();
            map.put(SIDE_KEY, PROVIDER_SIDE);

            ServiceConfig.appendRuntimeParameters(map);
            AbstractConfig.appendParameters(map, sc.getMetrics());
            AbstractConfig.appendParameters(map, sc.getApplication());
            AbstractConfig.appendParameters(map, sc.getModule());
            // remove 'default.' prefix for configs from ProviderConfig
            // appendParameters(map, provider, Constants.DEFAULT_KEY);
            AbstractConfig.appendParameters(map, sc.getProvider());
            AbstractConfig.appendParameters(map, protocolConfig);
            AbstractConfig.appendParameters(map, sc);
            if (CollectionUtils.isNotEmpty(sc.getMethods())) {
                for (MethodConfig method : sc.getMethods()) {
                    AbstractConfig.appendParameters(map, method, method.getName());
                    String retryKey = method.getName() + ".retry";
                    if (map.containsKey(retryKey)) {
                        String retryValue = map.remove(retryKey);
                        if ("false".equals(retryValue)) {
                            map.put(method.getName() + ".retries", "0");
                        }
                    }
                    List<ArgumentConfig> arguments = method.getArguments();
                    if (CollectionUtils.isNotEmpty(arguments)) {
                        for (ArgumentConfig argument : arguments) {
                            // convert argument type
                            if (argument.getType() != null && argument.getType().length() > 0) {
                                Method[] methods = interfaceClass.getMethods();
                                // visit all methods
                                if (methods != null && methods.length > 0) {
                                    for (int i = 0; i < methods.length; i++) {
                                        String methodName = methods[i].getName();
                                        // target the method, and get its signature
                                        if (methodName.equals(method.getName())) {
                                            Class<?>[] argtypes = methods[i].getParameterTypes();
                                            // one callback in the method
                                            if (argument.getIndex() != -1) {
                                                if (argtypes[argument.getIndex()].getName().equals(argument.getType())) {
                                                    AbstractConfig.appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                                                } else {
                                                    throw new IllegalArgumentException("Argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                                }
                                            } else {
                                                // multiple callbacks in the method
                                                for (int j = 0; j < argtypes.length; j++) {
                                                    Class<?> argclazz = argtypes[j];
                                                    if (argclazz.getName().equals(argument.getType())) {
                                                        AbstractConfig.appendParameters(map, argument, method.getName() + "." + j);
                                                        if (argument.getIndex() != -1 && argument.getIndex() != j) {
                                                            throw new IllegalArgumentException("Argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (argument.getIndex() != -1) {
                                AbstractConfig.appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                            } else {
                                throw new IllegalArgumentException("Argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                            }

                        }
                    }
                } // end of methods for
            }

            if (ProtocolUtils.isGeneric(sc.getGeneric())) {
                map.put(GENERIC_KEY, sc.getGeneric());
                map.put(METHODS_KEY, ANY_VALUE);
            } else {
                String revision = Version.getVersion(interfaceClass, sc.getVersion());
                if (revision != null && revision.length() > 0) {
                    map.put(REVISION_KEY, revision);
                }

                String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
                if (methods.length == 0) {
                    logger.warn("No method found in service interface " + interfaceClass.getName());
                    map.put(METHODS_KEY, ANY_VALUE);
                } else {
                    map.put(METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
                }
            }
            if (!ConfigUtils.isEmpty(sc.getToken())) {
                if (ConfigUtils.isDefault(sc.getToken())) {
                    map.put(TOKEN_KEY, UUID.randomUUID().toString());
                } else {
                    map.put(TOKEN_KEY, sc.getToken());
                }
            }
            //init serviceMetadata attachments
            sc.getServiceMetadata().getAttachments().putAll(map);

            // export service
            String host = findConfigedHosts(sc, protocolConfig, registryURLs, map);
            Integer port = findConfigedPorts(sc, protocolConfig, name, map);
            URL url = new URL(name, host, port, sc.getContextPath(protocolConfig).map(p -> p + "/" + sc.getPath()).orElse(sc.getPath()), map);

            // You can customize Configurator to append extra parameters
            if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                    .hasExtension(url.getProtocol())) {
                url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                        .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
            }

            String scope = url.getParameter(SCOPE_KEY);
            // don't export when none is configured
            if (!SCOPE_NONE.equalsIgnoreCase(scope)) {

                // export to local if the config is not remote (export to remote only when config is remote)
                if (!SCOPE_REMOTE.equalsIgnoreCase(scope)) {
                    exportLocal(sc, exporters, url);
                }
                // export to remote if the config is not local (export to local only when config is local)
                if (!SCOPE_LOCAL.equalsIgnoreCase(scope)) {
                    if (!isOnlyInJvm(sc) && logger.isInfoEnabled()) {
                        logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                    }
                    if (CollectionUtils.isNotEmpty(registryURLs)) {
                        for (URL registryURL : registryURLs) {
                            //if protocol is only injvm ,not register
                            if (LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
                                continue;
                            }
                            url = url.addParameterIfAbsent(DYNAMIC_KEY, registryURL.getParameter(DYNAMIC_KEY));
                            URL monitorUrl = BootstrapUtils.loadMonitor(sc, registryURL);
                            if (monitorUrl != null) {
                                url = url.addParameterAndEncoded(MONITOR_KEY, monitorUrl.toFullString());
                            }
                            if (logger.isInfoEnabled()) {
                                logger.info("Register dubbo service " + interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                            }

                            // For providers, this is used to enable custom proxy to generate invoker
                            String proxy = url.getParameter(PROXY_KEY);
                            if (StringUtils.isNotEmpty(proxy)) {
                                registryURL = registryURL.addParameter(PROXY_KEY, proxy);
                            }

                            Invoker<?> invoker = PROXY_FACTORY.getInvoker(sc.getRef(), (Class) interfaceClass, registryURL.addParameterAndEncoded(EXPORT_KEY, url.toFullString()));
                            DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, sc);

                            Exporter<?> exporter = protocol.export(wrapperInvoker);
                            exporters.add(exporter);
                        }
                    } else {
                        Invoker<?> invoker = PROXY_FACTORY.getInvoker(sc.getRef(), (Class) interfaceClass, url);
                        DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, sc);

                        Exporter<?> exporter = protocol.export(wrapperInvoker);
                        exporters.add(exporter);
                    }
                    /**
                     * @since 2.7.0
                     * ServiceData Store
                     */
                    WritableMetadataService metadataService = WritableMetadataService.getExtension(url.getParameter(METADATA_KEY, DEFAULT_METADATA_STORAGE_TYPE));
                    if (metadataService != null) {
                        metadataService.publishServiceDefinition(url);
                    }
                }
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        /**
         * always export injvm
         */
        private static void exportLocal(ServiceConfig<?> sc, List<Exporter<?>> exporters, URL url) {
            URL local = URLBuilder.from(url)
                    .setProtocol(LOCAL_PROTOCOL)
                    .setHost(LOCALHOST_VALUE)
                    .setPort(0)
                    .build();
            Exporter<?> exporter = protocol.export(
                    PROXY_FACTORY.getInvoker(sc.getRef(), (Class) sc.getInterfaceClass(), local));
            exporters.add(exporter);
            logger.info("Export dubbo service " + sc.getInterfaceClass().getName() + " to local registry url : " + local);
        }

        /**
         * Determine if it is injvm
         *
         * @return
         */
        private static boolean isOnlyInJvm(ServiceConfig<?> sc) {
            return sc.getProtocols().size() == 1
                    && LOCAL_PROTOCOL.equalsIgnoreCase(sc.getProtocols().get(0).getName());
        }

        /**
         * Register & bind IP address for service provider, can be configured separately.
         * Configuration priority: environment variables -> java system properties -> host property in config file ->
         * /etc/hosts -> default network address -> first available network address
         *
         * @param protocolConfig
         * @param registryURLs
         * @param map
         * @return
         */
        private static String findConfigedHosts(ServiceConfig<?> sc,
                                                ProtocolConfig protocolConfig,
                                                List<URL> registryURLs,
                                                Map<String, String> map) {
            boolean anyhost = false;

            String hostToBind = getValueFromConfig(protocolConfig, DUBBO_IP_TO_BIND);
            if (hostToBind != null && hostToBind.length() > 0 && isInvalidLocalHost(hostToBind)) {
                throw new IllegalArgumentException("Specified invalid bind ip from property:" + DUBBO_IP_TO_BIND + ", value:" + hostToBind);
            }

            // if bind ip is not found in environment, keep looking up
            if (StringUtils.isEmpty(hostToBind)) {
                hostToBind = protocolConfig.getHost();
                if (sc.getProvider() != null && StringUtils.isEmpty(hostToBind)) {
                    hostToBind = sc.getProvider().getHost();
                }
                if (isInvalidLocalHost(hostToBind)) {
                    anyhost = true;
                    try {
                        logger.info("No valid ip found from environment, try to find valid host from DNS.");
                        hostToBind = InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException e) {
                        logger.warn(e.getMessage(), e);
                    }
                    if (isInvalidLocalHost(hostToBind)) {
                        if (CollectionUtils.isNotEmpty(registryURLs)) {
                            for (URL registryURL : registryURLs) {
                                if (MULTICAST.equalsIgnoreCase(registryURL.getParameter("registry"))) {
                                    // skip multicast registry since we cannot connect to it via Socket
                                    continue;
                                }
                                try (Socket socket = new Socket()) {
                                    SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort());
                                    socket.connect(addr, 1000);
                                    hostToBind = socket.getLocalAddress().getHostAddress();
                                    break;
                                } catch (Exception e) {
                                    logger.warn(e.getMessage(), e);
                                }
                            }
                        }
                        if (isInvalidLocalHost(hostToBind)) {
                            hostToBind = getLocalHost();
                        }
                    }
                }
            }

            map.put(BIND_IP_KEY, hostToBind);

            // registry ip is not used for bind ip by default
            String hostToRegistry = getValueFromConfig(protocolConfig, DUBBO_IP_TO_REGISTRY);
            if (hostToRegistry != null && hostToRegistry.length() > 0 && isInvalidLocalHost(hostToRegistry)) {
                throw new IllegalArgumentException("Specified invalid registry ip from property:" + DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
            } else if (StringUtils.isEmpty(hostToRegistry)) {
                // bind ip is used as registry ip by default
                hostToRegistry = hostToBind;
            }

            map.put(ANYHOST_KEY, String.valueOf(anyhost));

            return hostToRegistry;
        }


        /**
         * Register port and bind port for the provider, can be configured separately
         * Configuration priority: environment variable -> java system properties -> port property in protocol config file
         * -> protocol default port
         *
         * @param protocolConfig
         * @param name
         * @return
         */
        private static Integer findConfigedPorts(ServiceConfig<?> sc,
                                                 ProtocolConfig protocolConfig,
                                                 String name,
                                                 Map<String, String> map) {
            Integer portToBind = null;

            // parse bind port from environment
            String port = getValueFromConfig(protocolConfig, DUBBO_PORT_TO_BIND);
            portToBind = parsePort(port);

            // if there's no bind port found from environment, keep looking up.
            if (portToBind == null) {
                portToBind = protocolConfig.getPort();
                if (sc.getProvider() != null && (portToBind == null || portToBind == 0)) {
                    portToBind = sc.getProvider().getPort();
                }
                final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
                if (portToBind == null || portToBind == 0) {
                    portToBind = defaultPort;
                }
                if (portToBind == null || portToBind <= 0) {
                    portToBind = getRandomPort(name);
                    if (portToBind == null || portToBind < 0) {
                        portToBind = getAvailablePort(defaultPort);
                        putRandomPort(name, portToBind);
                    }
                }
            }

            // save bind port, used as url's key later
            map.put(BIND_PORT_KEY, String.valueOf(portToBind));

            // registry port, not used as bind port by default
            String portToRegistryStr = getValueFromConfig(protocolConfig, DUBBO_PORT_TO_REGISTRY);
            Integer portToRegistry = parsePort(portToRegistryStr);
            if (portToRegistry == null) {
                portToRegistry = portToBind;
            }

            return portToRegistry;
        }

        private static Integer parsePort(String configPort) {
            Integer port = null;
            if (configPort != null && configPort.length() > 0) {
                try {
                    Integer intPort = Integer.parseInt(configPort);
                    if (isInvalidPort(intPort)) {
                        throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
                    }
                    port = intPort;
                } catch (Exception e) {
                    throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
                }
            }
            return port;
        }

        private static String getValueFromConfig(ProtocolConfig protocolConfig, String key) {
            String protocolPrefix = protocolConfig.getName().toUpperCase() + "_";
            String port = ConfigUtils.getSystemProperty(protocolPrefix + key);
            if (StringUtils.isEmpty(port)) {
                port = ConfigUtils.getSystemProperty(key);
            }
            return port;
        }

        private static Integer getRandomPort(String protocol) {
            protocol = protocol.toLowerCase();
            return RANDOM_PORT_MAP.getOrDefault(protocol, Integer.MIN_VALUE);
        }

        private static void putRandomPort(String protocol, Integer port) {
            protocol = protocol.toLowerCase();
            if (!RANDOM_PORT_MAP.containsKey(protocol)) {
                RANDOM_PORT_MAP.put(protocol, port);
                logger.warn("Use random available port(" + port + ") for protocol " + protocol);
            }
        }

        /**
         * Dispatch an {@link Event event}
         *
         * @param event an {@link Event event}
         * @since 2.7.4
         */
        private static void dispatch(Event event) {
            EventDispatcher.getDefaultExtension().dispatch(event);
        }
    }
}
