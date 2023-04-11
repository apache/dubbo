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
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
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
    public void decOnEvent(S source, K metricName) {
        doExecute(source, metricName, counter -> {
            counter.decrementAndGet();
            return true;
        });
    }

    @Override
    public void addRT(S source, Long rt) {
        MetricsCountSampleConfigurer<S, K, M> sampleConfigure = new MetricsCountSampleConfigurer<>();
        sampleConfigure.setSource(source);

        this.rtConfigure(sampleConfigure);

        M metric = sampleConfigure.getMetric();

        AtomicLongArray rtCalculator = ConcurrentHashMapUtils.computeIfAbsent(this.rtSample, metric, k -> new AtomicLongArray(4));

        // set lastRT
        rtCalculator.set(0, rt);

        // add to totalRT
        rtCalculator.addAndGet(1, rt);

        // add to rtCount
        rtCalculator.incrementAndGet(2);

        // calc avgRT. In order to reduce the amount of calculation, calculated when collect
        //rtArray.set(3, Math.floorDiv(rtArray.get(1), rtArray.get(2)));

        LongAccumulator min = ConcurrentHashMapUtils.computeIfAbsent(minRT, metric, k -> new LongAccumulator(Long::min, Long.MAX_VALUE));
        min.accumulate(rt);

        LongAccumulator max = ConcurrentHashMapUtils.computeIfAbsent(maxRT, metric, k -> new LongAccumulator(Long::max, Long.MIN_VALUE));
        max.accumulate(rt);

        sampleConfigure.setRt(rt);

        sampleConfigure.getFireEventHandler().accept(sampleConfigure);
    }

    @Override
    public void addRT(S source, K metricName, Long rt) {
        MetricsCountSampleConfigurer<S, K, M> sampleConfigure = new MetricsCountSampleConfigurer<>();
        sampleConfigure.setSource(source);
        sampleConfigure.setMetricsName(metricName);

        this.rtConfigure(sampleConfigure);

        M metric = sampleConfigure.getMetric();

        ConcurrentMap<M, AtomicLongArray> nameToCalculator = rtGroupSample.get(metricName);

        if (nameToCalculator == null) {
            ConcurrentHashMap<M, AtomicLongArray> calculator = new ConcurrentHashMap<>();
            calculator.put(metric, new AtomicLongArray(4));

            rtGroupSample.put(metricName, calculator);

            nameToCalculator = rtGroupSample.get(metricName);
        }
        AtomicLongArray calculator = nameToCalculator.get(metric);

        // set lastRT
        calculator.set(0, rt);

        // add to totalRT
        calculator.addAndGet(1, rt);

        // add to rtCount
        calculator.incrementAndGet(2);

        ConcurrentMap<M, LongAccumulator> minRT = ConcurrentHashMapUtils.computeIfAbsent(groupMinRT, metricName, k -> new ConcurrentHashMap<>());
        LongAccumulator min = ConcurrentHashMapUtils.computeIfAbsent(minRT, metric, k -> new LongAccumulator(Long::min, Long.MAX_VALUE));
        min.accumulate(rt);

        ConcurrentMap<M, LongAccumulator> maxRT = ConcurrentHashMapUtils.computeIfAbsent(groupMaxRT, metricName, k -> new ConcurrentHashMap<>());
        LongAccumulator max = ConcurrentHashMapUtils.computeIfAbsent(maxRT, metric, k -> new LongAccumulator(Long::max, Long.MIN_VALUE));
        max.accumulate(rt);
        sampleConfigure.setRt(rt);

        sampleConfigure.getFireEventHandler().accept(sampleConfigure);
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

    @Override
    public <R extends MetricSample> List<R> collectRT(MetricSampleFactory<M, R> factory, K metricName) {
        return collect(factory, rtGroupSample.get(metricName), groupMinRT.get(metricName), groupMaxRT.get(metricName));
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
