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
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.ThreadPoolMetric;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION;
import static org.apache.dubbo.metrics.model.MetricsCategory.THREAD_POOL;

public class ThreadPoolMetricsSampler implements MetricsSampler {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ThreadPoolMetricsSampler.class);

    private DefaultMetricsCollector collector;
    private FrameworkExecutorRepository frameworkExecutorRepository;
    private Map<String, ThreadPoolExecutor> sampleThreadPoolExecutor = new ConcurrentHashMap<>();

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

        sampleThreadPoolExecutor.forEach((name, executor)->{
            metricSamples.addAll(createMetricsSample(name,executor));
        });

        return metricSamples;
    }

    private List<MetricSample> createMetricsSample(String name,ThreadPoolExecutor executor) {
        List<MetricSample> list = new ArrayList<>();
        ThreadPoolMetric poolMetrics = new ThreadPoolMetric(collector.getApplicationName(), name, executor);

        list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_CORE_SIZE, poolMetrics.getTags(), THREAD_POOL, poolMetrics::getCorePoolSize));
        list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_LARGEST_SIZE, poolMetrics.getTags(), THREAD_POOL, poolMetrics::getLargestPoolSize));
        list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_MAX_SIZE, poolMetrics.getTags(), THREAD_POOL, poolMetrics::getMaximumPoolSize));
        list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_ACTIVE_SIZE, poolMetrics.getTags(), THREAD_POOL, poolMetrics::getActiveCount));
        list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_THREAD_COUNT, poolMetrics.getTags(), THREAD_POOL, poolMetrics::getPoolSize));
        list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_QUEUE_SIZE, poolMetrics.getTags(), THREAD_POOL, poolMetrics::getQueueSize));

        return list;
    }

    private void registryDefaultSampleThreadPoolExecutor() {
        try {
            if (this.frameworkExecutorRepository == null) {
                this.frameworkExecutorRepository = collector.getApplicationModel()
                    .getFrameworkModel().getBeanFactory()
                    .getBean(FrameworkExecutorRepository.class);
            }
        } catch (Exception ex) {
            logger.warn(COMMON_METRICS_COLLECTOR_EXCEPTION, "", "", "ThreadPoolMetricsSampler! frameworkExecutorRepository non-init");
        }
        if (this.frameworkExecutorRepository != null) {
            this.addExecutors("sharedExecutor", frameworkExecutorRepository.getSharedExecutor());
            this.addExecutors("mappingRefreshingExecutor", frameworkExecutorRepository.getMappingRefreshingExecutor());
            this.addExecutors("poolRouterExecutor", frameworkExecutorRepository.getPoolRouterExecutor());
        }
    }

}
