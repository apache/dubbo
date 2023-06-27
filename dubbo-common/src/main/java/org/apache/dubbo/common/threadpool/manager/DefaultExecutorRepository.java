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
package org.apache.dubbo.common.threadpool.manager;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionAccessor;
import org.apache.dubbo.common.extension.ExtensionAccessorAware;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.store.DataStore;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.rpc.executor.DefaultExecutorSupport;
import org.apache.dubbo.rpc.executor.ExecutorSupport;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SHARED_EXECUTOR_SERVICE_COMPONENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_EXPORT_THREAD_NUM;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_REFER_THREAD_NUM;
import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_ERROR_USE_THREAD_POOL;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_EXECUTORS_NO_FOUND;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXECUTORS_SHUTDOWN;

/**
 * Consider implementing {@code Lifecycle} to enable executors shutdown when the process stops.
 */
public class DefaultExecutorRepository implements ExecutorRepository, ExtensionAccessorAware {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DefaultExecutorRepository.class);

    private static final String MAX_KEY = String.valueOf(Integer.MAX_VALUE);

    private volatile ScheduledExecutorService serviceExportExecutor;

    private volatile ExecutorService serviceReferExecutor;

    private final ConcurrentMap<String, ConcurrentMap<String, ExecutorService>> data = new ConcurrentHashMap<>();

    private final Object LOCK = new Object();
    private ExtensionAccessor extensionAccessor;

    private final ApplicationModel applicationModel;
    private final FrameworkExecutorRepository frameworkExecutorRepository;
    private ExecutorSupport executorSupport;

    private final DataStore dataStore;

    public DefaultExecutorRepository(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.frameworkExecutorRepository = applicationModel.getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class);
        this.dataStore = applicationModel.getExtensionLoader(DataStore.class).getDefaultExtension();
    }

    /**
     * Get called when the server or client instance initiating.
     *
     * @param url
     * @return
     */
    @Override
    public synchronized ExecutorService createExecutorIfAbsent(URL url) {
        String executorKey = getExecutorKey(url);
        ConcurrentMap<String, ExecutorService> executors = ConcurrentHashMapUtils.computeIfAbsent(data, executorKey, k -> new ConcurrentHashMap<>());

        String executorCacheKey = getExecutorSecondKey(url);

        url = setThreadNameIfAbsent(url, executorCacheKey);

        URL finalUrl = url;
        ExecutorService executor = ConcurrentHashMapUtils.computeIfAbsent(executors, executorCacheKey, k -> createExecutor(finalUrl));
        // If executor has been shut down, create a new one
        if (executor.isShutdown() || executor.isTerminated()) {
            executors.remove(executorCacheKey);
            executor = createExecutor(url);
            executors.put(executorCacheKey, executor);
        }
        dataStore.put(executorKey, executorCacheKey, executor);
        return executor;
    }

    protected URL setThreadNameIfAbsent(URL url, String executorCacheKey) {
        if (url.getParameter(THREAD_NAME_KEY) == null) {
            String protocol = url.getProtocol();
            if (StringUtils.isEmpty(protocol)) {
                protocol = DEFAULT_PROTOCOL;
            }
            url = url.putAttribute(THREAD_NAME_KEY, protocol + "-protocol-" + executorCacheKey);
        }
        return url;
    }

    private String getExecutorSecondKey(URL url) {
        if (CONSUMER_SIDE.equalsIgnoreCase(url.getParameter(SIDE_KEY))) {
            return getConsumerKey(url);
        } else {
            return getProviderKey(url);
        }
    }

    private String getExecutorSecondKey(ServiceModel serviceModel, URL url) {
        if (serviceModel instanceof ConsumerModel) {
            return getConsumerKey(serviceModel);
        } else {
            return getProviderKey((ProviderModel) serviceModel, url);
        }
    }

    private String getConsumerKey(URL url) {
        // Consumer's executor is sharing globally, key=Integer.MAX_VALUE
        return String.valueOf(Integer.MAX_VALUE);
    }

    private String getConsumerKey(ServiceModel serviceModel) {
        // Consumer's executor is sharing globally, key=Integer.MAX_VALUE
        return MAX_KEY;
    }

    protected String getProviderKey(URL url) {
        // Provider's executor is sharing by protocol.
        return String.valueOf(url.getPort());
    }

    protected String getProviderKey(ProviderModel providerModel, URL url) {
        // Provider's executor is sharing by protocol.
        return String.valueOf(url.getPort());
    }

    /**
     * Return the executor key based on the type (internal or biz) of the current service.
     *
     * @param url
     * @return
     */
    private String getExecutorKey(URL url) {
        if (CONSUMER_SIDE.equalsIgnoreCase(url.getParameter(SIDE_KEY))) {
            return CONSUMER_SHARED_EXECUTOR_SERVICE_COMPONENT_KEY;
        }
        return EXECUTOR_SERVICE_COMPONENT_KEY;
    }

    private String getExecutorKey(ServiceModel serviceModel) {
        if (serviceModel instanceof ProviderModel) {
            return EXECUTOR_SERVICE_COMPONENT_KEY;
        } else {
            return CONSUMER_SHARED_EXECUTOR_SERVICE_COMPONENT_KEY;
        }
    }

    protected ExecutorService createExecutor(URL url) {
        return (ExecutorService) extensionAccessor.getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);
    }

    @Override
    public ExecutorService getExecutor(URL url) {
        Map<String, ExecutorService> executors = data.get(getExecutorKey(url));

        /*
         * It's guaranteed that this method is called after {@link #createExecutorIfAbsent(URL)}, so data should already
         * have Executor instances generated and stored.
         */
        if (executors == null) {
            logger.warn(COMMON_EXECUTORS_NO_FOUND, "", "", "No available executors, this is not expected, framework should call createExecutorIfAbsent first" +
                "before coming to here.");

            return null;
        }

        // Consumer's executor is sharing globally, key=Integer.MAX_VALUE. Provider's executor is sharing by protocol.
        String executorCacheKey = getExecutorSecondKey(url);
        ExecutorService executor = executors.get(executorCacheKey);
        if (executor != null && (executor.isShutdown() || executor.isTerminated())) {
            executors.remove(executorCacheKey);
            // Does not re-create a shutdown executor, use SHARED_EXECUTOR for downgrade.
            executor = null;
            logger.info("Executor for " + url + " is shutdown.");
        }
        if (executor == null) {
            return frameworkExecutorRepository.getSharedExecutor();
        } else {
            return executor;
        }
    }

    @Override
    public ExecutorService getExecutor(ServiceModel serviceModel, URL url) {
        Map<String, ExecutorService> executors = data.get(getExecutorKey(serviceModel));

        /*
         * It's guaranteed that this method is called after {@link #createExecutorIfAbsent(URL)}, so data should already
         * have Executor instances generated and stored.
         */
        if (executors == null) {
            logger.warn(COMMON_EXECUTORS_NO_FOUND, "", "", "No available executors, this is not expected, framework should call createExecutorIfAbsent first" +
                "before coming to here.");

            return null;
        }

        // Consumer's executor is sharing globally, key=Integer.MAX_VALUE. Provider's executor is sharing by protocol.
        String executorCacheKey = getExecutorSecondKey(serviceModel, url);
        ExecutorService executor = executors.get(executorCacheKey);
        if (executor != null && (executor.isShutdown() || executor.isTerminated())) {
            executors.remove(executorCacheKey);
            // Does not re-create a shutdown executor, use SHARED_EXECUTOR for downgrade.
            executor = null;
            logger.info("Executor for " + url + " is shutdown.");
        }
        if (executor == null) {
            return frameworkExecutorRepository.getSharedExecutor();
        } else {
            return executor;
        }
    }

    @Override
    public void updateThreadpool(URL url, ExecutorService executor) {
        try {
            if (url.hasParameter(THREADS_KEY)
                && executor instanceof ThreadPoolExecutor && !executor.isShutdown()) {
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                int threads = url.getParameter(THREADS_KEY, 0);
                int max = threadPoolExecutor.getMaximumPoolSize();
                int core = threadPoolExecutor.getCorePoolSize();
                if (threads > 0 && (threads != max || threads != core)) {
                    if (threads < core) {
                        threadPoolExecutor.setCorePoolSize(threads);
                        if (core == max) {
                            threadPoolExecutor.setMaximumPoolSize(threads);
                        }
                    } else {
                        threadPoolExecutor.setMaximumPoolSize(threads);
                        if (core == max) {
                            threadPoolExecutor.setCorePoolSize(threads);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error(COMMON_ERROR_USE_THREAD_POOL, "", "", t.getMessage(), t);
        }
    }

    @Override
    public ScheduledExecutorService getServiceExportExecutor() {
        synchronized (LOCK) {
            if (serviceExportExecutor == null) {
                int coreSize = getExportThreadNum();
                String applicationName = applicationModel.tryGetApplicationName();
                applicationName = StringUtils.isEmpty(applicationName) ? "app" : applicationName;
                serviceExportExecutor = Executors.newScheduledThreadPool(coreSize,
                    new NamedThreadFactory("Dubbo-" + applicationName + "-service-export", true));
            }
        }
        return serviceExportExecutor;
    }

    @Override
    public void shutdownServiceExportExecutor() {
        synchronized (LOCK) {
            if (serviceExportExecutor != null && !serviceExportExecutor.isShutdown()) {
                try {
                    serviceExportExecutor.shutdown();
                } catch (Throwable ignored) {
                    // ignored
                    logger.warn(COMMON_UNEXPECTED_EXECUTORS_SHUTDOWN, "", "", ignored.getMessage(), ignored);
                }
            }
            serviceExportExecutor = null;
        }
    }

    @Override
    public ExecutorService getServiceReferExecutor() {
        synchronized (LOCK) {
            if (serviceReferExecutor == null) {
                int coreSize = getReferThreadNum();
                String applicationName = applicationModel.tryGetApplicationName();
                applicationName = StringUtils.isEmpty(applicationName) ? "app" : applicationName;
                serviceReferExecutor = Executors.newFixedThreadPool(coreSize,
                    new NamedThreadFactory("Dubbo-" + applicationName + "-service-refer", true));
            }
        }
        return serviceReferExecutor;
    }

    @Override
    public void shutdownServiceReferExecutor() {
        synchronized (LOCK) {
            if (serviceReferExecutor != null && !serviceReferExecutor.isShutdown()) {
                try {
                    serviceReferExecutor.shutdown();
                } catch (Throwable ignored) {
                    logger.warn(COMMON_UNEXPECTED_EXECUTORS_SHUTDOWN, "", "", ignored.getMessage(), ignored);
                }
            }
            serviceReferExecutor = null;
        }
    }

    private Integer getExportThreadNum() {
        Integer threadNum = null;
        ApplicationModel applicationModel = ApplicationModel.ofNullable(this.applicationModel);
        for (ModuleModel moduleModel : applicationModel.getPubModuleModels()) {
            threadNum = getExportThreadNum(moduleModel);
            if (threadNum != null) {
                break;
            }
        }
        if (threadNum == null) {
            logger.info("Cannot get config `export-thread-num` from module config, using default: " + DEFAULT_EXPORT_THREAD_NUM);
            return DEFAULT_EXPORT_THREAD_NUM;
        }
        return threadNum;
    }

    private Integer getExportThreadNum(ModuleModel moduleModel) {
        ModuleConfig moduleConfig = moduleModel.getConfigManager().getModule().orElse(null);
        if (moduleConfig == null) {
            return null;
        }
        Integer threadNum = moduleConfig.getExportThreadNum();
        if (threadNum == null) {
            threadNum = moduleModel.getConfigManager().getProviders()
                .stream()
                .map(ProviderConfig::getExportThreadNum)
                .filter(k -> k != null && k > 0)
                .findAny().orElse(null);
        }
        return threadNum;
    }

    private Integer getReferThreadNum() {
        Integer threadNum = null;
        ApplicationModel applicationModel = ApplicationModel.ofNullable(this.applicationModel);
        for (ModuleModel moduleModel : applicationModel.getPubModuleModels()) {
            threadNum = getReferThreadNum(moduleModel);
            if (threadNum != null) {
                break;
            }
        }
        if (threadNum == null) {
            logger.info("Cannot get config `refer-thread-num` from module config, using default: " + DEFAULT_REFER_THREAD_NUM);
            return DEFAULT_REFER_THREAD_NUM;
        }
        return threadNum;
    }

    private Integer getReferThreadNum(ModuleModel moduleModel) {
        ModuleConfig moduleConfig = moduleModel.getConfigManager().getModule().orElse(null);
        if (moduleConfig == null) {
            return null;
        }
        Integer threadNum = moduleConfig.getReferThreadNum();
        if (threadNum == null) {
            threadNum = moduleModel.getConfigManager().getConsumers()
                .stream()
                .map(ConsumerConfig::getReferThreadNum)
                .filter(k -> k != null && k > 0)
                .findAny().orElse(null);
        }
        return threadNum;
    }

    @Override
    public void destroyAll() {
        logger.info("destroying application executor repository ..");
        shutdownServiceExportExecutor();
        shutdownServiceReferExecutor();

        data.values().forEach(executors -> {
            if (executors != null) {
                executors.values().forEach(executor -> {
                    if (executor != null && !executor.isShutdown()) {
                        try {
                            ExecutorUtil.shutdownNow(executor, 100);
                        } catch (Throwable ignored) {
                            // ignored
                            logger.warn(COMMON_UNEXPECTED_EXECUTORS_SHUTDOWN, "", "", ignored.getMessage(), ignored);
                        }
                    }
                });
            }
        });
        data.clear();
    }

    private void shutdownExecutorService(ExecutorService executorService, String name) {
        try {
            executorService.shutdownNow();
        } catch (Exception e) {
            String msg = "shutdown executor service [" + name + "] failed: ";
            logger.warn(COMMON_UNEXPECTED_EXECUTORS_SHUTDOWN, "", "", msg + e.getMessage(), e);
        }
    }

    @Override
    public void setExtensionAccessor(ExtensionAccessor extensionAccessor) {
        this.extensionAccessor = extensionAccessor;
    }

    @Override
    public ScheduledExecutorService nextScheduledExecutor() {
        return frameworkExecutorRepository.nextScheduledExecutor();
    }

    @Override
    public ExecutorService nextExecutorExecutor() {
        return frameworkExecutorRepository.nextExecutorExecutor();
    }

    @Override
    public ScheduledExecutorService getServiceDiscoveryAddressNotificationExecutor() {
        return frameworkExecutorRepository.getServiceDiscoveryAddressNotificationExecutor();
    }

    @Override
    public ScheduledExecutorService getMetadataRetryExecutor() {
        return frameworkExecutorRepository.getMetadataRetryExecutor();
    }

    @Override
    public ScheduledExecutorService getRegistryNotificationExecutor() {
        return frameworkExecutorRepository.getRegistryNotificationExecutor();
    }

    @Override
    public ExecutorService getSharedExecutor() {
        return frameworkExecutorRepository.getSharedExecutor();
    }

    @Override
    public ScheduledExecutorService getSharedScheduledExecutor() {
        return frameworkExecutorRepository.getSharedScheduledExecutor();
    }

    @Override
    public ExecutorService getPoolRouterExecutor() {
        return frameworkExecutorRepository.getPoolRouterExecutor();
    }

    @Override
    public ScheduledExecutorService getConnectivityScheduledExecutor() {
        return frameworkExecutorRepository.getConnectivityScheduledExecutor();
    }

    @Override
    public ScheduledExecutorService getCacheRefreshingScheduledExecutor() {
        return frameworkExecutorRepository.getCacheRefreshingScheduledExecutor();
    }

    @Override
    public ExecutorService getMappingRefreshingExecutor() {
        return frameworkExecutorRepository.getMappingRefreshingExecutor();
    }

    @Override
    public ExecutorSupport getExecutorSupport(URL url) {
        if (executorSupport == null) {
            executorSupport = new DefaultExecutorSupport(url);
        }
        return executorSupport;
    }
}
