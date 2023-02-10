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

import org.apache.dubbo.metrics.collector.stat.MetricsStatComposite;
import org.apache.dubbo.metrics.collector.stat.MetricsStatHandler;
import org.apache.dubbo.metrics.event.EmptyEvent;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.SimpleMetricsEventMulticaster;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.Invocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.apache.dubbo.metrics.model.MetricsCategory.APPLICATION;
import static org.apache.dubbo.metrics.model.MetricsCategory.REQUESTS;
import static org.apache.dubbo.metrics.model.MetricsCategory.RT;


/**
 * Default implementation of {@link MetricsCollector}
 */
public class DefaultMetricsCollector implements MetricsCollector {

    private AtomicBoolean collectEnabled = new AtomicBoolean(false);
    private final MetricsStatComposite stats;
    private final SimpleMetricsEventMulticaster eventMulticaster;

    public DefaultMetricsCollector() {
        this.stats = new MetricsStatComposite(this);
        this.eventMulticaster = new SimpleMetricsEventMulticaster();
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
        increaseAndPublishEvent(applicationName,MetricsEvent.Type.REQUEST_TIMEOUT, invocation);
    }

    public void limitRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName,MetricsEvent.Type.REQUEST_LIMIT, invocation);
    }

    public void increaseProcessingRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName,MetricsEvent.Type.PROCESSING, invocation);
    }

    public void decreaseProcessingRequests(String applicationName, Invocation invocation) {
        decreaseAndPublishEvent(applicationName,MetricsEvent.Type.PROCESSING, invocation);
    }

    public void totalFailedRequests(String applicationName, Invocation invocation) {
        increaseAndPublishEvent(applicationName,MetricsEvent.Type.TOTAL_FAILED, invocation);
    }

    private void increaseAndPublishEvent(String applicationName, MetricsEvent.Type total, Invocation invocation) {
        this.eventMulticaster.publishEvent(doExecute(total, statHandler -> statHandler.increase(applicationName,invocation)));
    }

    private void decreaseAndPublishEvent(String applicationName, MetricsEvent.Type type, Invocation invocation) {
        this.eventMulticaster.publishEvent(doExecute(type, statHandler -> statHandler.decrease(applicationName,invocation)));
    }

    public void addRT(String applicationName,Invocation invocation, Long responseTime) {
        this.eventMulticaster.publishEvent(stats.addRtAndRetrieveEvent(applicationName,invocation, responseTime));
    }

    public void addApplicationInfo(String applicationName) {
        doExecute(MetricsEvent.Type.APPLICATION_INFO, statHandler -> statHandler.addApplication(applicationName));
    }
    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        collectApplication(list);
        collectRequests(list);
        collectRT(list);

        return list;
    }

    private void collectApplication(List<MetricSample> list) {
        doCollect(MetricsEvent.Type.APPLICATION_INFO, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.APPLICATION_METRIC_INFO, k.getTags(),
                APPLICATION, v::get))));


    }

    private void collectRequests(List<MetricSample> list) {
        doCollect(MetricsEvent.Type.TOTAL, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS, k.getTags(), REQUESTS, v::get))));

        doCollect(MetricsEvent.Type.SUCCEED, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_SUCCEED, k.getTags(), REQUESTS, v::get))));

        doCollect(MetricsEvent.Type.UNKNOWN_FAILED, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_FAILED, k.getTags(), REQUESTS, v::get))));

        doCollect(MetricsEvent.Type.PROCESSING, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_PROCESSING, k.getTags(), REQUESTS, v::get))));

        doCollect(MetricsEvent.Type.BUSINESS_FAILED, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUEST_BUSINESS_FAILED, k.getTags(), REQUESTS, v::get))));

        doCollect(MetricsEvent.Type.REQUEST_TIMEOUT, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_TIMEOUT, k.getTags(), REQUESTS, v::get))));

        doCollect(MetricsEvent.Type.REQUEST_LIMIT, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_LIMIT, k.getTags(), REQUESTS, v::get))));

        doCollect(MetricsEvent.Type.TOTAL_FAILED, MetricsStatHandler::get).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_TOTAL_FAILED, k.getTags(), REQUESTS, v::get))));

    }

    private void collectRT(List<MetricSample> list) {
        this.stats.getLastRT().forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_LAST, k.getTags(), RT, v::get)));
        this.stats.getMinRT().forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_MIN, k.getTags(), RT, v::get)));
        this.stats.getMaxRT().forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_MAX, k.getTags(), RT, v::get)));

        this.stats.getTotalRT().forEach((k, v) -> {
            list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_SUM, k.getTags(), RT, v::get));

            AtomicLong avg = this.stats.getAvgRT().get(k);
            AtomicLong count = this.stats.getRtCount().get(k);
            avg.set(v.get() / count.get());
            list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_AVG, k.getTags(), RT, avg::get));
        });
    }

    private <
        T> Optional<T> doCollect(MetricsEvent.Type metricsEventType, Function<MetricsStatHandler, T> statExecutor) {
        if (isCollectEnabled()) {
            MetricsStatHandler handler = stats.getHandler(metricsEventType);
            T result = statExecutor.apply(handler);
            return Optional.ofNullable(result);
        }
        return Optional.empty();
    }

    private MetricsEvent doExecute(MetricsEvent.Type  metricsEventType,
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
