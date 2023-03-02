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
package org.apache.dubbo.metrics.collector.sample;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.store.DataStore;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.ThreadPoolMetric;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION;
import static org.apache.dubbo.metrics.model.MetricsCategory.THREAD_POOL;

public class ThreadPoolMetricsSampler implements MetricsSampler {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ThreadPoolMetricsSampler.class);

    private final DefaultMetricsCollector collector;
    private FrameworkExecutorRepository frameworkExecutorRepository;
    private DataStore dataStore;
    private final Map<String, ThreadPoolExecutor> sampleThreadPoolExecutor = new ConcurrentHashMap<>();

    public ThreadPoolMetricsSampler(DefaultMetricsCollector collector) {
        this.collector = collector;
        this.registryDefaultSampleThreadPoolExecutor();
    }

    public void addExecutors(String name, ExecutorService executorService) {
        Optional.ofNullable(executorService).filter(Objects::nonNull).filter(e -> e instanceof ThreadPoolExecutor)
            .map(e -> (ThreadPoolExecutor) e)
            .ifPresent(threadPoolExecutor -> sampleThreadPoolExecutor.put(name, threadPoolExecutor));
    }

    @Override
    public List<MetricSample> sample() {
        List<MetricSample> metricSamples = new ArrayList<>();

        sampleThreadPoolExecutor.forEach((name, executor) -> {
            metricSamples.addAll(createMetricsSample(name, executor));
        });

        return metricSamples;
    }

    private List<MetricSample> createMetricsSample(String name, ThreadPoolExecutor executor) {
        List<MetricSample> list = new ArrayList<>();
        ThreadPoolMetric poolMetrics = new ThreadPoolMetric(collector.getApplicationName(), name, executor);

        list.add(new GaugeMetricSample<>(MetricsKey.THREAD_POOL_CORE_SIZE, poolMetrics.getTags(), THREAD_POOL, poolMetrics, ThreadPoolMetric::getCorePoolSize));
        list.add(new GaugeMetricSample<>(MetricsKey.THREAD_POOL_LARGEST_SIZE, poolMetrics.getTags(), THREAD_POOL, poolMetrics, ThreadPoolMetric::getLargestPoolSize));
        list.add(new GaugeMetricSample<>(MetricsKey.THREAD_POOL_MAX_SIZE, poolMetrics.getTags(), THREAD_POOL, poolMetrics, ThreadPoolMetric::getMaximumPoolSize));
        list.add(new GaugeMetricSample<>(MetricsKey.THREAD_POOL_ACTIVE_SIZE, poolMetrics.getTags(), THREAD_POOL, poolMetrics, ThreadPoolMetric::getActiveCount));
        list.add(new GaugeMetricSample<>(MetricsKey.THREAD_POOL_THREAD_COUNT, poolMetrics.getTags(), THREAD_POOL, poolMetrics, ThreadPoolMetric::getPoolSize));
        list.add(new GaugeMetricSample<>(MetricsKey.THREAD_POOL_QUEUE_SIZE, poolMetrics.getTags(), THREAD_POOL, poolMetrics, ThreadPoolMetric::getQueueSize));

        return list;
    }

    private void registryDefaultSampleThreadPoolExecutor() {
        ApplicationModel applicationModel = collector.getApplicationModel();
        if (applicationModel == null) {
            return;
        }
        try {
            if (this.frameworkExecutorRepository == null) {
                this.frameworkExecutorRepository = collector.getApplicationModel().getBeanFactory()
                    .getBean(FrameworkExecutorRepository.class);
            }
        } catch (Exception ex) {
            logger.warn(COMMON_METRICS_COLLECTOR_EXCEPTION, "", "", "ThreadPoolMetricsSampler! frameworkExecutorRepository non-init");
        }
        if (this.dataStore == null) {
            this.dataStore = collector.getApplicationModel().getExtensionLoader(DataStore.class).getDefaultExtension();
        }
        if (dataStore != null) {
            Map<String, Object> executors = dataStore.get(EXECUTOR_SERVICE_COMPONENT_KEY);
            for (Map.Entry<String, Object> entry : executors.entrySet()) {
                ExecutorService executor = (ExecutorService) entry.getValue();
                if (executor instanceof ThreadPoolExecutor) {
                    this.addExecutors(entry.getKey(), executor);
                }
            }
        }
        if (this.frameworkExecutorRepository != null) {
            this.addExecutors("sharedExecutor", frameworkExecutorRepository.getSharedExecutor());
            this.addExecutors("mappingRefreshingExecutor", frameworkExecutorRepository.getMappingRefreshingExecutor());
            this.addExecutors("poolRouterExecutor", frameworkExecutorRepository.getPoolRouterExecutor());
        }
    }

}
