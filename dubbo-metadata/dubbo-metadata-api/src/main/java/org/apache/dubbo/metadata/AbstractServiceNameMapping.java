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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_FAILED_LOAD_MAPPING_CACHE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_PROPERTY_TYPE_MISMATCH;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SERVER_DISCONNECTED;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.toTreeSet;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

public abstract class AbstractServiceNameMapping implements ServiceNameMapping {
    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());
    protected ApplicationModel applicationModel;
    private final MappingCacheManager mappingCacheManager;
    private final ConcurrentHashMap<String, Set<MappingListener>> mappingListeners = new ConcurrentHashMap<>();
    // mapping lock is shared among registries of the same application.
    private final ConcurrentMap<String, ReentrantLock> mappingLocks = new ConcurrentHashMap<>();
    protected MetadataReportInstance metadataReportInstance;
    protected static final List<String> IGNORED_SERVICE_INTERFACES =
            Collections.singletonList(MetadataService.class.getName());

    public AbstractServiceNameMapping(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        boolean enableFileCache = true;
        Optional<ApplicationConfig> application =
                applicationModel.getApplicationConfigManager().getApplication();
        if (application.isPresent()) {
            enableFileCache = Boolean.TRUE.equals(application.get().getEnableFileCache());
        }
        this.mappingCacheManager = new MappingCacheManager(
                enableFileCache,
                applicationModel.tryGetApplicationName(),
                applicationModel
                        .getFrameworkModel()
                        .getBeanFactory()
                        .getBean(FrameworkExecutorRepository.class)
                        .getCacheRefreshingScheduledExecutor());
        metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
    }

    // only for ut
    public void setMetadataReportInstance(MetadataReportInstance metadataReportInstance) {
        this.metadataReportInstance = metadataReportInstance;
    }

    @Override
    public void mapping(URL url) {
        if (CollectionUtils.isEmpty(
                applicationModel.getApplicationConfigManager().getMetadataConfigs())) {
            logger.warn(
                    COMMON_PROPERTY_TYPE_MISMATCH,
                    "",
                    "",
                    "[METADATA_REGISTER] [SERVICE_NAME_MAPPING] No valid metadata config center found for mapping report.");
            return;
        }
        String serviceInterface = url.getServiceInterface();
        if (IGNORED_SERVICE_INTERFACES.contains(serviceInterface)) {
            return;
        }

        String appName = applicationModel.getApplicationName();
        for (Map.Entry<String, MetadataReport> entry :
                metadataReportInstance.getMetadataReports(true).entrySet()) {
            MetadataReport metadataReport = entry.getValue();
            if (metadataReport.registerServiceAppMapping(serviceInterface, appName, url)) {
                // MetadataReport support directly register service-app mapping
                continue;
            }

            boolean succeeded = false;
            try {
                succeeded = doMapping(metadataReport, url);
            } catch (Exception e) {
                logger.warn(
                        CONFIG_SERVER_DISCONNECTED,
                        e.getMessage(),
                        "service: " + serviceInterface + ", metadata-center url: " + metadataReport.getUrl(),
                        "[METADATA_REGISTER] [SERVICE_NAME_MAPPING] Failed registering mapping to remote."
                                + metadataReport,
                        e);
            }
            if (!succeeded) {
                getServiceNameMappingReportRetry().addTask(metadataReport, url);
            }
        }
        getServiceNameMappingReportRetry().start();
    }

    protected boolean doMapping(MetadataReport metadataReport, URL url) {
        if (!metadataReport.isAvailable()) {
            throw new IllegalStateException("metadata reporter is not available");
        }
        String appName = applicationModel.getApplicationName(), newConfigContent = appName;
        String serviceInterface = url.getServiceInterface();

        ConfigItem configItem = metadataReport.getConfigItem(serviceInterface, DEFAULT_MAPPING_GROUP);
        String oldConfigContent = configItem.getContent();
        boolean exist = false;
        if (StringUtils.isNotEmpty(oldConfigContent)) {
            String[] oldAppNames = oldConfigContent.split(COMMA_SEPARATOR);
            for (String oldAppName : oldAppNames) {
                if (StringUtils.trim(oldAppName).equals(appName)) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                newConfigContent = oldConfigContent + COMMA_SEPARATOR + appName;
            }
        }
        if (exist) {
            return true;
        }
        return metadataReport.registerServiceAppMapping(
                serviceInterface, DEFAULT_MAPPING_GROUP, newConfigContent, configItem.getTicket());
    }

    private static final Object initializeRetry = new Object();
    private static final NamedThreadFactory namedThreadFactory =
            new NamedThreadFactory("DubboServiceNameMappingReportRetry", true);
    private volatile ServiceNameMappingReportRetry serviceNameMappingReportRetry;

    protected ServiceNameMappingReportRetry getServiceNameMappingReportRetry() {
        if (serviceNameMappingReportRetry == null) {
            synchronized (initializeRetry) {
                if (serviceNameMappingReportRetry == null) {
                    serviceNameMappingReportRetry = new ServiceNameMappingReportRetry(namedThreadFactory);
                }
            }
        }
        return serviceNameMappingReportRetry;
    }

    class ServiceNameMappingReportRetry {
        private final ScheduledExecutorService executor;
        private volatile ScheduledFuture<?> future;
        private final Map<MetadataReport, Set<URL>> taskQueue = new ConcurrentHashMap<>();
        private final Object startLock = new Object();

        private ServiceNameMappingReportRetry(NamedThreadFactory namedThreadFactory) {
            this.executor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
        }

        void start() {
            if (future == null) {
                synchronized (startLock) {
                    if (future == null) {
                        future = executor.scheduleWithFixedDelay(this::run, 1000, 3000, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }

        void addTask(MetadataReport metadataReport, URL url) {
            taskQueue
                    .computeIfAbsent(metadataReport, k -> new ConcurrentHashSet<>())
                    .add(url);
        }

        void cancel() {
            if (future != null) {
                future.cancel(false);
            }
            executor.shutdown();
            serviceNameMappingReportRetry = null;
        }

        private void run() {
            if (taskQueue.isEmpty()) {
                cancel();
                return;
            }
            for (Entry<MetadataReport, Set<URL>> entry : taskQueue.entrySet()) {
                MetadataReport metadataReport = entry.getKey();
                if (!metadataReport.isAvailable()) {
                    logger.warn(
                            CONFIG_SERVER_DISCONNECTED,
                            "connect is not available",
                            "metadata-center url: " + metadataReport.getUrl(),
                            "[METADATA_REGISTER] [SERVICE_NAME_MAPPING] Retry Failed.");
                    continue;
                }
                Set<URL> urlSet = taskQueue.remove(metadataReport);
                for (URL url : urlSet) {
                    try {
                        AbstractServiceNameMapping.this.doMapping(metadataReport, url);
                    } catch (Throwable e) {
                        logger.warn(
                                CONFIG_SERVER_DISCONNECTED,
                                e.getMessage(),
                                "service url: " + url + ", metadata-center url: " + metadataReport.getUrl(),
                                "[METADATA_REGISTER] [SERVICE_NAME_MAPPING] Retry Failed. Add Retry Task.",
                                e);
                        addTask(metadataReport, url);
                    }
                }
            }
        }
    }

    // just for test
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    /**
     * Get the service names from the specified Dubbo service interface, group, version and protocol
     *
     * @return
     */
    public abstract Set<String> get(URL url);

    /**
     * Get the service names from the specified Dubbo service interface, group, version and protocol
     *
     * @return
     */
    public abstract Set<String> getAndListen(URL url, MappingListener mappingListener);

    protected abstract void removeListener(URL url, MappingListener mappingListener);

    @Override
    public Set<String> getAndListen(URL registryURL, URL subscribedURL, MappingListener listener) {
        String key = ServiceNameMapping.buildMappingKey(subscribedURL);
        // use previously cached services.
        Set<String> mappingServices = mappingCacheManager.get(key);

        // Asynchronously register listener in case previous cache does not exist or cache expired.
        if (CollectionUtils.isEmpty(mappingServices)) {
            try {
                logger.info("[METADATA_REGISTER] Local cache mapping is empty");
                mappingServices = (new AsyncMappingTask(listener, subscribedURL, false)).call();
            } catch (Exception e) {
                // ignore
            }
            if (CollectionUtils.isEmpty(mappingServices)) {
                String registryServices = registryURL.getParameter(SUBSCRIBED_SERVICE_NAMES_KEY);
                if (StringUtils.isNotEmpty(registryServices)) {
                    logger.info(subscribedURL.getServiceInterface() + " mapping to " + registryServices
                            + " instructed by registry subscribed-services.");
                    mappingServices = parseServices(registryServices);
                }
            }
            if (CollectionUtils.isNotEmpty(mappingServices)) {
                this.putCachedMapping(ServiceNameMapping.buildMappingKey(subscribedURL), mappingServices);
            }
        } else {
            ExecutorService executorService = applicationModel
                    .getFrameworkModel()
                    .getBeanFactory()
                    .getBean(FrameworkExecutorRepository.class)
                    .getMappingRefreshingExecutor();
            executorService.submit(new AsyncMappingTask(listener, subscribedURL, true));
        }

        return mappingServices;
    }

    @Override
    public MappingListener stopListen(URL subscribeURL, MappingListener listener) {
        synchronized (mappingListeners) {
            if (listener != null) {
                String mappingKey = ServiceNameMapping.buildMappingKey(subscribeURL);
                Set<MappingListener> listeners = mappingListeners.get(mappingKey);
                // todo, remove listener from remote metadata center
                if (CollectionUtils.isNotEmpty(listeners)) {
                    listeners.remove(listener);
                    listener.stop();
                    removeListener(subscribeURL, listener);
                }
                if (CollectionUtils.isEmpty(listeners)) {
                    mappingListeners.remove(mappingKey);
                    removeCachedMapping(mappingKey);
                    removeMappingLock(mappingKey);
                }
            }
            return listener;
        }
    }

    static Set<String> parseServices(String literalServices) {
        return isBlank(literalServices)
                ? emptySet()
                : unmodifiableSet(new TreeSet<>(of(literalServices.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotEmpty)
                        .collect(toSet())));
    }

    @Override
    public void putCachedMapping(String serviceKey, Set<String> apps) {
        mappingCacheManager.put(serviceKey, toTreeSet(apps));
    }

    protected void putCachedMappingIfAbsent(String serviceKey, Set<String> apps) {
        Lock lock = getMappingLock(serviceKey);
        try {
            lock.lock();
            if (CollectionUtils.isEmpty(mappingCacheManager.get(serviceKey))) {
                mappingCacheManager.put(serviceKey, toTreeSet(apps));
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<String> getMapping(URL consumerURL) {
        Set<String> mappingByUrl = ServiceNameMapping.getMappingByUrl(consumerURL);
        if (mappingByUrl != null) {
            return mappingByUrl;
        }
        return mappingCacheManager.get(ServiceNameMapping.buildMappingKey(consumerURL));
    }

    @Override
    public Set<String> getRemoteMapping(URL consumerURL) {
        return get(consumerURL);
    }

    @Override
    public Set<String> removeCachedMapping(String serviceKey) {
        return mappingCacheManager.remove(serviceKey);
    }

    public Lock getMappingLock(String key) {
        return ConcurrentHashMapUtils.computeIfAbsent(mappingLocks, key, _k -> new ReentrantLock());
    }

    protected void removeMappingLock(String key) {
        Lock lock = mappingLocks.get(key);
        if (lock != null) {
            try {
                lock.lock();
                mappingLocks.remove(key);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void $destroy() {
        mappingCacheManager.destroy();
        mappingListeners.clear();
        mappingLocks.clear();
        if (serviceNameMappingReportRetry != null) {
            serviceNameMappingReportRetry.cancel();
        }
    }

    private class AsyncMappingTask implements Callable<Set<String>> {
        private final MappingListener listener;
        private final URL subscribedURL;
        private final boolean notifyAtFirstTime;

        public AsyncMappingTask(MappingListener listener, URL subscribedURL, boolean notifyAtFirstTime) {
            this.listener = listener;
            this.subscribedURL = subscribedURL;
            this.notifyAtFirstTime = notifyAtFirstTime;
        }

        @Override
        public Set<String> call() throws Exception {
            synchronized (mappingListeners) {
                Set<String> mappedServices = emptySet();
                try {
                    String mappingKey = ServiceNameMapping.buildMappingKey(subscribedURL);
                    if (listener != null) {
                        mappedServices = toTreeSet(getAndListen(subscribedURL, listener));
                        Set<MappingListener> listeners = ConcurrentHashMapUtils.computeIfAbsent(
                                mappingListeners, mappingKey, _k -> new HashSet<>());
                        listeners.add(listener);
                        if (CollectionUtils.isNotEmpty(mappedServices)) {
                            if (notifyAtFirstTime) {
                                // guarantee at-least-once notification no matter what kind of underlying meta server is
                                // used.
                                // listener notification will also cause updating of mapping cache.
                                listener.onEvent(new MappingChangedEvent(mappingKey, mappedServices));
                            }
                        }
                    } else {
                        mappedServices = get(subscribedURL);
                        if (CollectionUtils.isNotEmpty(mappedServices)) {
                            AbstractServiceNameMapping.this.putCachedMapping(mappingKey, mappedServices);
                        }
                    }
                } catch (Exception e) {
                    logger.error(
                            COMMON_FAILED_LOAD_MAPPING_CACHE,
                            "",
                            "",
                            "Failed getting mapping info from remote center. ",
                            e);
                }
                return mappedServices;
            }
        }
    }
}
