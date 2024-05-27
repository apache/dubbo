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

import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.Metric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public abstract class MetricsNameCountSampler<S, K, M extends Metric> extends SimpleMetricsCountSampler<S, K, M> {

    protected final DefaultMetricsCollector collector;

    private final AtomicBoolean samplesChanged = new AtomicBoolean(true);

    protected final Set<K> metricNames = new ConcurrentHashSet<>();

    protected final MetricsCategory metricsCategory;

    protected final MetricsKey metricsKey;

    public MetricsNameCountSampler(
            DefaultMetricsCollector collector, MetricsCategory metricsCategory, MetricsKey metricsKey) {
        this.metricsCategory = metricsCategory;
        this.metricsKey = metricsKey;
        this.collector = collector;
        this.collector.addSampler(this);
    }

    public void addMetricName(K name) {
        this.metricNames.add(name);
        this.samplesChanged.set(true);
    }

    @Override
    public List<MetricSample> sample() {
        List<MetricSample> metricSamples = new ArrayList<>();
        metricNames.forEach(name -> collect(metricSamples, name));
        return metricSamples;
    }

    private void collect(List<MetricSample> samples, K metricName) {
        getCount(metricName)
                .filter(e -> !e.isEmpty())
                .ifPresent(map ->
                        map.forEach((k, v) -> samples.add(provideMetricsSample(k, v, metricsKey, metricsCategory))));
    }

    protected abstract MetricSample provideMetricsSample(
            M metric, AtomicLong count, MetricsKey metricsKey, MetricsCategory metricsCategory);

    @Override
    public boolean calSamplesChanged() {
        // CAS to get and reset the flag in an atomic operation
        return samplesChanged.compareAndSet(true, false);
    }
}
