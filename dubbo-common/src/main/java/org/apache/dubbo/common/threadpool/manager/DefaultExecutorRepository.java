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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ProviderConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_ASYNC_THREAD_NUM;
import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.rpc.model.ApplicationModel.getConfigManager;

/**
 * Consider implementing {@code Licycle} to enable executors shutdown when the process stops.
 */
public class DefaultExecutorRepository implements ExecutorRepository {
    private static final Logger logger = LoggerFactory.getLogger(DefaultExecutorRepository.class);

    private int DEFAULT_SCHEDULER_SIZE = Runtime.getRuntime().availableProcessors();

    private final ExecutorService SHARED_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("DubboSharedHandler", true));

    private Ring<ScheduledExecutorService> scheduledExecutors = new Ring<>();

    private volatile ScheduledExecutorService exportReferExecutor;

    public ScheduledExecutorService registryNotificationExecutor;

    private ScheduledExecutorService reconnectScheduledExecutor;

    private ScheduledExecutorService serviceDiscoveryAddressNotificationExecutor;

    private ScheduledExecutorService metadataRetryExecutor;

    private ConcurrentMap<String, ConcurrentMap<Integer, ExecutorService>> data = new ConcurrentHashMap<>();

    private ExecutorService poolRouterExecutor;

    private static Ring<ExecutorService> executorServiceRing = new Ring<ExecutorService>();

    private static final Object LOCK = new Object();

    public DefaultExecutorRepository() {
        for (int i = 0; i < DEFAULT_SCHEDULER_SIZE; i++) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory("Dubbo-framework-scheduler"));
            scheduledExecutors.addItem(scheduler);

            executorServiceRing.addItem(new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), new NamedInternalThreadFactory("Dubbo-state-router-loop", true)
                , new ThreadPoolExecutor.AbortPolicy()));
        }
//
//        reconnectScheduledExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-reconnect-scheduler"));
        poolRouterExecutor = new ThreadPoolExecutor(1, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024),
            new NamedInternalThreadFactory("Dubbo-state-router-pool-router", true), new ThreadPoolExecutor.AbortPolicy());
        serviceDiscoveryAddressNotificationExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-SD-address-refresh"));
        registryNotificationExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-registry-notification"));
        metadataRetryExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-metadata-retry"));
    }

    /**
     * Get called when the server or client instance initiating.
     *
     * @param url
     * @return
     */
    public synchronized ExecutorService createExecutorIfAbsent(URL url) {
        Map<Integer, ExecutorService> executors = data.computeIfAbsent(EXECUTOR_SERVICE_COMPONENT_KEY, k -> new ConcurrentHashMap<>());
        // Consumer's executor is sharing globally, key=Integer.MAX_VALUE. Provider's executor is sharing by protocol.
        Integer portKey = CONSUMER_SIDE.equalsIgnoreCase(url.getParameter(SIDE_KEY)) ? Integer.MAX_VALUE : url.getPort();
        ExecutorService executor = executors.computeIfAbsent(portKey, k -> createExecutor(url));
        // If executor has been shut down, create a new one
        if (executor.isShutdown() || executor.isTerminated()) {
            executors.remove(portKey);
            executor = createExecutor(url);
            executors.put(portKey, executor);
        }
        return executor;
    }

    public ExecutorService getExecutor(URL url) {
        Map<Integer, ExecutorService> executors = data.get(EXECUTOR_SERVICE_COMPONENT_KEY);

        /**
         * It's guaranteed that this method is called after {@link #createExecutorIfAbsent(URL)}, so data should already
         * have Executor instances generated and stored.
         */
        if (executors == null) {
            logger.warn("No available executors, this is not expected, framework should call createExecutorIfAbsent first " +
                "before coming to here.");
            return null;
        }

        // Consumer's executor is sharing globally, key=Integer.MAX_VALUE. Provider's executor is sharing by protocol.
        Integer portKey = CONSUMER_SIDE.equalsIgnoreCase(url.getParameter(SIDE_KEY)) ? Integer.MAX_VALUE : url.getPort();
        ExecutorService executor = executors.get(portKey);
        if (executor != null && (executor.isShutdown() || executor.isTerminated())) {
            executors.remove(portKey);
            // Does not re-create a shutdown executor, use SHARED_EXECUTOR for downgrade.
            executor = null;
            logger.info("Executor for " + url + " is shutdown.");
        }
        if (executor == null) {
            return SHARED_EXECUTOR;
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
            logger.error(t.getMessage(), t);
        }
    }

    @Override
    public ScheduledExecutorService nextScheduledExecutor() {
        return scheduledExecutors.pollItem();
    }

    @Override
    public ExecutorService nextExecutorExecutor() {
        return executorServiceRing.pollItem();
    }

    @Override
    public ScheduledExecutorService getExportReferExecutor() {
        if (exportReferExecutor == null) {
            synchronized (LOCK) {
                if (exportReferExecutor == null) {
                    int coreSize = getExportReferThreadNum();
                    exportReferExecutor = Executors.newScheduledThreadPool(coreSize,
                        new NamedThreadFactory("Dubbo-export-refer", true));
                }
            }
        }

        return exportReferExecutor;
    }

    public void shutdownExportReferExecutor() {
        synchronized (LOCK) {
            if (exportReferExecutor != null && !exportReferExecutor.isShutdown()) {
                exportReferExecutor.shutdown();
            }

            exportReferExecutor = null;
        }
    }

    private Integer getExportReferThreadNum() {
        Stream<Integer> provider = getConfigManager().getProviders()
            .stream()
            .map(ProviderConfig::getAsyncThreadNum);

        Stream<Integer> consumer = getConfigManager().getConsumers()
            .stream()
            .map(ConsumerConfig::getAsyncThreadNum);

        List<Integer> threadNums = Stream.concat(provider, consumer)
            .filter(k -> k != null && k > 0)
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(threadNums)) {
            logger.info("Cannot get config `async-thread-num` for export-refer thread, using default: " + DEFAULT_ASYNC_THREAD_NUM);
            return DEFAULT_ASYNC_THREAD_NUM;
        } else if (threadNums.size() > 1) {
            logger.info("Detect multiple config `async-thread-num` for export-refer thread, using: " + threadNums.get(0));
        }

        return threadNums.get(0);
    }

    @Override
    public ScheduledExecutorService getRegistryNotificationExecutor() {
        return registryNotificationExecutor;
    }

    public ScheduledExecutorService getServiceDiscoveryAddressNotificationExecutor() {
        return serviceDiscoveryAddressNotificationExecutor;
    }

    @Override
    public ScheduledExecutorService getMetadataRetryExecutor() {
        return metadataRetryExecutor;
    }

    @Override
    public ExecutorService getSharedExecutor() {
        return SHARED_EXECUTOR;
    }

    private ExecutorService createExecutor(URL url) {
        return (ExecutorService) ExtensionLoader.getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);
    }

    @Override
    public ExecutorService getPoolRouterExecutor() {
        return poolRouterExecutor;
    }

    @Override
    public void destroyAll() {
        poolRouterExecutor.shutdown();
        serviceDiscoveryAddressNotificationExecutor.shutdown();
        registryNotificationExecutor.shutdown();
        metadataRetryExecutor.shutdown();

        shutdownExportReferExecutor();

        data.values().forEach(executors -> {
            if (executors != null) {
                executors.values().forEach(executor -> {
                    if (executor != null && !executor.isShutdown()) {
                        ExecutorUtil.shutdownNow(executor, 100);
                    }
                });
            }
        });

        // TODO shutdown all executor services
//        for (ScheduledExecutorService executorService : scheduledExecutors.listItems()) {
//            executorService.shutdown();
//        }
//
//        for (ExecutorService executorService : executorServiceRing.listItems()) {
//            executorService.shutdown();
//        }
    }
}
