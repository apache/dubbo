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

import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.metrics.collector.stat.MetricsStatComposite;
import org.apache.dubbo.metrics.collector.stat.MetricsStatHandler;
import org.apache.dubbo.metrics.event.EmptyEvent;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.SimpleMetricsEventMulticaster;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.ThreadPoolMetric;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.Invocation;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.apache.dubbo.metrics.model.MetricsCategory.THREAD_POOL;
import static org.apache.dubbo.metrics.model.MetricsCategory.APPLICATION;
import static org.apache.dubbo.metrics.model.MetricsCategory.REQUESTS;
import static org.apache.dubbo.metrics.model.MetricsCategory.RT;


/**
 * Default implementation of {@link MetricsCollector}
 */
public class DefaultMetricsCollector implements MetricsCollector {

    private AtomicBoolean collectEnabled = new AtomicBoolean(false);
    private final Set<ThreadPoolMetric> threadPoolMetricSet = new HashSet<ThreadPoolMetric>();
    private final MetricsStatComposite stats;
    private final SimpleMetricsEventMulticaster eventMulticaster;

    public DefaultMetricsCollector() {
        this.stats = new MetricsStatComposite(this);
        this.eventMulticaster = SimpleMetricsEventMulticaster.getInstance();
    }

    public void setCollectEnabled(Boolean collectEnabled) {
        this.collectEnabled.compareAndSet(isCollectEnabled(), collectEnabled);
    }

    public Boolean isCollectEnabled() {
        return collectEnabled.get();
    }

    public void increaseTotalRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName, MetricsEvent.Type.TOTAL, invocation);
    }

    public void increaseSucceedRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName, MetricsEvent.Type.SUCCEED, invocation);
    }

    public void increaseUnknownFailedRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName, MetricsEvent.Type.UNKNOWN_FAILED, invocation);
    }

    public void businessFailedRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName, MetricsEvent.Type.BUSINESS_FAILED, invocation);
    }

    public void timeoutRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName, MetricsEvent.Type.REQUEST_TIMEOUT, invocation);
    }

    public void limitRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName, MetricsEvent.Type.REQUEST_LIMIT, invocation);
    }

    public void increaseProcessingRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName, MetricsEvent.Type.PROCESSING, invocation);
    }

    public void decreaseProcessingRequests(String applicationName, Invocation invocation) {
        decreaseAndPublishEvent(applicationName, MetricsEvent.Type.PROCESSING, invocation);
    }

    public void totalFailedRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName, MetricsEvent.Type.TOTAL_FAILED, invocation);
    }

    private void increaseAndPublishEvent(String applicationName, MetricsEvent.Type type, Invocation invocation) {
        this.eventMulticaster.publishEvent(doExecute(type, statHandler -> statHandler.increase(applicationName, invocation)));
    }

    private void decreaseAndPublishEvent(String applicationName, MetricsEvent.Type type, Invocation invocation) {
        this.eventMulticaster.publishEvent(doExecute(type, statHandler -> statHandler.decrease(applicationName, invocation)));
    }

    public void addRT(String applicationName, Invocation invocation, Long responseTime) {
        this.eventMulticaster.publishEvent(stats.addRtAndRetrieveEvent(applicationName, invocation, responseTime));
    }

    public void addApplicationInfo(String applicationName, String version) {
        doExecute(MetricsEvent.Type.APPLICATION_INFO, statHandler -> statHandler.addApplication(applicationName, version));
    }

    public void addThreadPool(FrameworkModel frameworkModel, String applicationName) {
        FrameworkExecutorRepository frameworkExecutorRepository =
            frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class);
        addThreadPoolExecutor(applicationName, "SharedExecutor", frameworkExecutorRepository.getSharedExecutor());
        addThreadPoolExecutor(applicationName, "MappingRefreshingExecutor", frameworkExecutorRepository.getMappingRefreshingExecutor());
        addThreadPoolExecutor(applicationName, "PoolRouterExecutor", frameworkExecutorRepository.getPoolRouterExecutor());
    }

    private void addThreadPoolExecutor(String applicationName, String threadPoolName, ExecutorService executorService) {
        Optional<ExecutorService> executorOptional = Optional.ofNullable(executorService);
        if (executorOptional.isPresent() && executorOptional.get() instanceof ThreadPoolExecutor) {
            threadPoolMetricSet.add(new ThreadPoolMetric(applicationName, threadPoolName,
                (ThreadPoolExecutor) executorOptional.get()));
        }
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        collectRequests(list);
        collectRT(list);
        collectApplication(list);
        collectThreadPool(list);
        return list;
    }

    private void collectThreadPool(List<MetricSample> list) {
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(e.getTags(), THREAD_POOL, e::getCorePoolSize, MetricsKey.THREAD_POOL_CORE_SIZE.getName(), MetricsKey.THREAD_POOL_CORE_SIZE.getDescription())));
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(e.getTags(), THREAD_POOL, e::getLargestPoolSize, MetricsKey.THREAD_POOL_LARGEST_SIZE.getName(), MetricsKey.THREAD_POOL_LARGEST_SIZE.getDescription())));
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(e.getTags(), THREAD_POOL, e::getMaximumPoolSize, MetricsKey.THREAD_POOL_MAX_SIZE.getName(), MetricsKey.THREAD_POOL_MAX_SIZE.getDescription())));
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(e.getTags(), THREAD_POOL, e::getActiveCount, MetricsKey.THREAD_POOL_ACTIVE_SIZE.getName(), MetricsKey.THREAD_POOL_ACTIVE_SIZE.getDescription())));
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(e.getTags(), THREAD_POOL, e::getPoolSize, MetricsKey.THREAD_POOL_THREAD_COUNT.getName(), MetricsKey.THREAD_POOL_THREAD_COUNT.getDescription())));
        threadPoolMetricSet.forEach(e -> list.add(new GaugeMetricSample(e.getTags(), THREAD_POOL, e::getQueueSize, MetricsKey.THREAD_POOL_QUEUE_SIZE.getName(), MetricsKey.THREAD_POOL_QUEUE_SIZE.getDescription())));
    }

    private void collectApplication(List<MetricSample> list) {
        doCollect(MetricsEvent.Type.APPLICATION_INFO, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(k.getTags(),
                APPLICATION, v::get, MetricsKey.APPLICATION_METRIC_INFO.getName(), MetricsKey.APPLICATION_METRIC_INFO.getDescription()))));
    }

    private void collectRequests(List<MetricSample> list) {
        doCollect(MetricsEvent.Type.TOTAL, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) ->
                list.add(new GaugeMetricSample(k.getTags(), REQUESTS, v::get,
                     MetricsKey.METRIC_REQUESTS.getNameByType(k.getSide()),
                    MetricsKey.METRIC_REQUESTS.getDescription()))));

        doCollect(MetricsEvent.Type.SUCCEED, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) ->
                list.add(new GaugeMetricSample(k.getTags(), REQUESTS, v::get,
                    MetricsKey.METRIC_REQUESTS_SUCCEED.getNameByType(k.getSide()),
                    MetricsKey.METRIC_REQUESTS_SUCCEED.getDescription()))));

        doCollect(MetricsEvent.Type.UNKNOWN_FAILED, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) ->
                list.add(new GaugeMetricSample(k.getTags(), REQUESTS, v::get,
                     MetricsKey.METRIC_REQUESTS_FAILED.getNameByType(k.getSide()),
                    MetricsKey.METRIC_REQUESTS_FAILED.getDescription()))));

        doCollect(MetricsEvent.Type.PROCESSING, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) ->
                list.add(new GaugeMetricSample(k.getTags(), REQUESTS, v::get,
                    MetricsKey.METRIC_REQUESTS_PROCESSING.getNameByType(k.getSide()),
                    MetricsKey.METRIC_REQUESTS_PROCESSING.getDescription()))));

        doCollect(MetricsEvent.Type.BUSINESS_FAILED, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) ->
                list.add(new GaugeMetricSample(k.getTags(), REQUESTS, v::get,
                    MetricsKey.METRIC_REQUEST_BUSINESS_FAILED.getNameByType(k.getSide()),
                    MetricsKey.METRIC_REQUEST_BUSINESS_FAILED.getDescription()))));

        doCollect(MetricsEvent.Type.REQUEST_TIMEOUT, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) ->
                list.add(new GaugeMetricSample(k.getTags(), REQUESTS, v::get,
                    MetricsKey.METRIC_REQUESTS_TIMEOUT.getNameByType(k.getSide()),
                    MetricsKey.METRIC_REQUESTS_TIMEOUT.getDescription()))));

        doCollect(MetricsEvent.Type.REQUEST_LIMIT, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) ->
                list.add(new GaugeMetricSample(k.getTags(), REQUESTS, v::get,
                    MetricsKey.METRIC_REQUESTS_LIMIT.getNameByType(k.getSide()),
                    MetricsKey.METRIC_REQUESTS_LIMIT.getDescription()))));

        doCollect(MetricsEvent.Type.TOTAL_FAILED, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) ->
                list.add(new GaugeMetricSample(k.getTags(), REQUESTS, v::get,
                    MetricsKey.METRIC_REQUESTS_TOTAL_FAILED.getNameByType(k.getSide()),
                    MetricsKey.METRIC_REQUESTS_TOTAL_FAILED.getDescription()))));

    }

    


    private void collectRT(List<MetricSample> list) {
        this.stats.getLastRT().forEach((k, v) ->
            list.add(new GaugeMetricSample(k.getTags(), RT, v::get,
                MetricsKey.METRIC_RT_LAST.getNameByType(k.getSide()), MetricsKey.METRIC_RT_LAST.getDescription())));
        this.stats.getMinRT().forEach((k, v) ->
            list.add(new GaugeMetricSample(k.getTags(), RT, v::get,
                MetricsKey.METRIC_RT_MIN.getNameByType(k.getSide()),
                MetricsKey.METRIC_RT_MIN.getDescription())));
        this.stats.getMaxRT().forEach((k, v) ->
            list.add(new GaugeMetricSample(k.getTags(), RT, v::get,
                MetricsKey.METRIC_RT_MAX.getNameByType(k.getSide()), MetricsKey.METRIC_RT_MAX.getDescription())));

        this.stats.getTotalRT().forEach((k, v) -> {
            list.add(new GaugeMetricSample(k.getTags(), RT, v::get,
                MetricsKey.METRIC_RT_SUM.getNameByType(k.getSide()),
                MetricsKey.METRIC_RT_SUM.getDescription()));

            AtomicLong avg = this.stats.getAvgRT().get(k);
            AtomicLong count = this.stats.getRtCount().get(k);
            avg.set(v.get() / count.get());
            list.add(new GaugeMetricSample(k.getTags(), RT, avg::get,
                MetricsKey.METRIC_RT_AVG.getNameByType(k.getSide()), MetricsKey.METRIC_RT_AVG.getDescription()));
        });
    }


    private <T> Optional<T> doCollect(MetricsEvent.Type metricsEventType, Function<MetricsStatHandler, T> statExecutor) {
        if (isCollectEnabled()) {
            MetricsStatHandler handler = stats.getHandler(metricsEventType);
            T result = statExecutor.apply(handler);
            return Optional.ofNullable(result);
        }
        return Optional.empty();
    }

    private MetricsEvent doExecute(MetricsEvent.Type metricsEventType,
                                   Function<MetricsStatHandler, MetricsEvent> statExecutor) {
        if (isCollectEnabled()) {
            MetricsStatHandler handler = stats.getHandler(metricsEventType);
            return statExecutor.apply(handler);
        }
        return EmptyEvent.instance();
    }

    public void addListener(MetricsListener listener) {
        this.eventMulticaster.addListener(listener);
    }
}
