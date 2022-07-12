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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERNAL_EXECUTOR_SERVICE_COMPONENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;

/**
 * Consider implementing {@code Licycle} to enable executors shutdown when the process stops.
 */
public class DefaultExecutorRepository extends AbstractExecutorRepository {

    private static final Logger logger = LoggerFactory.getLogger(DefaultExecutorRepository.class);

    public static final String NAME = "default";

    private final ConcurrentMap<String, ConcurrentMap<Integer, ExecutorService>> data = new ConcurrentHashMap<>();

    private final FrameworkExecutorRepository frameworkExecutorRepository;

    public DefaultExecutorRepository(URL url) {
        super(url.getOrDefaultApplicationModel());
        this.frameworkExecutorRepository = url.getOrDefaultApplicationModel().getFrameworkModel()
            .getBeanFactory().getBean(FrameworkExecutorRepository.class);
    }

    public DefaultExecutorRepository(ApplicationModel applicationModel) {
        super(applicationModel);
        this.frameworkExecutorRepository = applicationModel.getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class);
    }

    /**
     * Get called when the server or client instance initiating.
     *
     * @param url
     * @return
     */
    @Override
    public synchronized ExecutorService createExecutorIfAbsent(URL url) {
        Map<Integer, ExecutorService> executors = data.computeIfAbsent(getExecutorKey(url), k -> new ConcurrentHashMap<>());
        // Consumer's executor is sharing globally, key=Integer.MAX_VALUE. Provider's executor is sharing by protocol.
        Integer portKey = CONSUMER_SIDE.equalsIgnoreCase(url.getParameter(SIDE_KEY)) ? Integer.MAX_VALUE : url.getPort();
        if (url.getParameter(THREAD_NAME_KEY) == null) {
            url = url.putAttribute(THREAD_NAME_KEY, "Dubbo-protocol-" + portKey);
        }
        URL finalUrl = url;
        ExecutorService executor = executors.computeIfAbsent(portKey, k -> createExecutor(finalUrl));
        // If executor has been shut down, create a new one
        if (executor.isShutdown() || executor.isTerminated()) {
            executors.remove(portKey);
            executor = createExecutor(url);
            executors.put(portKey, executor);
        }
        return executor;
    }

    /**
     * Return the executor key based on the type (internal or biz) of the current service.
     *
     * @param url
     * @return
     */
    private String getExecutorKey(URL url) {
        String executorKey = INTERNAL_EXECUTOR_SERVICE_COMPONENT_KEY;
        ServiceDescriptor serviceDescriptor = applicationModel.getInternalModule().getServiceRepository().lookupService(url.getServiceInterface());
        // if not found in internal service repository, then it's biz service defined by user.
        if (serviceDescriptor == null) {
            executorKey = EXECUTOR_SERVICE_COMPONENT_KEY;

        }
        return executorKey;
    }

    private ExecutorService createExecutor(URL url) {
        return (ExecutorService) url.getOrDefaultApplicationModel()
            .getExtensionLoader(ThreadPool.class)
            .getAdaptiveExtension()
            .getExecutor(url);
    }

    @Override
    public ExecutorService getExecutor(URL url) {
        Map<Integer, ExecutorService> executors = data.get(getExecutorKey(url));

        /*
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
            return frameworkExecutorRepository.getSharedExecutor();
        } else {
            return executor;
        }
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
                            logger.warn(ignored.getMessage(), ignored);
                        }
                    }
                });
            }
        });
        data.clear();
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
}
