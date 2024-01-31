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
package org.apache.dubbo.metrics.data;

import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.Metric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.ServiceKeyMetric;
import org.apache.dubbo.metrics.model.container.AtomicLongContainer;
import org.apache.dubbo.metrics.model.container.LongAccumulatorContainer;
import org.apache.dubbo.metrics.model.container.LongContainer;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.report.AbstractMetricsExport;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * The data container of the rt dimension, including application, service, and method levels,
 * if there is no actual call to the existing call method,
 * the key will not be displayed when exporting (to be optimized)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RtStatComposite extends AbstractMetricsExport {

    private final AtomicBoolean samplesChanged = new AtomicBoolean(true);

    public RtStatComposite(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    private final Map<String, List<LongContainer<? extends Number>>> rtStats = new ConcurrentHashMap<>();

    public void init(MetricsPlaceValue... placeValues) {
        if (placeValues == null) {
            return;
        }
        for (MetricsPlaceValue placeValue : placeValues) {
            List<LongContainer<? extends Number>> containers = initStats(placeValue);
            for (LongContainer<? extends Number> container : containers) {
                rtStats.computeIfAbsent(container.getMetricsKeyWrapper().getType(), k -> new ArrayList<>())
                        .add(container);
            }
        }
        samplesChanged.set(true);
    }

    private List<LongContainer<? extends Number>> initStats(MetricsPlaceValue placeValue) {
        List<LongContainer<? extends Number>> singleRtStats = new ArrayList<>();
        singleRtStats.add(new AtomicLongContainer(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, placeValue)));
        singleRtStats.add(new LongAccumulatorContainer(
                new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, placeValue),
                new LongAccumulator(Long::min, Long.MAX_VALUE)));
        singleRtStats.add(new LongAccumulatorContainer(
                new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, placeValue),
                new LongAccumulator(Long::max, Long.MIN_VALUE)));
        singleRtStats.add(new AtomicLongContainer(
                new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, placeValue),
                (responseTime, longAccumulator) -> longAccumulator.addAndGet(responseTime)));
        // AvgContainer is a special counter that stores the number of times but outputs function of sum/times
        AtomicLongContainer avgContainer = new AtomicLongContainer(
                new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, placeValue), (k, v) -> v.incrementAndGet());
        avgContainer.setValueSupplier(applicationName -> {
            LongContainer<? extends Number> totalContainer = rtStats.values().stream()
                    .flatMap(List::stream)
                    .filter(longContainer -> longContainer.isKeyWrapper(MetricsKey.METRIC_RT_SUM, placeValue.getType()))
                    .findFirst()
                    .get();
            AtomicLong totalRtTimes = avgContainer.get(applicationName);
            AtomicLong totalRtSum = (AtomicLong) totalContainer.get(applicationName);
            return totalRtSum.get() / totalRtTimes.get();
        });
        singleRtStats.add(avgContainer);
        return singleRtStats;
    }

    public void calcServiceKeyRt(String registryOpType, Long responseTime, Metric key) {
        for (LongContainer container : rtStats.get(registryOpType)) {
            Number current = (Number) container.get(key);
            if (current == null) {
                container.putIfAbsent(key, container.getInitFunc().apply(key));
                samplesChanged.set(true);
                current = (Number) container.get(key);
            }
            container.getConsumerFunc().accept(responseTime, current);
        }
    }

    public void calcServiceKeyRt(Invocation invocation, String registryOpType, Long responseTime) {
        List<Action> actions;
        if (invocation.getServiceModel() != null && invocation.getServiceModel().getServiceKey() != null) {
            Map<String, Object> attributeMap =
                    invocation.getServiceModel().getServiceMetadata().getAttributeMap();
            Map<String, List<Action>> cache = (Map<String, List<Action>>) attributeMap.get("ServiceKeyRt");
            if (cache == null) {
                attributeMap.putIfAbsent("ServiceKeyRt", new ConcurrentHashMap<>(32));
                cache = (Map<String, List<Action>>) attributeMap.get("ServiceKeyRt");
            }
            actions = cache.get(registryOpType);
            if (actions == null) {
                actions = calServiceRtActions(invocation, registryOpType);
                cache.putIfAbsent(registryOpType, actions);
                samplesChanged.set(true);
                actions = cache.get(registryOpType);
            }
        } else {
            actions = calServiceRtActions(invocation, registryOpType);
        }

        for (Action action : actions) {
            action.run(responseTime);
        }
    }

    private List<Action> calServiceRtActions(Invocation invocation, String registryOpType) {
        List<Action> actions;
        actions = new LinkedList<>();

        ServiceKeyMetric key = new ServiceKeyMetric(getApplicationModel(), invocation.getTargetServiceUniqueName());
        for (LongContainer container : rtStats.get(registryOpType)) {
            Number current = (Number) container.get(key);
            if (current == null) {
                container.putIfAbsent(key, container.getInitFunc().apply(key));
                samplesChanged.set(true);
                current = (Number) container.get(key);
            }
            actions.add(new Action(container.getConsumerFunc(), current));
        }
        return actions;
    }

    public void calcMethodKeyRt(Invocation invocation, String registryOpType, Long responseTime) {
        List<Action> actions;

        if (getServiceLevel()
                && invocation.getServiceModel() != null
                && invocation.getServiceModel().getServiceMetadata() != null) {
            Map<String, Object> attributeMap =
                    invocation.getServiceModel().getServiceMetadata().getAttributeMap();
            Map<String, List<Action>> cache = (Map<String, List<Action>>) attributeMap.get("MethodKeyRt");
            if (cache == null) {
                attributeMap.putIfAbsent("MethodKeyRt", new ConcurrentHashMap<>(32));
                cache = (Map<String, List<Action>>) attributeMap.get("MethodKeyRt");
            }
            actions = cache.get(registryOpType);
            if (actions == null) {
                actions = calMethodRtActions(invocation, registryOpType);
                cache.putIfAbsent(registryOpType, actions);
                samplesChanged.set(true);
                actions = cache.get(registryOpType);
            }
        } else {
            actions = calMethodRtActions(invocation, registryOpType);
        }

        for (Action action : actions) {
            action.run(responseTime);
        }
    }

    private List<Action> calMethodRtActions(Invocation invocation, String registryOpType) {
        List<Action> actions;
        actions = new LinkedList<>();
        for (LongContainer container : rtStats.get(registryOpType)) {
            MethodMetric key = new MethodMetric(getApplicationModel(), invocation, getServiceLevel());
            Number current = (Number) container.get(key);
            if (current == null) {
                container.putIfAbsent(key, container.getInitFunc().apply(key));
                samplesChanged.set(true);
                current = (Number) container.get(key);
            }
            actions.add(new Action(container.getConsumerFunc(), current));
        }
        return actions;
    }

    public List<MetricSample> export(MetricsCategory category) {
        List<MetricSample> list = new ArrayList<>();
        for (List<LongContainer<? extends Number>> containers : rtStats.values()) {
            for (LongContainer<? extends Number> container : containers) {
                MetricsKeyWrapper metricsKeyWrapper = container.getMetricsKeyWrapper();
                for (Metric key : container.keySet()) {
                    // Use keySet to obtain the original key instance reference of ConcurrentHashMap to avoid early
                    // recycling of the micrometer
                    list.add(new GaugeMetricSample<>(
                            metricsKeyWrapper.targetKey(),
                            metricsKeyWrapper.targetDesc(),
                            key.getTags(),
                            category,
                            key,
                            value -> container.getValueSupplier().apply(value)));
                }
            }
        }
        return list;
    }

    public List<LongContainer<? extends Number>> getRtStats() {
        return rtStats.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    private static class Action {
        private final BiConsumer<Long, Number> consumerFunc;
        private final Number initValue;

        public Action(BiConsumer<Long, Number> consumerFunc, Number initValue) {
            this.consumerFunc = consumerFunc;
            this.initValue = initValue;
        }

        public void run(Long responseTime) {
            consumerFunc.accept(responseTime, initValue);
        }
    }

    @Override
    public boolean calSamplesChanged() {
        // CAS to get and reset the flag in an atomic operation
        return samplesChanged.compareAndSet(true, false);
    }
}
