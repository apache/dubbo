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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metrics.exception.MetricsNeverHappenException;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.report.AbstractMetricsExport;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Method-level data container,
 * if there is no actual call to the existing call method,
 * the key will not be displayed when exporting (to be optimized)
 */
public class MethodStatComposite extends AbstractMetricsExport {

    private final AtomicBoolean samplesChanged = new AtomicBoolean(true);

    public MethodStatComposite(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    private final Map<MetricsKeyWrapper, Map<MethodMetric, AtomicLong>> methodNumStats = new ConcurrentHashMap<>();

    public void initWrapper(List<MetricsKeyWrapper> metricsKeyWrappers) {
        if (CollectionUtils.isEmpty(metricsKeyWrappers)) {
            return;
        }
        metricsKeyWrappers.forEach(appKey -> {
            methodNumStats.put(appKey, new ConcurrentHashMap<>());
        });
        samplesChanged.set(true);
    }

    public void initMethodKey(MetricsKeyWrapper wrapper, Invocation invocation) {
        if (!methodNumStats.containsKey(wrapper)) {
            return;
        }

        methodNumStats
                .get(wrapper)
                .computeIfAbsent(
                        new MethodMetric(getApplicationModel(), invocation, getServiceLevel()),
                        k -> new AtomicLong(0L));
        samplesChanged.set(true);
    }

    public void incrementMethodKey(MetricsKeyWrapper wrapper, MethodMetric methodMetric, int size) {
        if (!methodNumStats.containsKey(wrapper)) {
            return;
        }
        AtomicLong stat = methodNumStats.get(wrapper).get(methodMetric);
        if (stat == null) {
            methodNumStats.get(wrapper).computeIfAbsent(methodMetric, (k) -> new AtomicLong(0L));
            samplesChanged.set(true);
            stat = methodNumStats.get(wrapper).get(methodMetric);
        }
        stat.getAndAdd(size);
        //        MetricsSupport.fillZero(methodNumStats);
    }

    public List<MetricSample> export(MetricsCategory category) {
        List<MetricSample> list = new ArrayList<>();
        for (MetricsKeyWrapper wrapper : methodNumStats.keySet()) {
            Map<MethodMetric, AtomicLong> stringAtomicLongMap = methodNumStats.get(wrapper);
            for (MethodMetric methodMetric : stringAtomicLongMap.keySet()) {
                if (wrapper.getSampleType() == MetricSample.Type.COUNTER) {
                    list.add(new CounterMetricSample<>(
                            wrapper, methodMetric.getTags(), category, stringAtomicLongMap.get(methodMetric)));
                } else if (wrapper.getSampleType() == MetricSample.Type.GAUGE) {
                    list.add(new GaugeMetricSample<>(
                            wrapper, methodMetric.getTags(), category, stringAtomicLongMap, value -> value.get(
                                            methodMetric)
                                    .get()));
                } else {
                    throw new MetricsNeverHappenException("Unsupported metricSample type: " + wrapper.getSampleType());
                }
            }
        }
        return list;
    }

    @Override
    public boolean calSamplesChanged() {
        // CAS to get and reset the flag in an atomic operation
        return samplesChanged.compareAndSet(true, false);
    }
}
