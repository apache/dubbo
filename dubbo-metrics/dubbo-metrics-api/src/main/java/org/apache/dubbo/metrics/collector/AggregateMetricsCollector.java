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
import org.apache.dubbo.common.metrics.event.BaseMetricsEvent;
import org.apache.dubbo.common.metrics.event.RTChangedEvent;
import org.apache.dubbo.common.metrics.event.RequestChangedEvent;
import org.apache.dubbo.common.metrics.listener.MetricsListener;
import org.apache.dubbo.common.metrics.model.MethodMetric;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.metrics.aggregate.TimeWindowCounter;
import org.apache.dubbo.metrics.aggregate.TimeWindowQuantile;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregation metrics collector implementation of {@link MetricsCollector}.
 * This collector only enabled when metrics aggregation config is enabled.
 */
public class AggregateMetricsCollector implements MetricsCollector, MetricsListener {

    private int bucketNum;
    private int timeWindowSeconds;

    private final Map<MethodMetric, TimeWindowCounter> totalRequests = new HashMap<>();
    private final Map<MethodMetric, TimeWindowCounter> succeedRequests = new HashMap<>();
    private final Map<MethodMetric, TimeWindowCounter> failedRequests = new HashMap<>();
    private final Map<MethodMetric, TimeWindowCounter> qps = new HashMap<>();
    private final Map<MethodMetric, TimeWindowQuantile> rt = new HashMap<>();

    private static final Integer DEFAULT_COMPRESSION = 100;
    private static final Integer DEFAULT_BUCKET_NUM = 10;
    private static final Integer DEFAULT_TIME_WINDOW_SECONDS = 120;

    private static final Integer QPS_BUCKET_NUM = 10;
    private static final Integer QPS_TIME_WINDOW_SECONDS = 1;

    private static final AggregateMetricsCollector INSTANCE = new AggregateMetricsCollector();

    private AggregateMetricsCollector() {
        ConfigManager configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
        MetricsConfig config = configManager.getMetrics().orElse(null);
        if (config != null && config.getAggregation() != null && Boolean.TRUE.equals(config.getAggregation().getEnabled())) {
            // only registered when aggregation is enabled.
            registerListener();

            AggregationConfig aggregation = config.getAggregation();
            this.bucketNum = aggregation.getBucketNum() == null ? DEFAULT_BUCKET_NUM : aggregation.getBucketNum();
            this.timeWindowSeconds = aggregation.getTimeWindowSeconds() == null ? DEFAULT_TIME_WINDOW_SECONDS : aggregation.getTimeWindowSeconds();
        }
    }

    public static AggregateMetricsCollector getInstance() {
        return INSTANCE;
    }

    private void registerListener() {
        DefaultMetricsCollector.getInstance().addListener(this);
    }

    @Override
    public void onEvent(BaseMetricsEvent event) {
        if (event instanceof RTChangedEvent) {
            onRTChangedEvent((RTChangedEvent) event);
        } else if (event instanceof RequestChangedEvent) {
            onRequestChangedEvent((RequestChangedEvent) event);
        }
    }

    private void onRTChangedEvent(RTChangedEvent event) {
        MethodMetric metric = (MethodMetric) event.getSource();
        Long responseTime = event.getRt();
        TimeWindowQuantile quantile = rt.computeIfAbsent(metric, k -> new TimeWindowQuantile(DEFAULT_COMPRESSION, bucketNum, timeWindowSeconds));
        quantile.add(responseTime);
    }

    private void onRequestChangedEvent(RequestChangedEvent event) {
        MethodMetric metric = (MethodMetric) event.getSource();
        RequestChangedEvent.Type type = event.getType();
        TimeWindowCounter counter = null;
        switch (type) {
            case TOTAL:
                counter = totalRequests.computeIfAbsent(metric, k -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
                TimeWindowCounter qpsCounter = qps.computeIfAbsent(metric, k -> new TimeWindowCounter(QPS_BUCKET_NUM, QPS_TIME_WINDOW_SECONDS));
                qpsCounter.increment();
                break;
            case SUCCEED:
                counter = succeedRequests.computeIfAbsent(metric, k -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
                break;
            case FAILED:
                counter = failedRequests.computeIfAbsent(metric, k -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
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
        totalRequests.forEach((k, v) -> list.add(new GaugeMetricSample("request.total." + timeWindowSeconds + "s", "Total Requests In " + timeWindowSeconds + "s", k.getTags(), v::get)));
        succeedRequests.forEach((k, v) -> list.add(new GaugeMetricSample("request.succeed." + timeWindowSeconds + "s", "Succeed Requests In " + timeWindowSeconds + "s", k.getTags(), v::get)));
        failedRequests.forEach((k, v) -> list.add(new GaugeMetricSample("request.failed." + timeWindowSeconds + "s", "Failed Requests In" + timeWindowSeconds + "s", k.getTags(), v::get)));
    }

    private void collectQPS(List<MetricSample> list) {
        qps.forEach((k, v) -> list.add(new GaugeMetricSample("qps", "Query Per Seconds", k.getTags(), v::get)));
    }

    private void collectRT(List<MetricSample> list) {
        rt.forEach((k, v) -> {
            list.add(new GaugeMetricSample("rt.p99", "Response Time P99", k.getTags(), () -> v.quantile(0.99)));
            list.add(new GaugeMetricSample("rt.p95", "Response Time P95", k.getTags(), () -> v.quantile(0.95)));
        });
    }
}
