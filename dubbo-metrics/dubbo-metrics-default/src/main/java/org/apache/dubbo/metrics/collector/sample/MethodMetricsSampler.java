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
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RTEvent;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.Metric;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.Invocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.metrics.model.MetricsCategory.REQUESTS;
import static org.apache.dubbo.metrics.model.MetricsCategory.RT;

public class MethodMetricsSampler extends SimpleMetricsCountSampler<Invocation, MetricsEvent.Type, MethodMetric>{

    private DefaultMetricsCollector collector;

    public MethodMetricsSampler(DefaultMetricsCollector collector) {
        this.collector = collector;
    }

    @Override
    protected void countConfigure(
            MetricsCountSampleConfigurer<Invocation, MetricsEvent.Type, MethodMetric> sampleConfigure) {
        sampleConfigure.configureMetrics(configure -> new MethodMetric(collector.getApplicationName(), configure.getSource()));
        sampleConfigure.configureEventHandler(configure -> collector.getEventMulticaster().publishEvent(new RequestEvent(configure.getMetric(), configure.getMetricName())));
    }

    @Override
    public void rtConfigure(
            MetricsCountSampleConfigurer<Invocation, MetricsEvent.Type, MethodMetric> sampleConfigure) {
        sampleConfigure.configureMetrics(configure -> new MethodMetric(collector.getApplicationName(), configure.getSource()));
        sampleConfigure.configureEventHandler(configure -> collector.getEventMulticaster().publishEvent(new RTEvent(configure.getMetric(), configure.getRt())));
    }

    @Override
    public List<MetricSample> sample() {
        List<MetricSample> metricSamples = new ArrayList<>();

        collect(metricSamples);

        collectRT(metricSamples);

        return metricSamples;
    }

    private void collect(List<MetricSample> list) {
        count(list, MetricsEvent.Type.TOTAL, MetricsKey.PROVIDER_METRIC_REQUESTS);
        count(list, MetricsEvent.Type.SUCCEED, MetricsKey.PROVIDER_METRIC_REQUESTS_SUCCEED);
        count(list, MetricsEvent.Type.UNKNOWN_FAILED, MetricsKey.PROVIDER_METRIC_REQUESTS_FAILED);
        count(list, MetricsEvent.Type.PROCESSING, MetricsKey.PROVIDER_METRIC_REQUESTS_PROCESSING);
        count(list, MetricsEvent.Type.BUSINESS_FAILED, MetricsKey.PROVIDER_METRIC_REQUEST_BUSINESS_FAILED);
        count(list, MetricsEvent.Type.REQUEST_TIMEOUT, MetricsKey.PROVIDER_METRIC_REQUESTS_TIMEOUT);
        count(list, MetricsEvent.Type.REQUEST_LIMIT, MetricsKey.PROVIDER_METRIC_REQUESTS_LIMIT);
        count(list, MetricsEvent.Type.TOTAL_FAILED, MetricsKey.PROVIDER_METRIC_REQUESTS_TOTAL_FAILED);
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

    private <T extends Metric> void count(List<MetricSample> list, MetricsEvent.Type eventType, MetricsKey metricsKey) {
        getCount(eventType).filter(e->!e.isEmpty())
                .ifPresent(map -> map.forEach((k, v) -> list.add(new GaugeMetricSample(metricsKey, k.getTags(),
                        REQUESTS, v::get))));
    }
}
