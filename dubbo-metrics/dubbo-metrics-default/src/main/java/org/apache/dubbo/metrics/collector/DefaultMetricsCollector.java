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

package org.apache.dubbo.metrics.collector;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.metrics.collector.sample.MetricsCountSampleConfigurer;
import org.apache.dubbo.metrics.collector.sample.SimpleMetricsCountSampler;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RTEvent;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.event.SimpleMetricsEventMulticaster;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.Metric;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.ThreadPoolMetric;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.metrics.model.MetricsCategory.APPLICATION;
import static org.apache.dubbo.metrics.model.MetricsCategory.REQUESTS;
import static org.apache.dubbo.metrics.model.MetricsCategory.RT;
import static org.apache.dubbo.metrics.model.MetricsCategory.THREAD_POOL;
import static org.apache.dubbo.metrics.model.MetricsKey.APPLICATION_METRIC_INFO;

/**
 * Default implementation of {@link MetricsCollector}
 */
public class DefaultMetricsCollector extends SimpleMetricsCountSampler<Invocation,MetricsEvent.Type, MethodMetric>
    implements MetricsCollector {

    private AtomicBoolean collectEnabled = new AtomicBoolean(false);
    private final Set<ThreadPoolMetric> threadPoolMetricSet = new HashSet<ThreadPoolMetric>();
    private final SimpleMetricsEventMulticaster eventMulticaster;

    private String applicationName;

    public DefaultMetricsCollector() {
        this.eventMulticaster = SimpleMetricsEventMulticaster.getInstance();
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setCollectEnabled(Boolean collectEnabled) {
        this.collectEnabled.compareAndSet(isCollectEnabled(), collectEnabled);
    }

    public Boolean isCollectEnabled() {
        return collectEnabled.get();
    }

    public void increaseTotalRequests(Invocation invocation) {
        this.incOnEvent(invocation,MetricsEvent.Type.TOTAL);
    }

    public void increaseSucceedRequests(Invocation invocation) {
        this.incOnEvent(invocation,MetricsEvent.Type.SUCCEED);
    }

    public void increaseUnknownFailedRequests(Invocation invocation) {
        this.incOnEvent(invocation,MetricsEvent.Type.UNKNOWN_FAILED);
    }

    public void businessFailedRequests(Invocation invocation) {
        this.incOnEvent(invocation,MetricsEvent.Type.BUSINESS_FAILED);
    }

    public void timeoutRequests(Invocation invocation) {
        this.incOnEvent(invocation,MetricsEvent.Type.REQUEST_TIMEOUT);
    }

    public void limitRequests(Invocation invocation) {
        this.incOnEvent(invocation,MetricsEvent.Type.REQUEST_LIMIT);
    }

    public void increaseProcessingRequests(Invocation invocation) {
        this.incOnEvent(invocation,MetricsEvent.Type.PROCESSING);
    }

    public void decreaseProcessingRequests(Invocation invocation) {
        this.dec(invocation,MetricsEvent.Type.PROCESSING);
    }

    public void totalFailedRequests(Invocation invocation) {
        this.incOnEvent(invocation,MetricsEvent.Type.TOTAL_FAILED);
    }

    public void addApplicationInfo(String applicationName) {
        this.setApplicationName(applicationName);

        applicationSampler.inc(applicationName,MetricsEvent.Type.APPLICATION_INFO);
    }

    public void addThreadPool(FrameworkModel frameworkModel, String applicationName) {
        FrameworkExecutorRepository frameworkExecutorRepository =
            frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class);
        addThreadPoolExecutor(applicationName, "SharedExecutor", frameworkExecutorRepository.getSharedExecutor());
        addThreadPoolExecutor(applicationName, "MappingRefreshingExecutor",  frameworkExecutorRepository.getMappingRefreshingExecutor());
        addThreadPoolExecutor(applicationName, "PoolRouterExecutor", frameworkExecutorRepository.getPoolRouterExecutor());
    }

    private void addThreadPoolExecutor(String applicationName, String threadPoolName, ExecutorService executorService) {
        Optional<ExecutorService> executorOptional = Optional.ofNullable(executorService);
        if (executorOptional.isPresent() && executorOptional.get() instanceof ThreadPoolExecutor ) {
            threadPoolMetricSet.add(new ThreadPoolMetric(applicationName, threadPoolName,
                (ThreadPoolExecutor) executorOptional.get()));
        }
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        collectApplication(list);
        collectRequests(list);
        collectRT(list);
        collectThreadPool(list);
        return list;
    }

    private void collectThreadPool(List<MetricSample> list) {
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_CORE_SIZE, e.getTags(), THREAD_POOL, e::getCorePoolSize)));
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_LARGEST_SIZE, e.getTags(), THREAD_POOL, e::getLargestPoolSize)));
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_MAX_SIZE, e.getTags(), THREAD_POOL, e::getMaximumPoolSize)));
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_ACTIVE_SIZE, e.getTags(), THREAD_POOL, e::getActiveCount)));
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_THREAD_COUNT, e.getTags(), THREAD_POOL, e::getPoolSize)));
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(MetricsKey.THREAD_POOL_QUEUE_SIZE, e.getTags(), THREAD_POOL, e::getQueueSize)));
    }

    private void collectApplication(List<MetricSample> list) {
        applicationSampler.getCount(MetricsEvent.Type.APPLICATION_INFO).filter(e->!e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(APPLICATION_METRIC_INFO, k.getTags(),
                APPLICATION, v::get))));
    }

    private void collectRequests(List<MetricSample> list) {
        count(list, MetricsEvent.Type.TOTAL, MetricsKey.PROVIDER_METRIC_REQUESTS);
        count(list, MetricsEvent.Type.SUCCEED, MetricsKey.PROVIDER_METRIC_REQUESTS_SUCCEED);
        count(list, MetricsEvent.Type.UNKNOWN_FAILED, MetricsKey.PROVIDER_METRIC_REQUESTS_FAILED);
        count(list, MetricsEvent.Type.PROCESSING, MetricsKey.PROVIDER_METRIC_REQUESTS_PROCESSING);
        count(list, MetricsEvent.Type.BUSINESS_FAILED, MetricsKey.PROVIDER_METRIC_REQUEST_BUSINESS_FAILED);
        count(list, MetricsEvent.Type.REQUEST_TIMEOUT, MetricsKey.PROVIDER_METRIC_REQUESTS_TIMEOUT);
        count(list, MetricsEvent.Type.REQUEST_LIMIT, MetricsKey.PROVIDER_METRIC_REQUESTS_LIMIT);
        count(list, MetricsEvent.Type.TOTAL_FAILED, MetricsKey.PROVIDER_METRIC_REQUESTS_TOTAL_FAILED);
    }

    private <T extends Metric> void count(List<MetricSample> list, MetricsEvent.Type eventType, MetricsKey metricsKey) {
        getCount(eventType).filter(e->!e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(metricsKey, k.getTags(),
                REQUESTS, v::get))));
    }

    private void collectRT(List<MetricSample> list) {
        this.getLastRT().forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_LAST, k.getTags(), RT, v::get)));
        this.getMinRT().forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_MIN, k.getTags(), RT, v::get)));
        this.getMaxRT().forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_MAX, k.getTags(), RT, v::get)));

        this.getTotalRT().forEach((k, v) -> {
            list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_SUM, k.getTags(), RT, v::get));

            AtomicLong avg = this.getAvgRT().get(k);
            AtomicLong count = this.getRtCount().get(k);
            avg.set(v.get() / count.get());
            list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_AVG, k.getTags(), RT, avg::get));
        });
    }

    public void addListener(MetricsListener listener) {
        this.eventMulticaster.addListener(listener);
    }

    @Override
    protected void countConfigure(
        MetricsCountSampleConfigurer<Invocation, MetricsEvent.Type, MethodMetric> sampleConfigure) {
        sampleConfigure.configureMetrics(configure -> new MethodMetric(applicationName, configure.getSource()));
        sampleConfigure.configureEventHandler(configure -> eventMulticaster.publishEvent(new RequestEvent(configure.getMetric(), configure.getMetricName())));
    }

    @Override
    public void rtConfigure(
        MetricsCountSampleConfigurer<Invocation, MetricsEvent.Type, MethodMetric> sampleConfigure) {
        sampleConfigure.configureMetrics(configure -> new MethodMetric(applicationName, configure.getSource()));
        sampleConfigure.configureEventHandler(configure -> eventMulticaster.publishEvent(new RTEvent(configure.getMetric(), configure.getRt())));
    }

    public SimpleMetricsCountSampler<String,MetricsEvent.Type, ApplicationMetric> applicationSampler = new SimpleMetricsCountSampler<String,MetricsEvent.Type,ApplicationMetric>(){

        @Override
        protected void countConfigure(
            MetricsCountSampleConfigurer<String, MetricsEvent.Type, ApplicationMetric> sampleConfigure) {
            sampleConfigure.configureMetrics(configure -> new ApplicationMetric(sampleConfigure.getSource(),
                Version.getVersion()));
        }
    };
}
