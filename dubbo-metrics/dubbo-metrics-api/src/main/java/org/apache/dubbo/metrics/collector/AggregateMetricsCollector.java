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

import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.common.metrics.collector.MetricsCollector;
import org.apache.dubbo.common.metrics.event.MetricsEvent;
import org.apache.dubbo.common.metrics.event.RTEvent;
import org.apache.dubbo.common.metrics.event.RequestEvent;
import org.apache.dubbo.common.metrics.listener.MetricsListener;
import org.apache.dubbo.common.metrics.model.MethodMetric;
import org.apache.dubbo.common.metrics.model.MetricsKey;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.metrics.aggregate.TimeWindowCounter;
import org.apache.dubbo.metrics.aggregate.TimeWindowQuantile;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.metrics.model.MetricsCategory.REQUESTS;
import static org.apache.dubbo.common.metrics.model.MetricsCategory.QPS;
import static org.apache.dubbo.common.metrics.model.MetricsCategory.RT;

/**
 * Aggregation metrics collector implementation of {@link MetricsCollector}.
 * This collector only enabled when metrics aggregation config is enabled.
 */
public class AggregateMetricsCollector implements MetricsCollector, MetricsListener {
    private int bucketNum;
    private int timeWindowSeconds;

    private final Map<MethodMetric, TimeWindowCounter> totalRequests = new ConcurrentHashMap<>();
    private final Map<MethodMetric, TimeWindowCounter> succeedRequests = new ConcurrentHashMap<>();
    private final Map<MethodMetric, TimeWindowCounter> failedRequests = new ConcurrentHashMap<>();
    private final Map<MethodMetric, TimeWindowCounter> businessFailedRequests = new ConcurrentHashMap<>();
    private final Map<MethodMetric, TimeWindowCounter> qps = new ConcurrentHashMap<>();
    private final Map<MethodMetric, TimeWindowQuantile> rt = new ConcurrentHashMap<>();

    private final ApplicationModel applicationModel;

    private static final Integer DEFAULT_COMPRESSION = 100;
    private static final Integer DEFAULT_BUCKET_NUM = 10;
    private static final Integer DEFAULT_TIME_WINDOW_SECONDS = 120;

    public AggregateMetricsCollector(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        ConfigManager configManager = applicationModel.getApplicationConfigManager();
        MetricsConfig config = configManager.getMetrics().orElse(null);
        if (config != null && config.getAggregation() != null && Boolean.TRUE.equals(config.getAggregation().getEnabled())) {
            // only registered when aggregation is enabled.
            registerListener();

            AggregationConfig aggregation = config.getAggregation();
            this.bucketNum = aggregation.getBucketNum() == null ? DEFAULT_BUCKET_NUM : aggregation.getBucketNum();
            this.timeWindowSeconds = aggregation.getTimeWindowSeconds() == null ? DEFAULT_TIME_WINDOW_SECONDS : aggregation.getTimeWindowSeconds();
        }
    }

    private void registerListener() {
        applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class).addListener(this);
    }

    @Override
    public void onEvent(MetricsEvent event) {
        if (event instanceof RTEvent) {
            onRTEvent((RTEvent) event);
        } else if (event instanceof RequestEvent) {
            onRequestEvent((RequestEvent) event);
        }
    }

    private void onRTEvent(RTEvent event) {
        MethodMetric metric = (MethodMetric) event.getSource();
        Long responseTime = event.getRt();
        TimeWindowQuantile quantile = rt.computeIfAbsent(metric, k -> new TimeWindowQuantile(DEFAULT_COMPRESSION, bucketNum, timeWindowSeconds));
        quantile.add(responseTime);
    }

    private void onRequestEvent(RequestEvent event) {
        MethodMetric metric = (MethodMetric) event.getSource();
        RequestEvent.Type type = event.getType();
        TimeWindowCounter counter = null;
        switch (type) {
            case TOTAL:
                counter = totalRequests.computeIfAbsent(metric, k -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
                TimeWindowCounter qpsCounter = qps.computeIfAbsent(metric, k -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
                qpsCounter.increment();
                break;
            case SUCCEED:
                counter = succeedRequests.computeIfAbsent(metric, k -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
                break;
            case FAILED:
                counter = failedRequests.computeIfAbsent(metric, k -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
                break;
            case BUSINESS_FAILED:
                counter = businessFailedRequests.computeIfAbsent(metric, k -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
                break;

            default:
                break;
        }

        if (counter != null) {
            counter.increment();
        }
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        collectRequests(list);
        collectQPS(list);
        collectRT(list);

        return list;
    }

    private void collectRequests(List<MetricSample> list) {
        totalRequests.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_REQUESTS_TOTAL_AGG, k.getTags(), REQUESTS, v::get)));
        succeedRequests.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_REQUESTS_SUCCEED_AGG, k.getTags(), REQUESTS, v::get)));
        failedRequests.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_REQUESTS_FAILED_AGG, k.getTags(), REQUESTS, v::get)));
        businessFailedRequests.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_REQUESTS_BUSINESS_FAILED_AGG, k.getTags(), REQUESTS, v::get)));
    }

    private void collectQPS(List<MetricSample> list) {
        qps.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_QPS, k.getTags(), QPS, () -> v.get() / v.bucketLivedSeconds())));
    }

    private void collectRT(List<MetricSample> list) {
        rt.forEach((k, v) -> {
            list.add(new GaugeMetricSample(MetricsKey.METRIC_RT_P99, k.getTags(), RT, () -> v.quantile(0.99)));
            list.add(new GaugeMetricSample(MetricsKey.METRIC_RT_P95, k.getTags(), RT, () -> v.quantile(0.95)));
        });
    }
}
