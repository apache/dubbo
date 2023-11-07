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
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.ThreadPoolRejectMetric;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.metrics.model.MetricsCategory.THREAD_POOL;

public class ThreadRejectMetricsCountSampler extends MetricsNameCountSampler<String, String, ThreadPoolRejectMetric> {

    public ThreadRejectMetricsCountSampler(DefaultMetricsCollector collector) {
        super(collector, THREAD_POOL, MetricsKey.THREAD_POOL_THREAD_REJECT_COUNT);
    }

    @Override
    protected MetricSample provideMetricsSample(
            ThreadPoolRejectMetric metric, AtomicLong count, MetricsKey metricsKey, MetricsCategory metricsCategory) {
        return new GaugeMetricSample<>(
                metricsKey.getNameByType(metric.getThreadPoolName()),
                metricsKey.getDescription(),
                metric.getTags(),
                metricsCategory,
                count,
                AtomicLong::get);
    }

    @Override
    protected void countConfigure(
            MetricsCountSampleConfigurer<String, String, ThreadPoolRejectMetric> sampleConfigure) {
        sampleConfigure.configureMetrics(
                configure -> new ThreadPoolRejectMetric(collector.getApplicationName(), configure.getSource()));
    }
}
