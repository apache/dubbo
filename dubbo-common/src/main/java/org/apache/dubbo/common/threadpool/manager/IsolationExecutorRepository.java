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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.IsolationThreadPool;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.MD5Utils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;

public class IsolationExecutorRepository implements ExecutorRepository, ExtensionAccessorAware {

    private static final Logger logger = LoggerFactory.getLogger(IsolationExecutorRepository.class);

    private ExtensionAccessor extensionAccessor;

    private final FrameworkExecutorRepository frameworkExecutorRepository;

    private final Map<String, ExecutorService> isolationThreadpool = new ConcurrentHashMap<>();

    public IsolationExecutorRepository(ApplicationModel applicationModel) {
        this.frameworkExecutorRepository = applicationModel.getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class);
    }

    @Override
    public void setExtensionAccessor(ExtensionAccessor extensionAccessor) {
        this.extensionAccessor = extensionAccessor;
    }

    /**
     * init isolation thread pool
     */
    @Override
    public ExecutorService createExecutorIfAbsent(URL url) {
        String key = getIsolationThreadpoolKey(url);
        ExecutorService executor = isolationThreadpool.computeIfAbsent(key, k -> createExecutor(url));
        // if executor has been shut down, create a new one
        if (executor.isShutdown() || executor.isTerminated()) {
            isolationThreadpool.remove(key);
            executor = createExecutor(url);
            isolationThreadpool.put(key, executor);
        }
        return executor;
    }

    private ExecutorService createExecutor(URL url) {
        return (ExecutorService) extensionAccessor.getExtensionLoader(IsolationThreadPool.class).getAdaptiveExtension().getExecutor(url);
    }

    private String getIsolationThreadpoolKey(URL url) {
        return new MD5Utils().getMd5(url.getServiceKey() + url.getPort());
    }

    @Override
    public ExecutorService getExecutor(URL url) {
        String key = getIsolationThreadpoolKey(url);
        ExecutorService executor = isolationThreadpool.get(key);

        /*
         * It's guaranteed that this method is called after {@link #createExecutorIfAbsent(URL)}, so isolationThreadpool should already
         * have Executor instances generated and stored.
         */
        if (Objects.isNull(executor)) {
            logger.warn("No available executors, this is not expected, framework should call createExecutorIfAbsent first " +
                "before coming to here.");
            return null;
        }

        if (executor.isShutdown() || executor.isTerminated()) {
            isolationThreadpool.remove(key);
            // Does not re-create a shutdown executor, use SHARED_EXECUTOR for downgrade.
            executor = null;
            logger.info("Executor for " + url + " is shutdown.");
        }
        if (Objects.isNull(executor)) {
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
            logger.error(t.getMessage(), t);
        }
    }

    @Override
    public ScheduledExecutorService getServiceExportExecutor() {
        return null;
    }

    @Override
    public void shutdownServiceExportExecutor() {

    }

    @Override
    public ExecutorService getServiceReferExecutor() {
        return null;
    }

    @Override
    public void shutdownServiceReferExecutor() {

    }

    @Override
    public void destroyAll() {
        if (CollectionUtils.isNotEmptyMap(isolationThreadpool)) {
            for (ExecutorService executor : isolationThreadpool.values()) {
                if (executor != null && !executor.isShutdown()) {
                    try {
                        ExecutorUtil.shutdownNow(executor, 100);
                    } catch (Throwable throwable) {
                        // ignored
                        logger.warn(throwable.getMessage(), throwable);
                    }
                }
            }
            isolationThreadpool.clear();
        }
    }

    @Deprecated
    @Override
    public ScheduledExecutorService nextScheduledExecutor() {
        return null;
    }

    @Deprecated
    @Override
    public ExecutorService nextExecutorExecutor() {
        return null;
    }

    @Deprecated
    @Override
    public ScheduledExecutorService getServiceDiscoveryAddressNotificationExecutor() {
        return null;
    }

    @Deprecated
    @Override
    public ScheduledExecutorService getMetadataRetryExecutor() {
        return null;
    }

    @Deprecated
    @Override
    public ScheduledExecutorService getRegistryNotificationExecutor() {
        return null;
    }

    @Deprecated
    @Override
    public ExecutorService getSharedExecutor() {
        return null;
    }

    @Deprecated
    @Override
    public ScheduledExecutorService getSharedScheduledExecutor() {
        return null;
    }

    @Deprecated
    @Override
    public ExecutorService getPoolRouterExecutor() {
        return null;
    }

    @Deprecated
    @Override
    public ScheduledExecutorService getConnectivityScheduledExecutor() {
        return null;
    }

    @Deprecated
    @Override
    public ScheduledExecutorService getCacheRefreshingScheduledExecutor() {
        return null;
    }

    @Deprecated
    @Override
    public ExecutorService getMappingRefreshingExecutor() {
        return null;
    }
}
