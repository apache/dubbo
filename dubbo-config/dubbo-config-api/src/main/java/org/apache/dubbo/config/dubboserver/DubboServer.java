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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceConfigBase;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.event.ReferenceConfigInitializedEvent;
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.event.GenericEventListener;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.store.InMemoryWritableMetadataService;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.ClusterUtils;
import org.apache.dubbo.rpc.cluster.support.registry.ZoneAwareCluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.AsyncMethodInfo;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROXY_CLASS_REF;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SEMICOLON_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.function.ThrowableAction.execute;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidLocalHost;
import static org.apache.dubbo.config.Constants.DUBBO_IP_TO_REGISTRY;
import static org.apache.dubbo.config.ReferenceConfig.REF_PROTOCOL;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_METADATA_PUBLISH_DELAY;
import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PUBLISH_DELAY_KEY;
import static org.apache.dubbo.metadata.WritableMetadataService.getDefaultExtension;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.calInstanceRevision;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setMetadataStorageType;
import static org.apache.dubbo.registry.support.AbstractRegistryFactory.getServiceDiscoveries;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

public class DubboServer extends GenericEventListener {

    private static final String NAME = DubboServer.class.getSimpleName();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static volatile DubboServer instance;

    private final DubboBootstrap dubboBootstrap;

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
     * See {@link ApplicationModel} and {@link ExtensionLoader} for why DubboServer is designed to be singleton.
     */
    public static DubboServer getInstance(DubboBootstrap dubboBootstrap) {
        if (instance == null) {
            synchronized (DubboServer.class) {
                if (instance == null) {
                    instance = new DubboServer(dubboBootstrap);
                }
            }
        }
        return instance;
    }

    public ApplicationConfig getApplication() {
        return dubboBootstrap.getApplication();
    }

    private DubboServer(DubboBootstrap dubboBootstrap) {
        configManager = ApplicationModel.getConfigManager();

        DubboShutdownHook.getDubboShutdownHook().register();
        ShutdownHookCallbacks.INSTANCE.addCallback(new ShutdownHookCallback() {
            @Override
            public void callback() throws Throwable {
                DubboServer.this.destroy();
            }
        });

        this.dubboBootstrap = dubboBootstrap;
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

        dubboBootstrap.setDefaultProperties();


        //这部分要不要补上
//        configManager.getServices().forEach(serviceConfigBase -> {
//            if (serviceConfigBase instanceof ServiceConfig) {
//                ((ServiceConfig) ((ServiceConfig<?>) serviceConfigBase)).beforeExportByDubboServer();
//            }
//        });
//
//        configManager.getReferences().forEach(referenceConfigBase -> {
//            if (referenceConfigBase instanceof ReferenceConfig) {
//                ((ReferenceConfig) ((ReferenceConfig<?>) referenceConfigBase)).beforeInitByDubboServer();
//            }
//        });

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
            initialize();
            if (logger.isInfoEnabled()) {
                logger.info(NAME + " is starting...");
            }

            //initializer the services and 这个操作很关键，是决定错误的原因
//            configManager.getServices().forEach(serviceConfigBase -> {
//                if (serviceConfigBase instanceof ServiceConfig) {
//                    ((ServiceConfig) ((ServiceConfig<?>) serviceConfigBase)).exportByDubboServer();
//                }
//            });
//
//            configManager.getReferences().forEach(referenceConfigBase -> {
//                if (referenceConfigBase instanceof ReferenceConfig) {
//                    ((ReferenceConfig) ((ReferenceConfig<?>) referenceConfigBase)).initByDubboServer();
//                }
//            });

            // 1. export Dubbo Services
            exportServices();

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
        DubboBootstrap dubboBootstrap = referenceConfig.getDubboBootstrap();
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
                    sc.export();
                    exportedServices.add(sc);
                });
                asyncExportingFutures.add(future);
                serviceConfigBase2AsyncExportingFutures.put(sc, future);
            } else {
                sc.export();
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
