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

import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.metrics.aggregate.TimeWindowCounter;
import org.apache.dubbo.metrics.aggregate.TimeWindowQuantile;
import org.apache.dubbo.metrics.event.MethodEvent;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RTEvent;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.metrics.model.MetricsCategory.QPS;
import static org.apache.dubbo.metrics.model.MetricsCategory.REQUESTS;
import static org.apache.dubbo.metrics.model.MetricsCategory.RT;

/**
 * Aggregation metrics collector implementation of {@link MetricsCollector}.
 * This collector only enabled when metrics aggregation config is enabled.
 */
public class AggregateMetricsCollector implements MetricsCollector, MetricsListener {
    private int bucketNum;
    private int timeWindowSeconds;
    private final Map<String, ConcurrentHashMap<MethodMetric, TimeWindowCounter>> methodTypeCounter = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodMetric, TimeWindowQuantile> rt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MethodMetric, TimeWindowCounter> qps = new ConcurrentHashMap<>();
    private final ApplicationModel applicationModel;
    private static final Integer DEFAULT_COMPRESSION = 100;
    private static final Integer DEFAULT_BUCKET_NUM = 10;
    private static final Integer DEFAULT_TIME_WINDOW_SECONDS = 120;

    public AggregateMetricsCollector(ApplicationModel applicationModel) {
        this.registryEventTypeHandler();

        this.applicationModel = applicationModel;
        ConfigManager configManager = applicationModel.getApplicationConfigManager();
        MetricsConfig config = configManager.getMetrics().orElse(null);
        if (config != null && config.getAggregation() != null && (config.getAggregation().getEnabled() == null || Boolean.TRUE.equals(config.getAggregation().getEnabled()))) {
            // only registered when aggregation is enabled.
            registerListener();

            AggregationConfig aggregation = config.getAggregation();
            this.bucketNum = aggregation.getBucketNum() == null ? DEFAULT_BUCKET_NUM : aggregation.getBucketNum();
            this.timeWindowSeconds = aggregation.getTimeWindowSeconds() == null ? DEFAULT_TIME_WINDOW_SECONDS : aggregation.getTimeWindowSeconds();
        }
    }

    @Override
    public void onEvent(MetricsEvent event) {
        if (event instanceof RTEvent) {
            onRTEvent((RTEvent) event);
        } else if (event instanceof MethodEvent) {
            onRequestEvent((MethodEvent) event);
        }
    }

    private void onRTEvent(RTEvent event) {
        MethodMetric metric = (MethodMetric) event.getMetric();
        Long responseTime = event.getRt();
        TimeWindowQuantile quantile = ConcurrentHashMapUtils.computeIfAbsent(rt, metric, k -> new TimeWindowQuantile(DEFAULT_COMPRESSION, bucketNum, timeWindowSeconds));
        quantile.add(responseTime);
    }


    private void onRequestEvent(MethodEvent event) {
        MethodMetric metric = event.getMethodMetric();

        String type = event.getType();

        ConcurrentMap<MethodMetric, TimeWindowCounter> counter = methodTypeCounter.get(type);

        if (counter == null) {
            return;
        }
        TimeWindowCounter windowCounter = ConcurrentHashMapUtils.computeIfAbsent(counter, metric, methodMetric -> new TimeWindowCounter(bucketNum, timeWindowSeconds));

        if (MetricsEvent.Type.TOTAL.getNameByType(PROVIDER_SIDE).equals(type)
            || MetricsEvent.Type.TOTAL.getNameByType(CONSUMER_SIDE).equals(type)) {
            TimeWindowCounter qpsCounter = ConcurrentHashMapUtils.computeIfAbsent(qps, metric, methodMetric -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
            qpsCounter.increment();
        }
        windowCounter.increment();
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
        collectBySide(list, PROVIDER_SIDE);
        collectBySide(list, CONSUMER_SIDE);
    }

    private void collectBySide(List<MetricSample> list, String side) {
        collectMethod(list, MetricsEvent.Type.TOTAL.getNameByType(side), MetricsKey.METRIC_REQUESTS_TOTAL_AGG);
        collectMethod(list, MetricsEvent.Type.SUCCEED.getNameByType(side), MetricsKey.METRIC_REQUESTS_SUCCEED_AGG);
        collectMethod(list, MetricsEvent.Type.UNKNOWN_FAILED.getNameByType(side), MetricsKey.METRIC_REQUESTS_FAILED_AGG);
        collectMethod(list, MetricsEvent.Type.BUSINESS_FAILED.getNameByType(side), MetricsKey.METRIC_REQUESTS_BUSINESS_FAILED_AGG);
        collectMethod(list, MetricsEvent.Type.REQUEST_TIMEOUT.getNameByType(side), MetricsKey.METRIC_REQUESTS_TIMEOUT_AGG);
        collectMethod(list, MetricsEvent.Type.REQUEST_LIMIT.getNameByType(side), MetricsKey.METRIC_REQUESTS_LIMIT_AGG);
        collectMethod(list, MetricsEvent.Type.TOTAL_FAILED.getNameByType(side), MetricsKey.METRIC_REQUESTS_TOTAL_FAILED_AGG);
        collectMethod(list, MetricsEvent.Type.NETWORK_EXCEPTION.getNameByType(side), MetricsKey.METRIC_REQUESTS_TOTAL_NETWORK_FAILED_AGG);
        collectMethod(list, MetricsEvent.Type.CODEC_EXCEPTION.getNameByType(side), MetricsKey.METRIC_REQUESTS_TOTAL_CODEC_FAILED_AGG);
        collectMethod(list, MetricsEvent.Type.SERVICE_UNAVAILABLE.getNameByType(side), MetricsKey.METRIC_REQUESTS_TOTAL_SERVICE_UNAVAILABLE_FAILED_AGG);
    }

    private void collectMethod(List<MetricSample> list, String eventType, MetricsKey metricsKey) {
        ConcurrentHashMap<MethodMetric, TimeWindowCounter> windowCounter = methodTypeCounter.get(eventType);
        if (windowCounter != null) {
            windowCounter.forEach((k, v) -> list.add(new GaugeMetricSample<>(metricsKey.getNameByType(k.getSide()),
                metricsKey.getDescription(), k.getTags(), REQUESTS, v, TimeWindowCounter::get)));
        }
    }

    private void collectQPS(List<MetricSample> list) {
        qps.forEach((k, v) -> list.add(new GaugeMetricSample<>(MetricsKey.METRIC_QPS.getNameByType(k.getSide()),
            MetricsKey.METRIC_QPS.getDescription(), k.getTags(), QPS, v, value -> value.get() / value.bucketLivedSeconds())));
    }

    private void collectRT(List<MetricSample> list) {
        rt.forEach((k, v) -> {
            list.add(new GaugeMetricSample<>(MetricsKey.METRIC_RT_P99.getNameByType(k.getSide()),
                MetricsKey.METRIC_RT_P99.getDescription(), k.getTags(), RT, v, value -> value.quantile(0.99)));
            list.add(new GaugeMetricSample<>(MetricsKey.METRIC_RT_P95.getNameByType(k.getSide()),
                MetricsKey.METRIC_RT_P99.getDescription(), k.getTags(), RT, v, value -> value.quantile(0.95)));
        });
    }

    private void registryEventTypeHandler() {
        registryBySide(PROVIDER_SIDE);
        registryBySide(CONSUMER_SIDE);
    }

    private void registryBySide(String side) {
        methodTypeCounter.put(MetricsEvent.Type.TOTAL.getNameByType(side), new ConcurrentHashMap<>());
        methodTypeCounter.put(MetricsEvent.Type.SUCCEED.getNameByType(side), new ConcurrentHashMap<>());
        methodTypeCounter.put(MetricsEvent.Type.UNKNOWN_FAILED.getNameByType(side), new ConcurrentHashMap<>());
        methodTypeCounter.put(MetricsEvent.Type.BUSINESS_FAILED.getNameByType(side), new ConcurrentHashMap<>());
        methodTypeCounter.put(MetricsEvent.Type.REQUEST_TIMEOUT.getNameByType(side), new ConcurrentHashMap<>());
        methodTypeCounter.put(MetricsEvent.Type.REQUEST_LIMIT.getNameByType(side), new ConcurrentHashMap<>());
        methodTypeCounter.put(MetricsEvent.Type.TOTAL_FAILED.getNameByType(side), new ConcurrentHashMap<>());
        methodTypeCounter.put(MetricsEvent.Type.SERVICE_UNAVAILABLE.getNameByType(side), new ConcurrentHashMap<>());
        methodTypeCounter.put(MetricsEvent.Type.NETWORK_EXCEPTION.getNameByType(side), new ConcurrentHashMap<>());
        methodTypeCounter.put(MetricsEvent.Type.CODEC_EXCEPTION.getNameByType(side), new ConcurrentHashMap<>());
    }

    private void registerListener() {
        applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class).addListener(this);
    }

}
