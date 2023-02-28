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

package org.apache.dubbo.metrics.metadata.collector.stat;

import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.metadata.event.MetadataEvent;
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
import java.util.stream.Collectors;

/**
 * As a data aggregator, use internal data containers calculates and classifies
 * the registry data collected by {@link MetricsCollector MetricsCollector}, and
 * provides an {@link MetricsExport MetricsExport} interface for exporting standard output formats.
 */
public class MetadataStatComposite implements MetricsExport {


    public Map<MetadataEvent.Type, Map<String, AtomicLong>> numStats = new ConcurrentHashMap<>();
    public List<LongContainer<? extends Number>> rtStats = new ArrayList<>();
    public static String OP_TYPE_PUSH = "push";
    public static String OP_TYPE_SUBSCRIBE = "subscribe";

    public MetadataStatComposite() {
        for (MetadataEvent.Type type : MetadataEvent.Type.values()) {
            numStats.put(type, new ConcurrentHashMap<>());
        }

        rtStats.addAll(initStats(OP_TYPE_PUSH));
        rtStats.addAll(initStats(OP_TYPE_SUBSCRIBE));
    }

    private List<LongContainer<? extends Number>> initStats(String registryOpType) {
        List<LongContainer<? extends Number>> singleRtStats = new ArrayList<>();
        singleRtStats.add(new AtomicLongContainer(new MetricsKeyWrapper(registryOpType, MetricsKey.METRIC_RT_LAST)));
        singleRtStats.add(new LongAccumulatorContainer(new MetricsKeyWrapper(registryOpType, MetricsKey.METRIC_RT_MIN), new LongAccumulator(Long::min, Long.MAX_VALUE)));
        singleRtStats.add(new LongAccumulatorContainer(new MetricsKeyWrapper(registryOpType, MetricsKey.METRIC_RT_MAX), new LongAccumulator(Long::max, Long.MIN_VALUE)));
        singleRtStats.add(new AtomicLongContainer(new MetricsKeyWrapper(registryOpType, MetricsKey.METRIC_RT_SUM), (responseTime, longAccumulator) -> longAccumulator.addAndGet(responseTime)));
        // AvgContainer is a special counter that stores the number of times but outputs function of sum/times
        AtomicLongContainer avgContainer = new AtomicLongContainer(new MetricsKeyWrapper(registryOpType, MetricsKey.METRIC_RT_AVG), (k, v) -> v.incrementAndGet());
        avgContainer.setValueSupplier(applicationName -> {
            LongContainer<? extends Number> totalContainer = rtStats.stream().filter(longContainer -> longContainer.isKeyWrapper(MetricsKey.METRIC_RT_SUM, registryOpType)).findFirst().get();
            AtomicLong totalRtTimes = avgContainer.get(applicationName);
            AtomicLong totalRtSum = (AtomicLong) totalContainer.get(applicationName);
            return totalRtSum.get() / totalRtTimes.get();
        });
        singleRtStats.add(avgContainer);
        return singleRtStats;
    }

    public void increment(MetadataEvent.Type type, String applicationName) {
        if (!numStats.containsKey(type)) {
            return;
        }
        numStats.get(type).computeIfAbsent(applicationName, k -> new AtomicLong(0L)).incrementAndGet();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void calcRt(String applicationName, String registryOpType, Long responseTime) {
        for (LongContainer container : rtStats.stream().filter(longContainer -> longContainer.specifyType(registryOpType)).collect(Collectors.toList())) {
            Number current = (Number) ConcurrentHashMapUtils.computeIfAbsent(container, applicationName, container.getInitFunc());
            container.getConsumerFunc().accept(responseTime, current);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<GaugeMetricSample> exportNumMetrics() {
        List<GaugeMetricSample> list = new ArrayList<>();
        for (MetadataEvent.Type type : numStats.keySet()) {
            Map<String, AtomicLong> stringAtomicLongMap = numStats.get(type);
            for (String applicationName : stringAtomicLongMap.keySet()) {
                list.add(convertToSample(applicationName, type, MetricsCategory.REGISTRY, stringAtomicLongMap.get(applicationName)));
            }
        }
        return list;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<GaugeMetricSample> exportRtMetrics() {
        List<GaugeMetricSample> list = new ArrayList<>();
        for (LongContainer<? extends Number> rtContainer : rtStats) {
            MetricsKeyWrapper metricsKeyWrapper = rtContainer.getMetricsKeyWrapper();
            for (Map.Entry<String, ? extends Number> entry : rtContainer.entrySet()) {
                list.add(new GaugeMetricSample<>(metricsKeyWrapper.targetKey(), metricsKeyWrapper.targetDesc(), ApplicationMetric.getTagsByName(entry.getKey()), MetricsCategory.RT, entry, value -> rtContainer.getValueSupplier().apply(value.getKey())));
            }
        }
        return list;
    }

    @SuppressWarnings("rawtypes")
    public GaugeMetricSample convertToSample(String applicationName, MetadataEvent.Type type, MetricsCategory category, AtomicLong targetNumber) {
        return new GaugeMetricSample<>(type.getMetricsKey(), ApplicationMetric.getTagsByName(applicationName), category, targetNumber, AtomicLong::get);
    }


    /**
     * Collect Number type data
     *
     * @param <NUMBER>
     */
    public static class LongContainer<NUMBER extends Number> extends ConcurrentHashMap<String, NUMBER> {

        /**
         * Provide the metric type name
         */
        private final MetricsKeyWrapper metricsKeyWrapper;
        /**
         * The initial value corresponding to the key is generally 0 of different data types
         */
        private final Function<String, NUMBER> initFunc;
        /**
         * Statistical data calculation function, which can be self-increment, self-decrement, or more complex avg function
         */
        private final BiConsumer<Long, NUMBER> consumerFunc;
        /**
         * Data output function required by  {@link GaugeMetricSample GaugeMetricSample}
         */
        private Function<String, Long> valueSupplier;


        public LongContainer(MetricsKeyWrapper metricsKeyWrapper, Supplier<NUMBER> initFunc, BiConsumer<Long, NUMBER> consumerFunc) {
            this.metricsKeyWrapper = metricsKeyWrapper;
            this.initFunc = s -> initFunc.get();
            this.consumerFunc = consumerFunc;
            this.valueSupplier = k -> this.get(k).longValue();
        }

        public boolean specifyType(String type) {
            return type.equals(getMetricsKeyWrapper().getType());
        }

        public MetricsKeyWrapper getMetricsKeyWrapper() {
            return metricsKeyWrapper;
        }

        public boolean isKeyWrapper(MetricsKey metricsKey, String registryOpType) {
            return metricsKeyWrapper.isKey(metricsKey,registryOpType);
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

        @Override
        public String toString() {
            return "LongContainer{" +
                "metricsKeyWrapper=" + metricsKeyWrapper +
                '}';
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
