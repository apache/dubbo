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
package org.apache.dubbo.config.dubboserver;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.lang.ShutdownHookCallback;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.concurrent.ScheduledCompletableFuture;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceConfigBase;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.event.ReferenceConfigDestroyedEvent;
import org.apache.dubbo.config.event.ReferenceConfigInitializedEvent;
import org.apache.dubbo.config.event.ServiceConfigExportedEvent;
import org.apache.dubbo.config.event.ServiceConfigUnexportedEvent;
import org.apache.dubbo.config.invoker.DelegateProviderMetaDataInvoker;
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.event.GenericEventListener;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.store.InMemoryWritableMetadataService;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.ConfiguratorFactory;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.ClusterUtils;
import org.apache.dubbo.rpc.cluster.support.registry.ZoneAwareCluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.AsyncMethodInfo;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.MAPPING_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROXY_CLASS_REF;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SEMICOLON_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.function.ThrowableAction.execute;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidLocalHost;
import static org.apache.dubbo.config.Constants.DUBBO_IP_TO_REGISTRY;
import static org.apache.dubbo.config.Constants.SCOPE_NONE;
import static org.apache.dubbo.config.ReferenceConfig.REF_PROTOCOL;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_METADATA_PUBLISH_DELAY;
import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PUBLISH_DELAY_KEY;
import static org.apache.dubbo.metadata.WritableMetadataService.getDefaultExtension;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.calInstanceRevision;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setMetadataStorageType;
import static org.apache.dubbo.registry.support.AbstractRegistryFactory.getServiceDiscoveries;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.Constants.PROXY_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

public class DubboServer extends GenericEventListener {

    private static final String NAME = DubboServer.class.getSimpleName();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static volatile DubboServer instance;

    private static volatile DubboBootstrap dubboBootstrap;

    private final AtomicBoolean awaited = new AtomicBoolean(false);

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private final Lock destroyLock = new ReentrantLock();

    private final ExecutorService executorService = newSingleThreadExecutor();

    private final EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    private final ExecutorRepository executorRepository = getExtensionLoader(ExecutorRepository.class).getDefaultExtension();

    private final ConfigManager configManager;

    private ReferenceConfigCache cache;

    private volatile boolean exportAsync;

    private volatile boolean referAsync;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    private AtomicBoolean started = new AtomicBoolean(false);

    private AtomicBoolean startup = new AtomicBoolean(true);

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    private AtomicBoolean shutdown = new AtomicBoolean(false);

    private volatile ServiceInstance serviceInstance;

    private volatile MetadataService metadataService;

    private volatile MetadataServiceExporter metadataServiceExporter;

    private List<ServiceConfigBase<?>> exportedServices = new ArrayList<>();

    private List<Future<?>> asyncExportingFutures = new ArrayList<>();

    private List<CompletableFuture<Object>> asyncReferringFutures = new ArrayList<>();

    private Map<ServiceConfigBase, Future<?>> serviceConfigBase2AsyncExportingFutures = new ConcurrentHashMap<>();

    private Map<ReferenceConfigBase, CompletableFuture<Object>> referenceConfigBase2AsyncExportingFutures = new ConcurrentHashMap<>();

    /**
     * A delayed exposure service timer
     */
    private static final ScheduledExecutorService DELAY_EXPORT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboServiceDelayExporter", true));

    /**
     * See {@link ApplicationModel} and {@link ExtensionLoader} for why DubboServer is designed to be singleton.
     */
    public static DubboServer getInstance() {
        if (instance == null) {
            synchronized (DubboServer.class) {
                if (instance == null) {
                    instance = new DubboServer();
                }
            }
            dubboBootstrap = DubboBootstrap.getInstance();
        }

        return instance;
    }

    public ApplicationConfig getApplication() {
        return dubboBootstrap.getApplication();
    }

    private DubboServer() {
        configManager = ApplicationModel.getConfigManager();

        DubboShutdownHook.getDubboShutdownHook().register();
        ShutdownHookCallbacks.INSTANCE.addCallback(new ShutdownHookCallback() {
            @Override
            public void callback() throws Throwable {
                DubboServer.this.destroy();
            }
        });
    }

    public void unRegisterShutdownHook() {
        DubboShutdownHook.getDubboShutdownHook().unregister();
    }

    private boolean isOnlyRegisterProvider() {
        Boolean registerConsumer = getApplication().getRegisterConsumer();
        return registerConsumer == null || !registerConsumer;
    }

    private String getMetadataType() {
        String type = getApplication().getMetadataType();
        if (StringUtils.isEmpty(type)) {
            type = DEFAULT_METADATA_STORAGE_TYPE;
        }
        return type;
    }

    /**
     * DubboServer is not responsible for configuration.
     */
    public void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info(NAME + " has been initialized!");
        }
    }

    /**
     * Start the bootstrap
     */
    public DubboServer start() {
        if (started.compareAndSet(false, true)) {
            startup.set(false);
            dubboBootstrap.initialize();
            if (logger.isInfoEnabled()) {
                logger.info(NAME + " is starting...");
            }

            // 1. export Dubbo Services
            exportAll();

            // Not only provider register
            if (!isOnlyRegisterProvider() || hasExportedServices()) {
                // 2. export MetadataService
                exportMetadataService();
                //3. Register the local ServiceInstance if required
                registerServiceInstance();
            }

            referAll();

            if (asyncExportingFutures.size() > 0) {
                new Thread(() -> {
                    try {
                        this.awaitFinish();
                    } catch (Exception e) {
                        logger.warn(NAME + " exportAsync occurred an exception.");
                    }
                    startup.set(true);
                    if (logger.isInfoEnabled()) {
                        logger.info(NAME + " is ready.");
                    }
                }).start();
            } else {
                startup.set(true);
                if (logger.isInfoEnabled()) {
                    logger.info(NAME + " is ready.");
                }
            }
            if (logger.isInfoEnabled()) {
                logger.info(NAME + " has started.");
            }
        }
        return this;
    }

    private boolean hasExportedServices() {
        return CollectionUtils.isNotEmpty(configManager.getServices());
    }

    /**
     * Block current thread to be await.
     *
     * @return {@link DubboServer}
     */
    public DubboServer await() {
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

    public DubboServer awaitFinish() throws Exception {
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

    public boolean isStartup() {
        return startup.get();
    }

    public boolean isShutdown() {
        return shutdown.get();
    }

    public DubboServer stop() throws IllegalStateException {
        destroy();
        return this;
    }

    /**
     * Add an instance of {@link EventListener}
     *
     * @param listener {@link EventListener}
     * @return {@link DubboServer}
     */
    public DubboServer addEventListener(EventListener<?> listener) {
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
        if (metadataServiceExporter != null && metadataServiceExporter.isExported()) {
            metadataServiceExporter.unexport();
        }
    }

    public DubboServer exportAsync() {
        this.exportAsync = true;
        return this;
    }

    public DubboServer referAsync() {
        this.referAsync = true;
        return this;
    }


    public DubboServer export(ServiceConfigBase serviceConfigBase) {
        if (serviceConfigBase != null) {
            exportService(serviceConfigBase);
        }
        return this;
    }

    public DubboServer exportAll() {
        exportServices();
        return this;
    }

    public DubboServer refer(ReferenceConfigBase referenceConfigBase) {
        if (referenceConfigBase != null) {
            referService(referenceConfigBase);
        }
        return this;
    }

    public DubboServer referAll() {
        referServices();
        return this;
    }

    public DubboServer unexport(ServiceConfigBase serviceConfigBase) {
        if (serviceConfigBase != null) {
            unexportService(serviceConfigBase);
        }
        return this;
    }

    public DubboServer unexportAll() {
        unexportServices();
        return this;
    }

    public DubboServer unrefer(ReferenceConfigBase referenceConfigBase) {
        if (referenceConfigBase != null) {
            unreferService(referenceConfigBase);
        }

        return this;
    }

    public DubboServer unreferAll() {
        unreferServices();
        return this;
    }

    public void init4ReferenceConfig(ReferenceConfig referenceConfig) {
        if (referenceConfig.isInitialized()) {
            return;
        }

        //use local variables to resonate with init() copied from ReferenceConfig
//        DubboBootstrap dubboBootstrap = referenceConfig.getDubboBootstrap();
        if (dubboBootstrap == null) {
            dubboBootstrap = DubboBootstrap.getInstance();
            dubboBootstrap.initialize();
            dubboBootstrap.reference(referenceConfig);
        }

        referenceConfig.checkAndUpdateSubConfigs();
        Class<?> interfaceClass = referenceConfig.getInterfaceClass();
        referenceConfig.checkStubAndLocal(interfaceClass);
        ConfigValidationUtils.checkMock(interfaceClass, referenceConfig);

        Map<String, String> map = new HashMap<String, String>();
        map.put(SIDE_KEY, CONSUMER_SIDE);

        String generic = referenceConfig.getGeneric();
        String version = referenceConfig.getVersion();

        ReferenceConfigBase.appendRuntimeParameters(map);
        if (!ProtocolUtils.isGeneric(generic)) {
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put(REVISION_KEY, revision);
            }

            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if (methods.length == 0) {
                logger.warn("No method found in service interface " + interfaceClass.getName());
                map.put(METHODS_KEY, ANY_VALUE);
            } else {
                map.put(METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), COMMA_SEPARATOR));
            }
        }

        String interfaceName = referenceConfig.getInterfaceName();
        map.put(INTERFACE_KEY, interfaceName);
        AbstractConfig.appendParameters(map, referenceConfig.getMetrics());
        AbstractConfig.appendParameters(map, getApplication());
        AbstractConfig.appendParameters(map, referenceConfig.getModule());
        // remove 'default.' prefix for configs from ConsumerConfig
        // appendParameters(map, consumer, Constants.DEFAULT_KEY);
        AbstractConfig.appendParameters(map, referenceConfig.getConsumer());
        AbstractConfig.appendParameters(map, referenceConfig);
        MetadataReportConfig metadataReportConfig = referenceConfig.getMetadataReportConfig();
        if (metadataReportConfig != null && metadataReportConfig.isValid()) {
            map.putIfAbsent(METADATA_KEY, REMOTE_METADATA_STORAGE_TYPE);
        }
        Map<String, AsyncMethodInfo> attributes = null;
        if (CollectionUtils.isNotEmpty(referenceConfig.getMethods())) {
            attributes = new HashMap<>();
            for (MethodConfig methodConfig : referenceConfig.getMethods()) {
                AbstractConfig.appendParameters(map, methodConfig, methodConfig.getName());
                String retryKey = methodConfig.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        map.put(methodConfig.getName() + ".retries", "0");
                    }
                }
                AsyncMethodInfo asyncMethodInfo = AbstractConfig.convertMethodConfig2AsyncInfo(methodConfig);
                if (asyncMethodInfo != null) {
//                    consumerModel.getMethodModel(methodConfig.getName()).addAttribute(ASYNC_KEY, asyncMethodInfo);
                    attributes.put(methodConfig.getName(), asyncMethodInfo);
                }
            }
        }

        String hostToRegistry = ConfigUtils.getSystemProperty(DUBBO_IP_TO_REGISTRY);
        if (StringUtils.isEmpty(hostToRegistry)) {
            hostToRegistry = NetUtils.getLocalHost();
        } else if (isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" + DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        }
        map.put(REGISTER_IP_KEY, hostToRegistry);

        ServiceMetadata serviceMetadata = referenceConfig.getServiceMetadata();
        serviceMetadata.getAttachments().putAll(map);

        referenceConfig.setRef(createProxy4ReferenceConfig(map, referenceConfig));

        serviceMetadata.setTarget(referenceConfig.getRef());
        serviceMetadata.addAttribute(PROXY_CLASS_REF, referenceConfig.getRef());
        ConsumerModel consumerModel = referenceConfig.getRepository().lookupReferredService(serviceMetadata.getServiceKey());
        consumerModel.setProxyObject(referenceConfig.getRef());
        consumerModel.init(attributes);

        referenceConfig.setInitialized(true);

        referenceConfig.checkInvokerAvailable();

        // dispatch a ReferenceConfigInitializedEvent since 2.7.4
        referenceConfig.dispatch(new ReferenceConfigInitializedEvent(referenceConfig, referenceConfig.getInvoker()));
    }

    public synchronized void destroy4ReferenceConfig(ReferenceConfig referenceConfig) {
        if (referenceConfig.getRef() == null) {
            return;
        }
        if (referenceConfig.isDestroyed()) {
            return;
        }

        referenceConfig.setDestroyed(true);
        try {
            referenceConfig.getInvoker().destroy();
        } catch (Throwable t) {
            logger.warn("Unexpected error occured when destroy invoker of ReferenceConfig(" + referenceConfig.getUrl() + ").", t);
        }
        referenceConfig.setInvoker(null);
        referenceConfig.setRef(null);

        // dispatch a ReferenceConfigDestroyedEvent since 2.7.4
        referenceConfig.dispatch(new ReferenceConfigDestroyedEvent(referenceConfig));
    }

    private Object createProxy4ReferenceConfig(Map<String, String> map, ReferenceConfig referenceConfig) {
        Class<?> interfaceClass = referenceConfig.getInterfaceClass();


        if (referenceConfig.shouldJvmRefer(map)) {
            URL url = new URL(LOCAL_PROTOCOL, LOCALHOST_VALUE, 0, interfaceClass.getName()).addParameters(map);
            referenceConfig.setInvoker(REF_PROTOCOL.refer(interfaceClass, url));
            if (logger.isInfoEnabled()) {
                logger.info("Using injvm service " + interfaceClass.getName());
            }
        } else {
            List<URL> urls = referenceConfig.getExportedUrls();
            urls.clear();

            String urlGlobal = referenceConfig.getUrl();
            String interfaceName = referenceConfig.getInterfaceName();
            if (urlGlobal != null && urlGlobal.length() > 0) { // user specified URL, could be peer-to-peer address, or register center's address.
                String[] us = SEMICOLON_SPLIT_PATTERN.split(urlGlobal);
                if (us != null && us.length > 0) {
                    for (String u : us) {
                        URL url = URL.valueOf(u);
                        if (StringUtils.isEmpty(url.getPath())) {
                            url = url.setPath(interfaceName);
                        }
                        if (UrlUtils.isRegistry(url)) {
                            urls.add(url.putAttribute(REFER_KEY, map));
                        } else {
                            urls.add(ClusterUtils.mergeUrl(url, map));
                        }
                    }
                }
            } else { // assemble URL from register center's configuration
                // if protocols not injvm checkRegistry
                if (!LOCAL_PROTOCOL.equalsIgnoreCase(referenceConfig.getProtocol())) {
                    referenceConfig.checkRegistry();
                    List<URL> us = ConfigValidationUtils.loadRegistries(referenceConfig, false);
                    if (CollectionUtils.isNotEmpty(us)) {
                        for (URL u : us) {
                            URL monitorUrl = ConfigValidationUtils.loadMonitor(referenceConfig, u);
                            if (monitorUrl != null) {
                                map.put(MONITOR_KEY, URL.encode(monitorUrl.toFullString()));
                            }
                            urls.add(u.putAttribute(REFER_KEY, map));
                        }
                    }
                    if (urls.isEmpty()) {
                        throw new IllegalStateException("No such any registry to reference " + interfaceName + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <dubbo:registry address=\"...\" /> to your spring config.");
                    }
                }
            }

            if (urls.size() == 1) {
                referenceConfig.setInvoker(REF_PROTOCOL.refer(interfaceClass, urls.get(0)));
            } else {
                List<Invoker<?>> invokers = new ArrayList<Invoker<?>>();
                URL registryURL = null;
                for (URL url : urls) {
                    invokers.add(REF_PROTOCOL.refer(interfaceClass, url));
                    if (UrlUtils.isRegistry(url)) {
                        registryURL = url; // use last registry url
                    }
                }
                if (registryURL != null) { // registry url is available
                    // for multi-subscription scenario, use 'zone-aware' policy by default
                    String cluster = registryURL.getParameter(CLUSTER_KEY, ZoneAwareCluster.NAME);
                    // The invoker wrap sequence would be: ZoneAwareClusterInvoker(StaticDirectory) -> FailoverClusterInvoker(RegistryDirectory, routing happens here) -> Invoker
                    referenceConfig.setInvoker(Cluster.getCluster(cluster, false).join(new StaticDirectory(registryURL, invokers)));
                } else { // not a registry url, must be direct invoke.
                    String cluster = CollectionUtils.isNotEmpty(invokers)
                            ? (invokers.get(0).getUrl() != null ? invokers.get(0).getUrl().getParameter(CLUSTER_KEY, ZoneAwareCluster.NAME) : Cluster.DEFAULT)
                            : Cluster.DEFAULT;
                    referenceConfig.setInvoker(Cluster.getCluster(cluster).join(new StaticDirectory(invokers)));
                }
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Referred dubbo service " + interfaceClass.getName());
        }

        URL consumerURL = new URL(CONSUMER_PROTOCOL, map.get(REGISTER_IP_KEY), 0, map.get(INTERFACE_KEY), map);
        MetadataUtils.publishServiceDefinition(consumerURL);

        // create service proxy
        return ReferenceConfig.getProxyFactory().getProxy(referenceConfig.getInvoker(), ProtocolUtils.isGeneric(referenceConfig.getGeneric()));
    }

    public void exported4ServiceConfig(ServiceConfig serviceConfig) {
        List<URL> exportedURLs = serviceConfig.getExportedUrls();
        exportedURLs.forEach(url -> {
            Map<String, String> parameters = getApplication().getParameters();
            ServiceNameMapping.getExtension(parameters != null ? parameters.get(MAPPING_KEY) : null).map(url);
        });
        // dispatch a ServiceConfigExportedEvent since 2.7.4
        serviceConfig.dispatch(new ServiceConfigExportedEvent(serviceConfig));
    }

    public void export4ServiceConfig(ServiceConfig serviceConfig) {
        if (!serviceConfig.shouldExport() || serviceConfig.isExported()) {
            return;
        }
        DubboBootstrap dubboBootstrap = serviceConfig.getDubboBootstrap();

        if (dubboBootstrap == null) {
            dubboBootstrap = DubboBootstrap.getInstance();
            dubboBootstrap.initialize();
            dubboBootstrap.service(serviceConfig);
        }

        serviceConfig.checkAndUpdateSubConfigs();

        ServiceMetadata serviceMetadata = serviceConfig.getServiceMetadata();
        //init serviceMetadata
        serviceMetadata.setVersion(serviceConfig.getVersion());
        serviceMetadata.setGroup(serviceConfig.getGroup());
        serviceMetadata.setDefaultGroup(serviceConfig.getGroup());
        serviceMetadata.setServiceType(serviceConfig.getInterfaceClass());
        serviceMetadata.setServiceInterfaceName(serviceConfig.getInterface());
        serviceMetadata.setTarget(serviceConfig.getRef());

        if (!serviceConfig.shouldExport()) {
            return;
        }

        if (serviceConfig.shouldDelay()) {
            //这没法穿参数，是否要改成 runnable 的东西
            DELAY_EXPORT_EXECUTOR.schedule(new Runnable() {
                @Override
                public void run() {
                    doExport4ServiceConfig(serviceConfig);
                }
            }, serviceConfig.getDelay(), TimeUnit.MILLISECONDS);
        } else {
            doExport4ServiceConfig(serviceConfig);
        }
    }

    public void unexport4ServiceConfig(ServiceConfig serviceConfig) {
        if (!serviceConfig.isExported()) {
            return;
        }
        if (serviceConfig.isUnexported()) {
            return;
        }
        List<Exporter<?>> exporters = serviceConfig.getExporters();
        if (!exporters.isEmpty()) {
            for (Exporter<?> exporter : exporters) {
                try {
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn("Unexpected error occured when unexport " + exporter, t);
                }
            }
            exporters.clear();
        }
        serviceConfig.setUnexported(true);

        // dispatch a ServiceConfigUnExportedEvent since 2.7.4
        serviceConfig.dispatch(new ServiceConfigUnexportedEvent(serviceConfig));
    }

    private void doExport4ServiceConfig(ServiceConfig serviceConfig) {
        if (serviceConfig.isUnexported()) {
            throw new IllegalStateException("The service " + serviceConfig.getInterfaceClass().getName() + " has already unexported!");
        }
        if (serviceConfig.isExported()) {
            return;
        }
        serviceConfig.setExported(true);

        if (StringUtils.isEmpty(serviceConfig.getPath())) {
            serviceConfig.setPath(serviceConfig.getInterfaceName());
        }
        doExportUrls4ServiceConfig(serviceConfig);
        exported4ServiceConfig(serviceConfig);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doExportUrls4ServiceConfig(ServiceConfig serviceConfig) {
        ServiceRepository repository = ApplicationModel.getServiceRepository();
        ServiceDescriptor serviceDescriptor = repository.registerService(serviceConfig.getInterfaceClass());
        repository.registerProvider(
                serviceConfig.getUniqueServiceName(),
                serviceConfig.getRef(),
                serviceDescriptor,
                serviceConfig,
                serviceConfig.getServiceMetadata()
                                   );

        List<URL> registryURLs = ConfigValidationUtils.loadRegistries(serviceConfig, true);

        for (ProtocolConfig protocolConfig : serviceConfig.getProtocols()) {
            String pathKey = URL.buildKey((String) serviceConfig.getContextPath(protocolConfig)
                    .map(p -> p + "/" + serviceConfig.getPath())
                    .orElse(serviceConfig.getPath()), serviceConfig.getGroup(), serviceConfig.getVersion());
            // In case user specified path, register service one more time to map it to path.
            repository.registerService(pathKey, serviceConfig.getInterfaceClass());
            // TODO, uncomment this line once service key is unified
            serviceConfig.getServiceMetadata().setServiceKey(pathKey);
            doExportUrlsFor1Protocol(protocolConfig, registryURLs, serviceConfig);
        }
    }

    private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs, ServiceConfig serviceConfig) {
        Class<?> interfaceClass = serviceConfig.getInterfaceClass();
        String generic = serviceConfig.getGeneric();

        String name = protocolConfig.getName();
        if (StringUtils.isEmpty(name)) {
            name = DUBBO;
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put(SIDE_KEY, PROVIDER_SIDE);

        ServiceConfig.appendRuntimeParameters(map);
        AbstractConfig.appendParameters(map, serviceConfig.getMetrics());
        AbstractConfig.appendParameters(map, serviceConfig.getApplication());
        AbstractConfig.appendParameters(map, serviceConfig.getModule());
        // remove 'default.' prefix for configs from ProviderConfig
        // appendParameters(map, provider, Constants.DEFAULT_KEY);
        AbstractConfig.appendParameters(map, serviceConfig.getProvider());
        AbstractConfig.appendParameters(map, protocolConfig);
        AbstractConfig.appendParameters(map, this);
        MetadataReportConfig metadataReportConfig = serviceConfig.getMetadataReportConfig();
        if (metadataReportConfig != null && metadataReportConfig.isValid()) {
            map.putIfAbsent(METADATA_KEY, REMOTE_METADATA_STORAGE_TYPE);
        }
        if (CollectionUtils.isNotEmpty(serviceConfig.getMethods())) {
            for (MethodConfig method : serviceConfig.getMethods()) {
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
                            if (methods.length > 0) {
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

        if (ProtocolUtils.isGeneric(generic)) {
            map.put(GENERIC_KEY, generic);
            map.put(METHODS_KEY, ANY_VALUE);
        } else {
            String revision = Version.getVersion(interfaceClass, serviceConfig.getVersion());
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

        String token = serviceConfig.getToken();
        /**
         * Here the token value configured by the provider is used to assign the value to ServiceConfig#token
         */
        if (ConfigUtils.isEmpty(token) && serviceConfig.getProvider() != null) {
            serviceConfig.setToken(serviceConfig.getProvider().getToken());
        }

        if (!ConfigUtils.isEmpty(token)) {
            if (ConfigUtils.isDefault(token)) {
                map.put(TOKEN_KEY, UUID.randomUUID().toString());
            } else {
                map.put(TOKEN_KEY, token);
            }
        }
        ServiceMetadata serviceMetadata = serviceConfig.getServiceMetadata();
        //init serviceMetadata attachments
        serviceMetadata.getAttachments().putAll(map);

        // export service
        String host = serviceConfig.findConfigedHosts(protocolConfig, registryURLs, map);
        Integer port = serviceConfig.findConfigedPorts(protocolConfig, name, map);
        String path = serviceConfig.getPath();
        URL url = new URL(name, host, port, (String) serviceConfig.getContextPath(protocolConfig).map(p -> p + "/" + path).orElse(path), map);

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
                exportLocal(url, serviceConfig);
            }
            // export to remote if the config is not local (export to local only when config is local)
            if (!SCOPE_LOCAL.equalsIgnoreCase(scope)) {
                if (CollectionUtils.isNotEmpty(registryURLs)) {
                    for (URL registryURL : registryURLs) {
                        //if protocol is only injvm ,not register
                        if (LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
                            continue;
                        }
                        url = url.addParameterIfAbsent(DYNAMIC_KEY, registryURL.getParameter(DYNAMIC_KEY));
                        URL monitorUrl = ConfigValidationUtils.loadMonitor(serviceConfig, registryURL);
                        if (monitorUrl != null) {
                            url = url.addParameterAndEncoded(MONITOR_KEY, monitorUrl.toFullString());
                        }
                        if (logger.isInfoEnabled()) {
                            if (url.getParameter(REGISTER_KEY, true)) {
                                logger.info("Register dubbo service " + interfaceClass.getName() + " url " + url.getServiceKey() + " to registry " + registryURL.getAddress());
                            } else {
                                logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url.getServiceKey());
                            }
                        }

                        // For providers, this is used to enable custom proxy to generate invoker
                        String proxy = url.getParameter(PROXY_KEY);
                        if (StringUtils.isNotEmpty(proxy)) {
                            registryURL = registryURL.addParameter(PROXY_KEY, proxy);
                        }

                        Invoker<?> invoker = ServiceConfig.PROXY_FACTORY.getInvoker(serviceConfig.getRef(), (Class) interfaceClass, registryURL.putAttribute(EXPORT_KEY, url));
                        DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, serviceConfig);

                        Exporter<?> exporter = ServiceConfig.PROTOCOL.export(wrapperInvoker);
                        serviceConfig.addExporter(exporter);
                    }
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                    }
                    if (MetadataService.class.getName().equals(url.getServiceInterface())) {
                        MetadataUtils.saveMetadataURL(url);
                    }
                    Invoker<?> invoker = ServiceConfig.PROXY_FACTORY.getInvoker(serviceConfig.getRef(), (Class) interfaceClass, url);
                    DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, serviceConfig);

                    Exporter<?> exporter = ServiceConfig.PROTOCOL.export(wrapperInvoker);
                    serviceConfig.addExporter(exporter);
                }

                MetadataUtils.publishServiceDefinition(url);
            }
        }
        serviceConfig.getExportedUrls().add(url);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    /**
     * always export injvm
     */
    private void exportLocal(URL url, ServiceConfig serviceConfig) {
        URL local = URLBuilder.from(url)
                .setProtocol(LOCAL_PROTOCOL)
                .setHost(LOCALHOST_VALUE)
                .setPort(0)
                .build();
        Exporter<?> exporter = ServiceConfig.PROTOCOL.export(
                ServiceConfig.PROXY_FACTORY.getInvoker(serviceConfig.getRef(), (Class) serviceConfig.getInterfaceClass(), local));
        serviceConfig.addExporter(exporter);
        logger.info("Export dubbo service " + serviceConfig.getInterfaceClass().getName() + " to local registry url : " + local);
    }


    private void exportServices() {
        configManager.getServices().forEach(sc -> {
            if (sc instanceof ServiceConfig) {
                exportService(sc);
            }
        });
    }

    private void unexportServices() {
        exportedServices.forEach(sc -> {
            unexportService(sc);
        });
        asyncExportingFutures.clear();
        exportedServices.clear();
    }

    private void referServices() {
        configManager.getReferences().forEach(rc -> {
            if (rc instanceof ReferenceConfig) {
                referService(rc);
            }
        });
    }

    private void unreferServices() {
        if (cache == null) {
            cache = ReferenceConfigCache.getCache();
        }

        configManager.getReferences().stream().filter(referenceConfigBase ->
        {
            return referenceConfigBase instanceof ReferenceConfig;
        }).forEach(this::unreferService);

        referenceConfigBase2AsyncExportingFutures.clear();
        asyncReferringFutures.clear();
        cache.destroyAll();
    }

    private void exportService(ServiceConfigBase sc) {
        if (sc instanceof ServiceConfig) {
            if (exportAsync) {
                ExecutorService executor = executorRepository.getServiceExporterExecutor();
                Future<?> future = executor.submit(() -> {
                    export4ServiceConfig((ServiceConfig)sc);
                    exportedServices.add(sc);
                });
                asyncExportingFutures.add(future);
                serviceConfigBase2AsyncExportingFutures.put(sc, future);
            } else {
                export4ServiceConfig((ServiceConfig)sc);
                exportedServices.add(sc);
            }
        }
    }

    private void unexportService(ServiceConfigBase sc) {
        if (sc == null || !exportedServices.contains(sc)) {
            return;
        }
        sc.unexport();
        cancleAsyncExportingFutures(sc);
        exportedServices.remove(sc);
    }

    private void referService(ReferenceConfigBase rc) {
        if (cache == null) {
            cache = ReferenceConfigCache.getCache();
        }
        if (rc instanceof ReferenceConfig) {
            if (rc.shouldInit()) {
                if (referAsync) {
                    CompletableFuture<Object> future = ScheduledCompletableFuture.submit(
                            executorRepository.getServiceExporterExecutor(),
                            () -> cache.get(rc));
                    asyncReferringFutures.add(future);
                    referenceConfigBase2AsyncExportingFutures.put(rc, future);
                } else {
                    cache.get(rc);
                }
            }
        }
    }

    private void unreferService(ReferenceConfigBase rc) {
        if (rc == null) {
            return;
        }
        cache.destroy(rc);
        cancelAsyncReferringFutures(rc);
        return;
    }

    private void cancleAsyncExportingFutures(ServiceConfigBase sc) {
        if (exportAsync && sc != null) {
            Future<?> future = serviceConfigBase2AsyncExportingFutures.get(sc);
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }

            asyncExportingFutures.remove(future);
            serviceConfigBase2AsyncExportingFutures.remove(sc);
        }
    }

    private void cancelAsyncReferringFutures(ReferenceConfigBase referenceConfigBase) {
        if (referenceConfigBase != null && referAsync) {
            CompletableFuture<Object> future = referenceConfigBase2AsyncExportingFutures.get(referenceConfigBase);
            if (!future.isDone()) {
                future.cancel(true);
            }
            referenceConfigBase2AsyncExportingFutures.remove(referenceConfigBase);
            asyncReferringFutures.remove(future);
        }
    }

    private void registerServiceInstance() {
        ApplicationConfig application = getApplication();

        String serviceName = application.getName();

        ServiceInstance serviceInstance = createServiceInstance(serviceName);

        doRegisterServiceInstance(serviceInstance);

        // scheduled task for updating Metadata and ServiceInstance
        executorRepository.nextScheduledExecutor().scheduleAtFixedRate(() -> {
            InMemoryWritableMetadataService localMetadataService = (InMemoryWritableMetadataService) WritableMetadataService.getDefaultExtension();
            localMetadataService.blockUntilUpdated();
            ServiceInstanceMetadataUtils.refreshMetadataAndInstance(serviceInstance);
        }, 0, ConfigurationUtils.get(METADATA_PUBLISH_DELAY_KEY, DEFAULT_METADATA_PUBLISH_DELAY), TimeUnit.MILLISECONDS);
    }

    private void doRegisterServiceInstance(ServiceInstance serviceInstance) {
        // register instance only when at least one service is exported.
        if (serviceInstance.getPort() != null && serviceInstance.getPort() != -1) {
            publishMetadataToRemote(serviceInstance);
            logger.info("Start registering instance address to registry.");
            getServiceDiscoveries().forEach(serviceDiscovery ->
            {
                calInstanceRevision(serviceDiscovery, serviceInstance);
                if (logger.isDebugEnabled()) {
                    logger.info("Start registering instance address to registry" + serviceDiscovery.getUrl() + ", instance " + serviceInstance);
                }
                // register metadata
                serviceDiscovery.register(serviceInstance);
            });
        }
    }

    private void publishMetadataToRemote(ServiceInstance serviceInstance) {
//        InMemoryWritableMetadataService localMetadataService = (InMemoryWritableMetadataService)WritableMetadataService.getDefaultExtension();
//        localMetadataService.blockUntilUpdated();
        if (logger.isInfoEnabled()) {
            logger.info("Start publishing metadata to remote center, this only makes sense for applications enabled remote metadata center.");
        }
        RemoteMetadataServiceImpl remoteMetadataService = MetadataUtils.getRemoteMetadataService();
        remoteMetadataService.publishMetadata(serviceInstance.getServiceName());
    }

    private void unregisterServiceInstance() {
        if (serviceInstance != null) {
            getServiceDiscoveries().forEach(serviceDiscovery -> {
                serviceDiscovery.unregister(serviceInstance);
            });
        }
    }

    private ServiceInstance createServiceInstance(String serviceName) {
        this.serviceInstance = new DefaultServiceInstance(serviceName);
        setMetadataStorageType(serviceInstance, getMetadataType());
        ServiceInstanceMetadataUtils.customizeInstance(this.serviceInstance);
        return this.serviceInstance;
    }

    public void destroy() {
        if (destroyLock.tryLock()
                && shutdown.compareAndSet(false, true)) {
            try {
                DubboShutdownHook.destroyAll();

                if (started.compareAndSet(true, false)
                        && destroyed.compareAndSet(false, true)) {

                    unregisterServiceInstance();
                    unexportMetadataService();
                    unexportServices();
                    unreferServices();

                    destroyRegistries();
                    DubboShutdownHook.destroyProtocols();
                    destroyServiceDiscoveries();

                    clear();
                    shutdown();
                    release();
                }
            } finally {
                destroyLock.unlock();
            }
        }
    }

    private void destroyRegistries() {
        AbstractRegistryFactory.destroyAll();
    }

    private void destroyServiceDiscoveries() {
        getServiceDiscoveries().forEach(serviceDiscovery -> {
            execute(serviceDiscovery::destroy);
        });
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ServiceDiscoveries have been destroyed.");
        }
    }

    private void clear() {
        clearConfigs();
        clearApplicationModel();
    }

    private void clearApplicationModel() {

    }

    private void clearConfigs() {
        configManager.destroy();
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

    /**
     * Initialize {@link MetadataService} from {@link WritableMetadataService}'s extension
     */
    public void initMetadataService() {
//        startMetadataCenter();
        this.metadataService = getDefaultExtension();
        this.metadataServiceExporter = new ConfigurableMetadataServiceExporter(metadataService);
    }

}
