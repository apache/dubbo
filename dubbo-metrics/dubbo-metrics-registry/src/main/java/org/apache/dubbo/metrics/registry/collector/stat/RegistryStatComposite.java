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

package org.apache.dubbo.metrics.registry.collector.stat;

import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.registry.MetricsKeyWrapper;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.metrics.registry.event.RegistryRegisterEvent;
import org.apache.dubbo.metrics.report.MetricsExport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RegistryStatComposite implements MetricsExport {

    public Map<RegistryRegisterEvent.Type, Map<String, AtomicLong>> numStats = new ConcurrentHashMap<>();
    public List<LongContainer<? extends Number>> rtStats = new ArrayList<>();

    public RegistryStatComposite() {
        initDataStructure();
    }

    private void initDataStructure() {
        for (RegistryEvent.Type type : RegistryEvent.Type.values()) {
            numStats.put(type, new ConcurrentHashMap<>());
        }

        rtStats.addAll(initStats("register"));
        rtStats.addAll(initStats("subscribe"));

    }

    private List<LongContainer<? extends Number>> initStats(String registryOpType) {
        List<LongContainer<? extends Number>> soleRtStats = new ArrayList<>();
        soleRtStats.add(new AtomicLongContainer(new MetricsKeyWrapper(registryOpType, MetricsKey.GENERIC_METRIC_RT_LAST)));
        soleRtStats.add(new LongAccumulatorContainer(new MetricsKeyWrapper(registryOpType, MetricsKey.GENERIC_METRIC_RT_MIN), new LongAccumulator(Long::min, Long.MAX_VALUE)));
        soleRtStats.add(new LongAccumulatorContainer(new MetricsKeyWrapper(registryOpType, MetricsKey.GENERIC_METRIC_RT_MAX), new LongAccumulator(Long::max, Long.MIN_VALUE)));
        soleRtStats.add(new AtomicLongContainer(new MetricsKeyWrapper(registryOpType, MetricsKey.GENERIC_METRIC_RT_SUM), (responseTime, longAccumulator) -> longAccumulator.addAndGet(responseTime)));
        AtomicLongContainer avgContainer = new AtomicLongContainer(new MetricsKeyWrapper(registryOpType, MetricsKey.GENERIC_METRIC_RT_AVG), (k, v) -> v.incrementAndGet());
        avgContainer.setValueSupplier(applicationName -> {
            LongContainer<? extends Number> totalContainer = rtStats.stream().filter(longContainer -> longContainer.isKey(MetricsKey.GENERIC_METRIC_RT_SUM)).findFirst().get();
            AtomicLong totalRtTimes = avgContainer.get(applicationName);
            AtomicLong totalRtSum = (AtomicLong) totalContainer.get(applicationName);
            return totalRtSum.get() / totalRtTimes.get();
        });
        soleRtStats.add(avgContainer);
        return soleRtStats;
    }

    public void increment(RegistryRegisterEvent.Type type, String applicationName) {
        if (!numStats.containsKey(type)) {
            return;
        }
        numStats.get(type).computeIfAbsent(applicationName, k -> new AtomicLong(0L)).incrementAndGet();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void calcRt(String applicationName, Long responseTime) {
        for (LongContainer container : rtStats) {
            Number current = (Number) ConcurrentHashMapUtils.computeIfAbsent(container, applicationName, container.getInitFunc());
            BiConsumer consumerFunc = container.getConsumerFunc();
            consumerFunc.accept(responseTime, current);
        }
    }

    @Override
    public List<GaugeMetricSample> exportNumMetrics() {
        List<GaugeMetricSample> list = new ArrayList<>();
        for (RegistryEvent.Type type : numStats.keySet()) {
            Map<String, AtomicLong> stringAtomicLongMap = numStats.get(type);
            for (String applicationName : stringAtomicLongMap.keySet()) {
                list.add(convertTargetFormat(applicationName, type, MetricsCategory.REQUESTS, stringAtomicLongMap.get(applicationName)));
            }
        }
        return list;
    }

    @Override
    public List<GaugeMetricSample> exportRtMetrics() {
        List<GaugeMetricSample> list = new ArrayList<>();
        for (LongContainer<? extends Number> rtContainer : rtStats) {
            MetricsKeyWrapper metricsKeyWrapper = rtContainer.getMetricsKeyWrapper();
            for (Map.Entry<String, ? extends Number> entry : rtContainer.entrySet()) {
                list.add(new GaugeMetricSample(metricsKeyWrapper.targetKey(), metricsKeyWrapper.targetDesc(), ApplicationMetric.getTagsByName(entry.getKey()), MetricsCategory.RT, () -> rtContainer.getValueSupplier().apply(entry.getKey())));
            }
        }
        return list;
    }

    public GaugeMetricSample convertTargetFormat(String applicationName, RegistryEvent.Type type, MetricsCategory category, AtomicLong targetNumber) {
        return new GaugeMetricSample(type.getMetricsKey(), ApplicationMetric.getTagsByName(applicationName), category, targetNumber::get);
    }


    public static class LongContainer<NUMBER extends Number> extends ConcurrentHashMap<String, NUMBER> {

        private final MetricsKeyWrapper metricsKeyWrapper;
        private final Function<String, NUMBER> initFunc;
        private final BiConsumer<Long, NUMBER> consumerFunc;
        private Function<String, Long> valueSupplier;


        public LongContainer(MetricsKeyWrapper metricsKeyWrapper, Supplier<NUMBER> initFunc, BiConsumer<Long, NUMBER> consumerFunc) {
            this.metricsKeyWrapper = metricsKeyWrapper;
            this.initFunc = s -> initFunc.get();
            this.consumerFunc = consumerFunc;
            this.valueSupplier = k -> this.get(k).longValue();
        }

        public MetricsKeyWrapper getMetricsKeyWrapper() {
            return metricsKeyWrapper;
        }

        public boolean isKey(MetricsKey metricsKey) {
            return metricsKeyWrapper.isKey(metricsKey);
        }

        public Function<String, NUMBER> getInitFunc() {
            return initFunc;
        }

        public BiConsumer<Long, NUMBER> getConsumerFunc() {
            return consumerFunc;
        }

        public Function<String, Long> getValueSupplier() {
            return valueSupplier;
        }

        public void setValueSupplier(Function<String, Long> valueSupplier) {
            this.valueSupplier = valueSupplier;
        }
    }

    public static class AtomicLongContainer extends LongContainer<AtomicLong> {

        public AtomicLongContainer(MetricsKeyWrapper metricsKeyWrapper) {
            super(metricsKeyWrapper, AtomicLong::new, (responseTime, longAccumulator) -> longAccumulator.set(responseTime));
        }

        public AtomicLongContainer(MetricsKeyWrapper metricsKeyWrapper, BiConsumer<Long, AtomicLong> consumerFunc) {
            super(metricsKeyWrapper, AtomicLong::new, consumerFunc);
        }

    }

    public static class LongAccumulatorContainer extends LongContainer<LongAccumulator> {
        public LongAccumulatorContainer(MetricsKeyWrapper metricsKeyWrapper, LongAccumulator accumulator) {
            super(metricsKeyWrapper, () -> accumulator, (responseTime, longAccumulator) -> longAccumulator.accumulate(responseTime));
        }
    }


}
