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
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_EXPORT_THREAD_NUM;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_REFER_THREAD_NUM;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;

/**
 * abstract executor repository with common method
 */
public abstract class AbstractExecutorRepository implements ExecutorRepository {

    private static final Logger logger = LoggerFactory.getLogger(AbstractExecutorRepository.class);

    private volatile ScheduledExecutorService serviceExportExecutor;

    private volatile ExecutorService serviceReferExecutor;

    private final Object LOCK = new Object();

    public final ApplicationModel applicationModel;

    public AbstractExecutorRepository(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
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
                    logger.warn(ignored.getMessage(), ignored);
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
                    logger.warn(ignored.getMessage(), ignored);
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
}
