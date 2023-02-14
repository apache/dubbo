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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.Function;

/**
 * @param <S> request source
 * @param <K> metricsName
 * @param <M> metric
 */

public abstract class SimpleMetricsCountSampler<S, K, M extends Metric>
    implements MetricsCountSampler<S, K, M> {

    private final ConcurrentMap<M, AtomicLong>      lastRT = new ConcurrentHashMap<>();
    private final ConcurrentMap<M, LongAccumulator> minRT  = new ConcurrentHashMap<>();
    private final ConcurrentMap<M, LongAccumulator> maxRT  = new ConcurrentHashMap<>();
    private final ConcurrentMap<M, AtomicLong>      avgRT   = new ConcurrentHashMap<>();
    private final ConcurrentMap<M, AtomicLong>      totalRT = new ConcurrentHashMap<>();
    private final ConcurrentMap<M, AtomicLong>      rtCount = new ConcurrentHashMap<>();

    private Map<K, ConcurrentMap<M, AtomicLong>> metricCounter = new ConcurrentHashMap<>();

    @Override
    public void inc(S source, K metricName) {
        doExecute(source,metricName,counter->{
            counter.incrementAndGet();
            return false;
        });
    }

    @Override
    public void dec(S source, K metricName) {
        doExecute(source,metricName,counter->{
            counter.decrementAndGet();
            return false;
        });
    }

    @Override
    public void incOnEvent(S source, K metricName) {
        doExecute(source,metricName,counter->{
            counter.incrementAndGet();
            return true;
        });
    }

    @Override
    public void decOnEvent(S source, K metricName) {
        doExecute(source,metricName,counter->{
            counter.decrementAndGet();
            return true;
        });
    }

    @Override
    public void addRT(S source, Long rt) {
        MetricsCountSampleConfigurer<S,K,M> sampleConfigure = new MetricsCountSampleConfigurer<>();
        sampleConfigure.setSource(source);

        this.rtConfigure(sampleConfigure);

        M metric = sampleConfigure.getMetric();

        AtomicLong last = ConcurrentHashMapUtils.computeIfAbsent(lastRT, metric, k -> new AtomicLong());
        last.set(rt);

        LongAccumulator min = ConcurrentHashMapUtils.computeIfAbsent(minRT, metric, k -> new LongAccumulator(Long::min, Long.MAX_VALUE));
        min.accumulate(rt);

        LongAccumulator max = ConcurrentHashMapUtils.computeIfAbsent(maxRT, metric, k -> new LongAccumulator(Long::max, Long.MIN_VALUE));
        max.accumulate(rt);

        AtomicLong total = ConcurrentHashMapUtils.computeIfAbsent(totalRT, metric, k -> new AtomicLong());
        total.addAndGet(rt);

        AtomicLong count = ConcurrentHashMapUtils.computeIfAbsent(rtCount, metric, k -> new AtomicLong());
        count.incrementAndGet();

        ConcurrentHashMapUtils.computeIfAbsent(avgRT, metric, key -> new AtomicLong());

        sampleConfigure.setRt(rt);

        sampleConfigure.getFireEventHandler().accept(sampleConfigure);

    }

    @Override
    public Optional<ConcurrentMap<M, AtomicLong>> getCount(K metricName) {
        return Optional.ofNullable(metricCounter.get(metricName) == null ?
            new ConcurrentHashMap<>() :
            metricCounter.get(metricName));
    }

    @Override
    public ConcurrentMap<M, AtomicLong> getLastRT() {
        return this.lastRT;
    }

    @Override
    public ConcurrentMap<M, LongAccumulator> getMinRT() {
        return this.minRT;
    }

    @Override
    public ConcurrentMap<M, LongAccumulator> getMaxRT() {
        return this.maxRT;
    }

    @Override
    public ConcurrentMap<M, AtomicLong> getAvgRT() {
        return this.avgRT;
    }

    @Override
    public ConcurrentMap<M, AtomicLong> getTotalRT() {
        return this.totalRT;
    }

    @Override
    public ConcurrentMap<M, AtomicLong> getRtCount() {
        return this.rtCount;
    }

    protected void rtConfigure(MetricsCountSampleConfigurer<S,K,M> configure) {

    }

    protected abstract void countConfigure(MetricsCountSampleConfigurer<S,K,M> sampleConfigure);

    private void doExecute(S source,K metricsName,Function<AtomicLong,Boolean> counter){
        MetricsCountSampleConfigurer<S,K,M> sampleConfigure = new MetricsCountSampleConfigurer<>();
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
