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
package org.apache.dubbo.metadata.report;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_SERVER_DISCONNECTED;

public class MetadataReportRetryTask {
    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());
    private final Object startLock = new Object();
    private volatile ScheduledFuture<?> future;
    private final Map<MetadataReport, Set<URL>> taskQueue = new ConcurrentHashMap<>();
    private final int mappingRetryInterval;
    private final ScheduledExecutorService scheduledExecutor;
    private BiFunction<MetadataReport, URL, Boolean> retryHandler;

    public MetadataReportRetryTask(ApplicationModel applicationModel) {
        this.mappingRetryInterval = applicationModel
                .getApplicationConfigManager()
                .getApplication()
                .map(ApplicationConfig::getMappingRetryInterval)
                .orElse(5000);
        this.scheduledExecutor = applicationModel
                .getBeanFactory()
                .getBean(FrameworkExecutorRepository.class)
                .getSharedScheduledExecutor();
    }

    public void setRetryHandler(BiFunction<MetadataReport, URL, Boolean> retryHandler) {
        this.retryHandler = retryHandler;
    }

    public void addTask(MetadataReport metadataReport, URL url) {
        taskQueue
                .computeIfAbsent(metadataReport, k -> new ConcurrentHashSet<>())
                .add(url);
    }

    /**
     * start retry task once
     * @return bool is retry task running
     */
    public boolean start() {
        if (future == null && !taskQueue.isEmpty()) {
            synchronized (startLock) {
                if (future == null && !taskQueue.isEmpty()) {
                    future = scheduledExecutor.scheduleWithFixedDelay(
                            this::retry, mappingRetryInterval, mappingRetryInterval, TimeUnit.MILLISECONDS);
                }
            }
        }
        return future != null;
    }

    /**
     * stop retry task
     */
    public void cancel() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    private void retry() {
        // stop task if there is no task
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
                    if (!retryHandler.apply(metadataReport, url)) {
                        throw new Exception("method doMap() return false");
                    }
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
