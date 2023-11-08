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
import org.apache.dubbo.metrics.MetricsConstants;
import org.apache.dubbo.metrics.aggregate.TimeWindowAggregator;
import org.apache.dubbo.metrics.aggregate.TimeWindowCounter;
import org.apache.dubbo.metrics.aggregate.TimeWindowQuantile;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.metrics.DefaultConstants.INIT_AGG_METHOD_KEYS;
import static org.apache.dubbo.metrics.DefaultConstants.METRIC_THROWABLE;
import static org.apache.dubbo.metrics.model.MetricsCategory.QPS;
import static org.apache.dubbo.metrics.model.MetricsCategory.REQUESTS;
import static org.apache.dubbo.metrics.model.MetricsCategory.RT;

/**
 * Aggregation metrics collector implementation of {@link MetricsCollector}.
 * This collector only enabled when metrics aggregation config is enabled.
 */
public class AggregateMetricsCollector implements MetricsCollector<RequestEvent> {
    private int bucketNum = DEFAULT_BUCKET_NUM;
    private int timeWindowSeconds = DEFAULT_TIME_WINDOW_SECONDS;
    private int qpsTimeWindowMillSeconds = DEFAULT_QPS_TIME_WINDOW_MILL_SECONDS;
    private final Map<MetricsKeyWrapper, ConcurrentHashMap<MethodMetric, TimeWindowCounter>> methodTypeCounter =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodMetric, TimeWindowQuantile> rt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MethodMetric, TimeWindowCounter> qps = new ConcurrentHashMap<>();
    private final ApplicationModel applicationModel;
    private static final Integer DEFAULT_COMPRESSION = 100;
    private static final Integer DEFAULT_BUCKET_NUM = 10;
    private static final Integer DEFAULT_TIME_WINDOW_SECONDS = 120;
    private static final Integer DEFAULT_QPS_TIME_WINDOW_MILL_SECONDS = 3000;
    private Boolean collectEnabled = null;
    private boolean enableQps;
    private boolean enableRtPxx;
    private boolean enableRt;
    private boolean enableRequest;
    private final AtomicBoolean samplesChanged = new AtomicBoolean(true);

    private final ConcurrentMap<MethodMetric, TimeWindowAggregator> rtAgr = new ConcurrentHashMap<>();

    private boolean serviceLevel;

    public AggregateMetricsCollector(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        ConfigManager configManager = applicationModel.getApplicationConfigManager();
        if (isCollectEnabled()) {
            // only registered when aggregation is enabled.
            Optional<MetricsConfig> optional = configManager.getMetrics();
            if (optional.isPresent()) {
                registerListener();
                AggregationConfig aggregation = optional.get().getAggregation();
                this.bucketNum = Optional.ofNullable(aggregation.getBucketNum()).orElse(DEFAULT_BUCKET_NUM);
                this.timeWindowSeconds =
                        Optional.ofNullable(aggregation.getTimeWindowSeconds()).orElse(DEFAULT_TIME_WINDOW_SECONDS);
                this.qpsTimeWindowMillSeconds = Optional.ofNullable(aggregation.getQpsTimeWindowMillSeconds())
                        .orElse(DEFAULT_QPS_TIME_WINDOW_MILL_SECONDS);
                this.enableQps = Optional.ofNullable(aggregation.getEnableQps()).orElse(true);
                this.enableRtPxx =
                        Optional.ofNullable(aggregation.getEnableRtPxx()).orElse(true);
                this.enableRt = Optional.ofNullable(aggregation.getEnableRt()).orElse(true);
                this.enableRequest =
                        Optional.ofNullable(aggregation.getEnableRequest()).orElse(true);
            }
            this.serviceLevel = MethodMetric.isServiceLevel(applicationModel);
        }
    }

    public void setCollectEnabled(Boolean collectEnabled) {
        if (collectEnabled != null) {
            this.collectEnabled = collectEnabled;
        }
    }

    @Override
    public boolean isCollectEnabled() {
        if (collectEnabled == null) {
            ConfigManager configManager = applicationModel.getApplicationConfigManager();
            configManager
                    .getMetrics()
                    .ifPresent(metricsConfig ->
                            setCollectEnabled(metricsConfig.getAggregation().getEnabled()));
        }
        return Optional.ofNullable(collectEnabled).orElse(true);
    }

    @Override
    public boolean isSupport(MetricsEvent event) {
        return event instanceof RequestEvent;
    }

    @Override
    public void onEvent(RequestEvent event) {
        if (enableQps) {
            MethodMetric metric = calcWindowCounter(event, MetricsKey.METRIC_REQUESTS);
            TimeWindowCounter qpsCounter = qps.get(metric);
            if (qpsCounter == null) {
                qpsCounter = ConcurrentHashMapUtils.computeIfAbsent(
                        qps,
                        metric,
                        methodMetric -> new TimeWindowCounter(
                                bucketNum, TimeUnit.MILLISECONDS.toSeconds(qpsTimeWindowMillSeconds)));
                samplesChanged.set(true);
            }
            qpsCounter.increment();
        }
    }

    @Override
    public void onEventFinish(RequestEvent event) {
        MetricsKey targetKey = MetricsKey.METRIC_REQUESTS_SUCCEED;
        Object throwableObj = event.getAttachmentValue(METRIC_THROWABLE);
        if (throwableObj != null) {
            targetKey = MetricsSupport.getAggMetricsKey((Throwable) throwableObj);
        }
        calcWindowCounter(event, targetKey);
        onRTEvent(event);
    }

    @Override
    public void onEventError(RequestEvent event) {
        if (enableRequest) {
            MetricsKey targetKey = MetricsKey.METRIC_REQUESTS_FAILED;
            Object throwableObj = event.getAttachmentValue(METRIC_THROWABLE);
            if (throwableObj != null) {
                targetKey = MetricsSupport.getAggMetricsKey((Throwable) throwableObj);
            }
            calcWindowCounter(event, targetKey);
        }
        if (enableRt || enableRtPxx) {
            onRTEvent(event);
        }
    }

    private void onRTEvent(RequestEvent event) {
        MethodMetric metric =
                new MethodMetric(applicationModel, event.getAttachmentValue(MetricsConstants.INVOCATION), serviceLevel);
        long responseTime = event.getTimePair().calc();
        if (enableRt) {
            TimeWindowQuantile quantile = rt.get(metric);
            if (quantile == null) {
                quantile = ConcurrentHashMapUtils.computeIfAbsent(
                        rt, metric, k -> new TimeWindowQuantile(DEFAULT_COMPRESSION, bucketNum, timeWindowSeconds));
                samplesChanged.set(true);
            }
            quantile.add(responseTime);
        }

        if (enableRtPxx) {
            TimeWindowAggregator timeWindowAggregator = rtAgr.get(metric);
            if (timeWindowAggregator == null) {
                timeWindowAggregator = ConcurrentHashMapUtils.computeIfAbsent(
                        rtAgr, metric, methodMetric -> new TimeWindowAggregator(bucketNum, timeWindowSeconds));
                samplesChanged.set(true);
            }
            timeWindowAggregator.add(responseTime);
        }
    }

    private MethodMetric calcWindowCounter(RequestEvent event, MetricsKey targetKey) {
        MetricsPlaceValue placeType =
                MetricsPlaceValue.of(event.getAttachmentValue(MetricsConstants.INVOCATION_SIDE), MetricsLevel.SERVICE);
        MetricsKeyWrapper metricsKeyWrapper = new MetricsKeyWrapper(targetKey, placeType);
        MethodMetric metric =
                new MethodMetric(applicationModel, event.getAttachmentValue(MetricsConstants.INVOCATION), serviceLevel);

        ConcurrentMap<MethodMetric, TimeWindowCounter> counter =
                methodTypeCounter.computeIfAbsent(metricsKeyWrapper, k -> new ConcurrentHashMap<>());

        TimeWindowCounter windowCounter = counter.get(metric);
        if (windowCounter == null) {
            windowCounter = ConcurrentHashMapUtils.computeIfAbsent(
                    counter, metric, methodMetric -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
            samplesChanged.set(true);
        }
        windowCounter.increment();
        return metric;
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        if (!isCollectEnabled()) {
            return list;
        }
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
        collectMethod(list, side, MetricsKey.METRIC_REQUESTS_TOTAL_AGG);
        collectMethod(list, side, MetricsKey.METRIC_REQUESTS_SUCCEED_AGG);
        collectMethod(list, side, MetricsKey.METRIC_REQUESTS_FAILED_AGG);
        collectMethod(list, side, MetricsKey.METRIC_REQUEST_BUSINESS_FAILED_AGG);
        collectMethod(list, side, MetricsKey.METRIC_REQUESTS_TIMEOUT_AGG);
        collectMethod(list, side, MetricsKey.METRIC_REQUESTS_LIMIT_AGG);
        collectMethod(list, side, MetricsKey.METRIC_REQUESTS_TOTAL_FAILED_AGG);
        collectMethod(list, side, MetricsKey.METRIC_REQUESTS_NETWORK_FAILED_AGG);
        collectMethod(list, side, MetricsKey.METRIC_REQUESTS_CODEC_FAILED_AGG);
        collectMethod(list, side, MetricsKey.METRIC_REQUESTS_TOTAL_SERVICE_UNAVAILABLE_FAILED_AGG);
    }

    private void collectMethod(List<MetricSample> list, String side, MetricsKey metricsKey) {
        MetricsKeyWrapper metricsKeyWrapper =
                new MetricsKeyWrapper(metricsKey, MetricsPlaceValue.of(side, MetricsLevel.SERVICE));
        ConcurrentHashMap<MethodMetric, TimeWindowCounter> windowCounter = methodTypeCounter.get(metricsKeyWrapper);
        if (windowCounter != null) {
            windowCounter.forEach((k, v) -> list.add(new GaugeMetricSample<>(
                    metricsKey.getNameByType(k.getSide()),
                    metricsKey.getDescription(),
                    k.getTags(),
                    REQUESTS,
                    v,
                    TimeWindowCounter::get)));
        }
    }

    private void collectQPS(List<MetricSample> list) {
        qps.forEach((k, v) -> list.add(new GaugeMetricSample<>(
                MetricsKey.METRIC_QPS.getNameByType(k.getSide()),
                MetricsKey.METRIC_QPS.getDescription(),
                k.getTags(),
                QPS,
                v,
                value -> {
                    double total = value.get();
                    long millSeconds = value.bucketLivedMillSeconds();
                    return total / millSeconds * 1000;
                })));
    }

    private void collectRT(List<MetricSample> list) {
        rt.forEach((k, v) -> {
            list.add(new GaugeMetricSample<>(
                    MetricsKey.METRIC_RT_P99.getNameByType(k.getSide()),
                    MetricsKey.METRIC_RT_P99.getDescription(),
                    k.getTags(),
                    RT,
                    v,
                    value -> value.quantile(0.99)));
            list.add(new GaugeMetricSample<>(
                    MetricsKey.METRIC_RT_P95.getNameByType(k.getSide()),
                    MetricsKey.METRIC_RT_P95.getDescription(),
                    k.getTags(),
                    RT,
                    v,
                    value -> value.quantile(0.95)));
            list.add(new GaugeMetricSample<>(
                    MetricsKey.METRIC_RT_P90.getNameByType(k.getSide()),
                    MetricsKey.METRIC_RT_P90.getDescription(),
                    k.getTags(),
                    RT,
                    v,
                    value -> value.quantile(0.90)));
            list.add(new GaugeMetricSample<>(
                    MetricsKey.METRIC_RT_P50.getNameByType(k.getSide()),
                    MetricsKey.METRIC_RT_P50.getDescription(),
                    k.getTags(),
                    RT,
                    v,
                    value -> value.quantile(0.50)));
        });

        rtAgr.forEach((k, v) -> {
            list.add(new GaugeMetricSample<>(
                    MetricsKey.METRIC_RT_MIN_AGG.getNameByType(k.getSide()),
                    MetricsKey.METRIC_RT_MIN_AGG.getDescription(),
                    k.getTags(),
                    RT,
                    v,
                    value -> v.get().getMin()));

            list.add(new GaugeMetricSample<>(
                    MetricsKey.METRIC_RT_MAX_AGG.getNameByType(k.getSide()),
                    MetricsKey.METRIC_RT_MAX_AGG.getDescription(),
                    k.getTags(),
                    RT,
                    v,
                    value -> v.get().getMax()));

            list.add(new GaugeMetricSample<>(
                    MetricsKey.METRIC_RT_AVG_AGG.getNameByType(k.getSide()),
                    MetricsKey.METRIC_RT_AVG_AGG.getDescription(),
                    k.getTags(),
                    RT,
                    v,
                    value -> v.get().getAvg()));
        });
    }

    private void registerListener() {
        applicationModel
                .getBeanFactory()
                .getBean(DefaultMetricsCollector.class)
                .getEventMulticaster()
                .addListener(this);
    }

    @Override
    public void initMetrics(MetricsEvent event) {
        MethodMetric metric =
                new MethodMetric(applicationModel, event.getAttachmentValue(MetricsConstants.INVOCATION), serviceLevel);
        if (enableQps) {
            initMethodMetric(event);
            initQpsMetric(metric);
        }
        if (enableRt) {
            initRtMetric(metric);
        }
        if (enableRtPxx) {
            initRtAgrMetric(metric);
        }
    }

    public void initMethodMetric(MetricsEvent event) {
        INIT_AGG_METHOD_KEYS.stream().forEach(key -> initWindowCounter(event, key));
    }

    public void initQpsMetric(MethodMetric metric) {
        ConcurrentHashMapUtils.computeIfAbsent(
                qps, metric, methodMetric -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
        samplesChanged.set(true);
    }

    public void initRtMetric(MethodMetric metric) {
        ConcurrentHashMapUtils.computeIfAbsent(
                rt, metric, k -> new TimeWindowQuantile(DEFAULT_COMPRESSION, bucketNum, timeWindowSeconds));
        samplesChanged.set(true);
    }

    public void initRtAgrMetric(MethodMetric metric) {
        ConcurrentHashMapUtils.computeIfAbsent(
                rtAgr, metric, k -> new TimeWindowAggregator(bucketNum, timeWindowSeconds));
        samplesChanged.set(true);
    }

    public void initWindowCounter(MetricsEvent event, MetricsKey targetKey) {

        MetricsKeyWrapper metricsKeyWrapper = new MetricsKeyWrapper(
                targetKey,
                MetricsPlaceValue.of(event.getAttachmentValue(MetricsConstants.INVOCATION_SIDE), MetricsLevel.SERVICE));

        MethodMetric metric =
                new MethodMetric(applicationModel, event.getAttachmentValue(MetricsConstants.INVOCATION), serviceLevel);

        ConcurrentMap<MethodMetric, TimeWindowCounter> counter =
                methodTypeCounter.computeIfAbsent(metricsKeyWrapper, k -> new ConcurrentHashMap<>());

        ConcurrentHashMapUtils.computeIfAbsent(
                counter, metric, methodMetric -> new TimeWindowCounter(bucketNum, timeWindowSeconds));
        samplesChanged.set(true);
    }

    @Override
    public boolean calSamplesChanged() {
        // CAS to get and reset the flag in an atomic operation
        return samplesChanged.compareAndSet(true, false);
    }
}
