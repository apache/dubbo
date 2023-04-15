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

import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.event.MethodEvent;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RTEvent;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.Metric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.Invocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToDoubleFunction;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;

public class MethodMetricsSampler extends SimpleMetricsCountSampler<Invocation, String, MethodMetric> {

    private final DefaultMetricsCollector collector;

    public MethodMetricsSampler(DefaultMetricsCollector collector) {
        this.collector = collector;
    }

    @Override
    protected void countConfigure(
        MetricsCountSampleConfigurer<Invocation, String, MethodMetric> sampleConfigure) {
        sampleConfigure.configureMetrics(configure -> new MethodMetric(collector.getApplicationName(), configure.getSource()));
        sampleConfigure.configureEventHandler(configure -> collector.getEventMulticaster().publishEvent(new MethodEvent(collector.getApplicationModel(), configure.getMetric(),
            configure.getMetricName())));
    }

    @Override
    public void rtConfigure(
        MetricsCountSampleConfigurer<Invocation, String, MethodMetric> sampleConfigure) {
        sampleConfigure.configureMetrics(configure -> new MethodMetric(collector.getApplicationName(), configure.getSource()));
        sampleConfigure.configureEventHandler(configure -> collector.getEventMulticaster().publishEvent(new RTEvent(collector.getApplicationModel(), configure.getMetric(), configure.getRt())));
    }

    @Override
    public List<MetricSample> sample() {
        List<MetricSample> metricSamples = new ArrayList<>();

        collect(metricSamples);
        metricSamples.addAll(
            this.collectRT(new MetricSampleFactory<MethodMetric, GaugeMetricSample<?>>() {
                @Override
                public <T> GaugeMetricSample<?> newInstance(MetricsKey key, MethodMetric metric, T value, ToDoubleFunction<T> apply) {
                    return createGaugeMetricSample(key, metric, MetricsCategory.RT, value, apply);
                }
            }));

        return metricSamples;
    }

    private void collect(List<MetricSample> list) {
        collectBySide(list, PROVIDER_SIDE);
        collectBySide(list, CONSUMER_SIDE);
    }

    private void collectBySide(List<MetricSample> list, String side) {
        count(list, MetricsEvent.Type.TOTAL.getNameByType(side), MetricSample.Type.COUNTER, MetricsKey.METRIC_REQUESTS);
        count(list, MetricsEvent.Type.SUCCEED.getNameByType(side), MetricSample.Type.COUNTER, MetricsKey.METRIC_REQUESTS_SUCCEED);
        count(list, MetricsEvent.Type.UNKNOWN_FAILED.getNameByType(side), MetricSample.Type.COUNTER, MetricsKey.METRIC_REQUESTS_FAILED);
        count(list, MetricsEvent.Type.PROCESSING.getNameByType(side), MetricSample.Type.GAUGE, MetricsKey.METRIC_REQUESTS_PROCESSING);
        count(list, MetricsEvent.Type.BUSINESS_FAILED.getNameByType(side), MetricSample.Type.COUNTER, MetricsKey.METRIC_REQUEST_BUSINESS_FAILED);
        count(list, MetricsEvent.Type.REQUEST_TIMEOUT.getNameByType(side), MetricSample.Type.COUNTER, MetricsKey.METRIC_REQUESTS_TIMEOUT);
        count(list, MetricsEvent.Type.REQUEST_LIMIT.getNameByType(side), MetricSample.Type.COUNTER, MetricsKey.METRIC_REQUESTS_LIMIT);
        count(list, MetricsEvent.Type.TOTAL_FAILED.getNameByType(side), MetricSample.Type.COUNTER, MetricsKey.METRIC_REQUESTS_TOTAL_FAILED);
        count(list, MetricsEvent.Type.NETWORK_EXCEPTION.getNameByType(side), MetricSample.Type.COUNTER, MetricsKey.METRIC_REQUESTS_NETWORK_FAILED);
        count(list, MetricsEvent.Type.SERVICE_UNAVAILABLE.getNameByType(side), MetricSample.Type.COUNTER, MetricsKey.METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED);
        count(list, MetricsEvent.Type.CODEC_EXCEPTION.getNameByType(side), MetricSample.Type.COUNTER, MetricsKey.METRIC_REQUESTS_CODEC_FAILED);
    }


    private <T> GaugeMetricSample<T> createGaugeMetricSample(MetricsKey metricsKey,
                                                             MethodMetric methodMetric,
                                                             MetricsCategory metricsCategory,
                                                             T value,
                                                             ToDoubleFunction<T> apply) {
        return new GaugeMetricSample<>(
            metricsKey.getNameByType(methodMetric.getSide()),
            metricsKey.getDescription(),
            methodMetric.getTags(),
            metricsCategory,
            value,
            apply);
    }

    private <T extends Metric> void count(List<MetricSample> list, String eventType, MetricSample.Type type, MetricsKey metricsKey) {
        getCount(eventType).filter(e -> !e.isEmpty())
            .ifPresent(map -> map.forEach((k, v) -> {
                    if(type == MetricSample.Type.COUNTER){
                        list.add(createCounterMetricSample(metricsKey, k, MetricsCategory.REQUESTS, v));
                    }else if(type == MetricSample.Type.GAUGE){
                        list.add(createGaugeMetricSample(metricsKey, k, MetricsCategory.REQUESTS, v, AtomicLong::get));
                    }
                }
            ));
    }

    private MetricSample createCounterMetricSample(MetricsKey metricsKey, MethodMetric methodMetric, MetricsCategory metricsCategory, AtomicLong value) {
        return new CounterMetricSample<>(metricsKey.getNameByType(methodMetric.getSide()),
            metricsKey.getDescription(),
            methodMetric.getTags(), metricsCategory, value);


    }
}
