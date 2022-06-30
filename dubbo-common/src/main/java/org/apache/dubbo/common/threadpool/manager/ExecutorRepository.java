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
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

@SPI(value = "default", scope = ExtensionScope.APPLICATION)
public interface ExecutorRepository {

    /**
     * Called by both Client and Server. TODO, consider separate these two parts.
     * When the Client or Server starts for the first time, generate a new threadpool according to the parameters specified.
     *
     * @param url
     * @return
     */
    ExecutorService createExecutorIfAbsent(URL url);

    /**
     * Be careful,The semantics of this method are getOrDefaultExecutor
     *
     * @param url
     * @return
     */
    ExecutorService getExecutor(URL url);


    /**
     * Modify some of the threadpool's properties according to the url, for example, coreSize, maxSize, ...
     *
     * @param url
     * @param executor
     */
    void updateThreadpool(URL url, ExecutorService executor);

    ScheduledExecutorService getServiceExportExecutor();

    /**
     * The executor only used in bootstrap currently, we should call this method to release the resource
     * after the async export is done.
     */
    void shutdownServiceExportExecutor();

    ExecutorService getServiceReferExecutor();

    /**
     * The executor only used in bootstrap currently, we should call this method to release the resource
     * after the async refer is done.
     */
    void shutdownServiceReferExecutor();

    /**
     * Destroy all executors that are not in shutdown state
     */
    void destroyAll();

    /**
     * Returns a scheduler from the scheduler list, call this method whenever you need a scheduler for a cron job.
     * If your cron cannot burden the possible schedule delay caused by sharing the same scheduler, please consider define a dedicate one.
     *
     * @return ScheduledExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#nextScheduledExecutor()} instead
     */
    @Deprecated
    ScheduledExecutorService nextScheduledExecutor();

    /**
     * @return ExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#nextExecutorExecutor()} instead
     */
    @Deprecated
    ExecutorService nextExecutorExecutor();

    /**
     * @return ScheduledExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#getServiceDiscoveryAddressNotificationExecutor()} instead
     */
    @Deprecated
    ScheduledExecutorService getServiceDiscoveryAddressNotificationExecutor();

    /**
     * @return ScheduledExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#getMetadataRetryExecutor()} instead
     */
    @Deprecated
    ScheduledExecutorService getMetadataRetryExecutor();

    /**
     * Scheduled executor handle registry notification.
     *
     * @return ScheduledExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#getRegistryNotificationExecutor()} instead
     */
    @Deprecated
    ScheduledExecutorService getRegistryNotificationExecutor();

    /**
     * Get the default shared threadpool.
     *
     * @return ScheduledExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#getSharedExecutor()} instead
     */
    @Deprecated
    ExecutorService getSharedExecutor();

    /**
     * Get the shared schedule executor
     *
     * @return ScheduledExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#getSharedScheduledExecutor()} instead
     */
    @Deprecated
    ScheduledExecutorService getSharedScheduledExecutor();

    /**
     * @return ExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#getPoolRouterExecutor()} instead
     */
    @Deprecated
    ExecutorService getPoolRouterExecutor();

    /**
     * Scheduled executor handle connectivity check task
     *
     * @return ScheduledExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#getConnectivityScheduledExecutor()} instead
     */
    @Deprecated
    ScheduledExecutorService getConnectivityScheduledExecutor();

    /**
     * Scheduler used to refresh file based caches from memory to disk.
     *
     * @return ScheduledExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#getCacheRefreshingScheduledExecutor()} instead
     */
    @Deprecated
    ScheduledExecutorService getCacheRefreshingScheduledExecutor();

    /**
     * Executor used to run async mapping tasks
     *
     * @return ExecutorService
     * @deprecated use {@link FrameworkExecutorRepository#getMappingRefreshingExecutor()} instead
     */
    @Deprecated
    ExecutorService getMappingRefreshingExecutor();
}
