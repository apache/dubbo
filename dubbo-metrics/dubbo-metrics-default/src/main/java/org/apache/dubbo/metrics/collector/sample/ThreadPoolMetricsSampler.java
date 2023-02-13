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

import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.ThreadPoolMetric;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.dubbo.metrics.model.MetricsCategory.THREAD_POOL;

public class ThreadPoolMetricsSampler implements MetricsSampler {

    private DefaultMetricsCollector collector;
    private FrameworkExecutorRepository frameworkExecutorRepository;
    private Set<ThreadPoolMetric> threadPoolMetricSet = new HashSet<>();

    public ThreadPoolMetricsSampler(DefaultMetricsCollector collector) {
        this.collector = collector;
    }

    @Override
    public List<MetricSample> sample() {
        collect();
        List<MetricSample> metricSamples = new ArrayList<>();
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_CORE_SIZE, e.getTags(), THREAD_POOL, e::getCorePoolSize)));
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_LARGEST_SIZE, e.getTags(), THREAD_POOL, e::getLargestPoolSize)));
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_MAX_SIZE, e.getTags(), THREAD_POOL, e::getMaximumPoolSize)));
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_ACTIVE_SIZE, e.getTags(), THREAD_POOL, e::getActiveCount)));
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_THREAD_COUNT, e.getTags(), THREAD_POOL, e::getPoolSize)));
        threadPoolMetricSet.forEach(e -> metricSamples.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_QUEUE_SIZE, e.getTags(), THREAD_POOL, e::getQueueSize)));

        return metricSamples;
    }

    private void collect() {
        try{
            if (this.frameworkExecutorRepository == null) {
                this.frameworkExecutorRepository = collector.getApplicationModel().getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class);
            }
        }catch(Exception ex){}

        if (frameworkExecutorRepository != null) {
            addThread("SharedExecutor", frameworkExecutorRepository.getSharedExecutor());
            addThread("MappingRefreshingExecutor", frameworkExecutorRepository.getMappingRefreshingExecutor());
            addThread("PoolRouterExecutor", frameworkExecutorRepository.getPoolRouterExecutor());
        }
    }

    private void addThread(String threadPoolName, ExecutorService executorService) {
        Optional<ExecutorService> executorOptional = Optional.ofNullable(executorService);
        if (executorOptional.isPresent() && executorOptional.get() instanceof ThreadPoolExecutor) {
            threadPoolMetricSet.add(
                    new ThreadPoolMetric(collector.getApplicationName(), threadPoolName,
                            (ThreadPoolExecutor) executorOptional.get()));
        }

    }
}
