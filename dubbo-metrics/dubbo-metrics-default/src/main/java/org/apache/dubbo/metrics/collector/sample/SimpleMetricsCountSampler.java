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

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.metrics.model.Metric;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.Function;

/**
 * @param <S> request source
 * @param <K> metricsName
 * @param <M> metric
 */

public abstract class SimpleMetricsCountSampler<S, K, M extends Metric>
    implements MetricsCountSampler<S, K, M> {

    private final ConcurrentMap<M, AtomicLong> EMPTY_COUNT = new ConcurrentHashMap<>();
    private final Map<K, ConcurrentMap<M, AtomicLong>> metricCounter = new ConcurrentHashMap<>();
    // lastRT, totalRT, rtCount, avgRT share a container, can utilize the system cache line
    private final ConcurrentMap<M, AtomicLongArray> rtSample = new ConcurrentHashMap<>();
    private final ConcurrentMap<M, LongAccumulator> minRT = new ConcurrentHashMap<>();
    private final ConcurrentMap<M, LongAccumulator> maxRT = new ConcurrentHashMap<>();

    private final ConcurrentMap<K, ConcurrentMap<M, AtomicLongArray>> rtGroupSample = new ConcurrentHashMap<>();
    private final ConcurrentMap<K, ConcurrentMap<M, LongAccumulator>> groupMinRT = new ConcurrentHashMap<>();
    private final ConcurrentMap<K, ConcurrentMap<M, LongAccumulator>> groupMaxRT = new ConcurrentHashMap<>();

    @Override
    public void inc(S source, K metricName) {
        doExecute(source, metricName, counter -> {
            counter.incrementAndGet();
            return false;
        });
    }

    @Override
    public void dec(S source, K metricName) {
        doExecute(source, metricName, counter -> {
            counter.decrementAndGet();
            return false;
        });
    }

    @Override
    public void incOnEvent(S source, K metricName) {
        doExecute(source, metricName, counter -> {
            counter.incrementAndGet();
            return true;
        });
    }

    @Override
    public Optional<ConcurrentMap<M, AtomicLong>> getCount(K metricName) {
        return Optional.ofNullable(metricCounter.get(metricName) == null ?
            EMPTY_COUNT :
            metricCounter.get(metricName));
    }

    @Override
    public <R extends MetricSample> List<R> collectRT(MetricSampleFactory<M, R> factory) {
        return collect(factory, rtSample, this.minRT, this.maxRT);
    }

    private <R extends MetricSample> List<R> collect(MetricSampleFactory<M, R> factory,
                                                     ConcurrentMap<M, AtomicLongArray> rtSample,
                                                     ConcurrentMap<M, LongAccumulator> min,
                                                     ConcurrentMap<M, LongAccumulator> max) {
        final List<R> result = new ArrayList<>();
        rtSample.forEach((k, v) -> {
            // lastRT
            result.add(factory.newInstance(MetricsKey.METRIC_RT_LAST, k, v, value -> value.get(0)));
            // totalRT
            result.add(factory.newInstance(MetricsKey.METRIC_RT_SUM, k, v, value -> value.get(1)));
            // avgRT
            result.add(factory.newInstance(MetricsKey.METRIC_RT_AVG, k, v, value -> Math.floorDiv(value.get(1), value.get(2))));
        });

        min.forEach((k, v) ->
            result.add(factory.newInstance(MetricsKey.METRIC_RT_MIN, k, v, LongAccumulator::get)));

        max.forEach((k, v) ->
            result.add(factory.newInstance(MetricsKey.METRIC_RT_MAX, k, v, LongAccumulator::get)));

        return result;
    }

    protected void rtConfigure(MetricsCountSampleConfigurer<S, K, M> configure) {
    }

    protected abstract void countConfigure(MetricsCountSampleConfigurer<S, K, M> sampleConfigure);

    private void doExecute(S source, K metricsName, Function<AtomicLong, Boolean> counter) {
        MetricsCountSampleConfigurer<S, K, M> sampleConfigure = new MetricsCountSampleConfigurer<>();
        sampleConfigure.setSource(source);
        sampleConfigure.setMetricsName(metricsName);

        this.countConfigure(sampleConfigure);

        Map<M, AtomicLong> metricAtomic = metricCounter.get(metricsName);

        if (metricAtomic == null) {
            metricAtomic = metricCounter.computeIfAbsent(metricsName, k -> new ConcurrentHashMap<>());
        }

        Assert.notNull(sampleConfigure.getMetric(), "metrics is null");

        AtomicLong atomicCounter = metricAtomic.get(sampleConfigure.getMetric());

        if (atomicCounter == null) {
            atomicCounter = metricAtomic.computeIfAbsent(sampleConfigure.getMetric(), k -> new AtomicLong());
        }
        Boolean isEvent = counter.apply(atomicCounter);

        if (isEvent) {
            sampleConfigure.getFireEventHandler().accept(sampleConfigure);
        }
    }

}
