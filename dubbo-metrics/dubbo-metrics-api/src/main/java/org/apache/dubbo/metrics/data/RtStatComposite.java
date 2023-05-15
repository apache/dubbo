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

import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.metrics.model.MetricsCategory;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.stream.Collectors;

/**
 * The data container of the rt dimension, including application, service, and method levels,
 * if there is no actual call to the existing call method,
 * the key will not be displayed when exporting (to be optimized)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RtStatComposite extends AbstractMetricsExport {

    public RtStatComposite(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    private final List<LongContainer<? extends Number>> rtStats = new ArrayList<>();

    public void init(MetricsPlaceValue... placeValues) {
        if (placeValues == null) {
            return;
        }
        Arrays.stream(placeValues).forEach(metricsPlaceType -> rtStats.addAll(initStats(metricsPlaceType)));
    }

    private List<LongContainer<? extends Number>> initStats(MetricsPlaceValue placeValue) {
        List<LongContainer<? extends Number>> singleRtStats = new ArrayList<>();
        singleRtStats.add(new AtomicLongContainer(new MetricsKeyWrapper(MetricsKey.METRIC_RT_LAST, placeValue)));
        singleRtStats.add(new LongAccumulatorContainer(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MIN, placeValue), new LongAccumulator(Long::min, Long.MAX_VALUE)));
        singleRtStats.add(new LongAccumulatorContainer(new MetricsKeyWrapper(MetricsKey.METRIC_RT_MAX, placeValue), new LongAccumulator(Long::max, Long.MIN_VALUE)));
        singleRtStats.add(new AtomicLongContainer(new MetricsKeyWrapper(MetricsKey.METRIC_RT_SUM, placeValue), (responseTime, longAccumulator) -> longAccumulator.addAndGet(responseTime)));
        // AvgContainer is a special counter that stores the number of times but outputs function of sum/times
        AtomicLongContainer avgContainer = new AtomicLongContainer(new MetricsKeyWrapper(MetricsKey.METRIC_RT_AVG, placeValue), (k, v) -> v.incrementAndGet());
        avgContainer.setValueSupplier(applicationName -> {
            LongContainer<? extends Number> totalContainer = rtStats.stream().filter(longContainer -> longContainer.isKeyWrapper(MetricsKey.METRIC_RT_SUM, placeValue.getType())).findFirst().get();
            AtomicLong totalRtTimes = avgContainer.get(applicationName);
            AtomicLong totalRtSum = (AtomicLong) totalContainer.get(applicationName);
            return totalRtSum.get() / totalRtTimes.get();
        });
        singleRtStats.add(avgContainer);
        return singleRtStats;
    }

    public void calcApplicationRt(String registryOpType, Long responseTime) {
        for (LongContainer container : rtStats.stream().filter(longContainer -> longContainer.specifyType(registryOpType)).collect(Collectors.toList())) {
            Number current = (Number) ConcurrentHashMapUtils.computeIfAbsent(container, getAppName(), container.getInitFunc());
            container.getConsumerFunc().accept(responseTime, current);
        }
    }

    public void calcServiceKeyRt(String serviceKey, String registryOpType, Long responseTime) {
        for (LongContainer container : rtStats.stream().filter(longContainer -> longContainer.specifyType(registryOpType)).collect(Collectors.toList())) {
            Number current = (Number) ConcurrentHashMapUtils.computeIfAbsent(container, serviceKey, container.getInitFunc());
            container.getConsumerFunc().accept(responseTime, current);
        }
    }

    public void calcMethodKeyRt(Invocation invocation, String registryOpType, Long responseTime) {
        for (LongContainer container : rtStats.stream().filter(longContainer -> longContainer.specifyType(registryOpType)).collect(Collectors.toList())) {
            Number current = (Number) ConcurrentHashMapUtils.computeIfAbsent(container, invocation.getServiceName() + "_" + invocation.getMethodName(), container.getInitFunc());
            container.getConsumerFunc().accept(responseTime, current);
        }
    }

    public List<MetricSample> export(MetricsCategory category) {
        List<MetricSample> list = new ArrayList<>();
        for (LongContainer<? extends Number> rtContainer : rtStats) {
            MetricsKeyWrapper metricsKeyWrapper = rtContainer.getMetricsKeyWrapper();
            for (Map.Entry<String, ? extends Number> entry : rtContainer.entrySet()) {
                list.add(new GaugeMetricSample<>(metricsKeyWrapper.targetKey(), metricsKeyWrapper.targetDesc(), metricsKeyWrapper.tagName(getApplicationModel(), entry.getKey()), category, entry.getKey().intern(), value -> rtContainer.getValueSupplier().apply(value.intern())));
            }
        }
        return list;
    }

    public List<LongContainer<? extends Number>> getRtStats() {
        return rtStats;
    }

}
