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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.Disposable;
import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXECUTORS_SHUTDOWN;

public class FrameworkExecutorRepository implements Disposable {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(FrameworkExecutorRepository.class);

    private final ExecutorService sharedExecutor;
    private final ScheduledExecutorService sharedScheduledExecutor;

    private final Ring<ScheduledExecutorService> scheduledExecutors = new Ring<>();

    private final ScheduledExecutorService connectivityScheduledExecutor;

    private final ScheduledExecutorService cacheRefreshingScheduledExecutor;

    private final ExecutorService mappingRefreshingExecutor;

    public final Ring<ScheduledExecutorService> registryNotificationExecutorRing = new Ring<>();

    private final Ring<ScheduledExecutorService> serviceDiscoveryAddressNotificationExecutorRing = new Ring<>();

    private final ScheduledExecutorService metadataRetryExecutor;

    private final ExecutorService poolRouterExecutor;

    private final Ring<ExecutorService> executorServiceRing = new Ring<>();

    private final ExecutorService internalServiceExecutor;

    public FrameworkExecutorRepository() {
        sharedExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("Dubbo-framework-shared-handler", true));
        sharedScheduledExecutor = Executors.newScheduledThreadPool(8, new NamedThreadFactory("Dubbo-framework-shared-scheduler", true));

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < availableProcessors; i++) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory("Dubbo-framework-scheduler-" + i, true));
            scheduledExecutors.addItem(scheduler);

            executorServiceRing.addItem(new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024), new NamedInternalThreadFactory("Dubbo-framework-state-router-loop-" + i, true)
                , new ThreadPoolExecutor.AbortPolicy()));
        }

        connectivityScheduledExecutor = Executors.newScheduledThreadPool(availableProcessors, new NamedThreadFactory("Dubbo-framework-connectivity-scheduler", true));
        cacheRefreshingScheduledExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-framework-cache-refreshing-scheduler", true));
        mappingRefreshingExecutor = Executors.newFixedThreadPool(availableProcessors, new NamedThreadFactory("Dubbo-framework-mapping-refreshing-scheduler", true));
        poolRouterExecutor = new ThreadPoolExecutor(1, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024),
            new NamedInternalThreadFactory("Dubbo-framework-state-router-pool-router", true), new ThreadPoolExecutor.AbortPolicy());

        for (int i = 0; i < availableProcessors; i++) {
            ScheduledExecutorService serviceDiscoveryAddressNotificationExecutor =
                Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-framework-SD-address-refresh-" + i));
            ScheduledExecutorService registryNotificationExecutor =
                Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-framework-registry-notification-" + i));

            serviceDiscoveryAddressNotificationExecutorRing.addItem(serviceDiscoveryAddressNotificationExecutor);
            registryNotificationExecutorRing.addItem(registryNotificationExecutor);
        }

        metadataRetryExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-framework-metadata-retry"));
        internalServiceExecutor = new ThreadPoolExecutor(0, 100, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new NamedInternalThreadFactory("Dubbo-internal-service", true),
            new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Returns a scheduler from the scheduler list, call this method whenever you need a scheduler for a cron job.
     * If your cron cannot burden the possible schedule delay caused by sharing the same scheduler, please consider define a dedicated one.
     *
     * @return ScheduledExecutorService
     */
    public ScheduledExecutorService nextScheduledExecutor() {
        return scheduledExecutors.pollItem();
    }

    public ExecutorService nextExecutorExecutor() {
        return executorServiceRing.pollItem();
    }

    /**
     * Scheduled executor handle registry notification.
     *
     * @return ScheduledExecutorService
     */
    public ScheduledExecutorService getRegistryNotificationExecutor() {
        return registryNotificationExecutorRing.pollItem();
    }

    public ScheduledExecutorService getServiceDiscoveryAddressNotificationExecutor() {
        return serviceDiscoveryAddressNotificationExecutorRing.pollItem();
    }

    public ScheduledExecutorService getMetadataRetryExecutor() {
        return metadataRetryExecutor;
    }

    public ExecutorService getInternalServiceExecutor() {
        return internalServiceExecutor;
    }

    /**
     * Get the default shared thread pool.
     *
     * @return ExecutorService
     */
    public ExecutorService getSharedExecutor() {
        return sharedExecutor;
    }

    /**
     * Get the shared schedule executor
     *
     * @return ScheduledExecutorService
     */
    public ScheduledExecutorService getSharedScheduledExecutor() {
        return sharedScheduledExecutor;
    }


    public ExecutorService getPoolRouterExecutor() {
        return poolRouterExecutor;
    }

    /**
     * Scheduled executor handle connectivity check task
     *
     * @return ScheduledExecutorService
     */
    public ScheduledExecutorService getConnectivityScheduledExecutor() {
        return connectivityScheduledExecutor;
    }

    /**
     * Scheduler used to refresh file based caches from memory to disk.
     *
     * @return ScheduledExecutorService
     */
    public ScheduledExecutorService getCacheRefreshingScheduledExecutor() {
        return cacheRefreshingScheduledExecutor;
    }

    /**
     * Executor used to run async mapping tasks
     *
     * @return ExecutorService
     */
    public ExecutorService getMappingRefreshingExecutor() {
        return mappingRefreshingExecutor;
    }

    @Override
    public void destroy() {
        logger.info("destroying framework executor repository ..");
        shutdownExecutorService(poolRouterExecutor, "poolRouterExecutor");
        shutdownExecutorService(metadataRetryExecutor, "metadataRetryExecutor");
        shutdownExecutorService(internalServiceExecutor, "internalServiceExecutor");

        // scheduledExecutors
        shutdownExecutorServices(scheduledExecutors.listItems(), "scheduledExecutors");

        // executorServiceRing
        shutdownExecutorServices(executorServiceRing.listItems(), "executorServiceRing");

        // connectivityScheduledExecutor
        shutdownExecutorService(connectivityScheduledExecutor, "connectivityScheduledExecutor");
        shutdownExecutorService(cacheRefreshingScheduledExecutor, "cacheRefreshingScheduledExecutor");

        // shutdown share executor
        shutdownExecutorService(sharedExecutor, "sharedExecutor");
        shutdownExecutorService(sharedScheduledExecutor, "sharedScheduledExecutor");

        // serviceDiscoveryAddressNotificationExecutorRing
        shutdownExecutorServices(serviceDiscoveryAddressNotificationExecutorRing.listItems(),
            "serviceDiscoveryAddressNotificationExecutorRing");

        // registryNotificationExecutorRing
        shutdownExecutorServices(registryNotificationExecutorRing.listItems(),
            "registryNotificationExecutorRing");

        // mappingRefreshingExecutor
        shutdownExecutorService(mappingRefreshingExecutor,
            "mappingRefreshingExecutor");
    }

    private void shutdownExecutorServices(List<? extends ExecutorService> executorServices, String msg) {
        for (ExecutorService executorService : executorServices) {
            shutdownExecutorService(executorService, msg);
        }
    }

    private void shutdownExecutorService(ExecutorService executorService, String name) {
        try {
            executorService.shutdownNow();
        } catch (Exception e) {
            String msg = "shutdown executor service [" + name + "] failed: ";
            logger.warn(COMMON_UNEXPECTED_EXECUTORS_SHUTDOWN, "", "", msg + e.getMessage(), e);
        }
    }
}
