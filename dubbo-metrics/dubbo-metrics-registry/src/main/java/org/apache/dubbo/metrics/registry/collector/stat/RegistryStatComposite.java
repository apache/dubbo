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
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.container.AtomicLongContainer;
import org.apache.dubbo.metrics.model.container.LongAccumulatorContainer;
import org.apache.dubbo.metrics.model.container.LongContainer;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.metrics.report.MetricsExport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * As a data aggregator, use internal data containers calculates and classifies
 * the registry data collected by {@link MetricsCollector MetricsCollector}, and
 * provides an {@link MetricsExport MetricsExport} interface for exporting standard output formats.
 */
public class RegistryStatComposite implements MetricsExport {


    public Map<RegistryEvent.ApplicationType, Map<String, AtomicLong>> applicationNumStats = new ConcurrentHashMap<>();
    public Map<RegistryEvent.ServiceType, Map<ServiceKeyMetric, AtomicLong>> serviceNumStats = new ConcurrentHashMap<>();
    public Map<RegistryEvent.ServiceType, Map<ServiceKeyMetric, AtomicLong>> skStats = new ConcurrentHashMap<>();
    public List<LongContainer<? extends Number>> appRtStats = new ArrayList<>();
    public List<LongContainer<? extends Number>> serviceRtStats = new ArrayList<>();
    public static String OP_TYPE_REGISTER = "register";
    public static String OP_TYPE_SUBSCRIBE = "subscribe";
    public static String OP_TYPE_NOTIFY = "notify";
    public static String OP_TYPE_REGISTER_SERVICE = "register.service";
    public static String OP_TYPE_SUBSCRIBE_SERVICE = "subscribe.service";

    public RegistryStatComposite() {
        for (RegistryEvent.ApplicationType type : RegistryEvent.ApplicationType.values()) {
            // Application key and increment val
            applicationNumStats.put(type, new ConcurrentHashMap<>());
        }

        for (RegistryEvent.ServiceType type : RegistryEvent.ServiceType.values()) {
            // Service key
            skStats.put(type, new ConcurrentHashMap<>());
        }


        appRtStats.addAll(initStats(OP_TYPE_REGISTER));
        appRtStats.addAll(initStats(OP_TYPE_SUBSCRIBE));
        appRtStats.addAll(initStats(OP_TYPE_NOTIFY));

        serviceRtStats.addAll(initStats(OP_TYPE_REGISTER_SERVICE));
        serviceRtStats.addAll(initStats(OP_TYPE_SUBSCRIBE_SERVICE));
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
            LongContainer<? extends Number> totalContainer = appRtStats.stream().filter(longContainer -> longContainer.isKeyWrapper(MetricsKey.METRIC_RT_SUM, registryOpType)).findFirst().get();
            AtomicLong totalRtTimes = avgContainer.get(applicationName);
            AtomicLong totalRtSum = (AtomicLong) totalContainer.get(applicationName);
            return totalRtSum.get() / totalRtTimes.get();
        });
        singleRtStats.add(avgContainer);
        return singleRtStats;
    }

    public void setApplicationKey(RegistryEvent.ApplicationType type, String applicationName, int num) {
        if (!applicationNumStats.containsKey(type)) {
            return;
        }
        applicationNumStats.get(type).computeIfAbsent(applicationName, k -> new AtomicLong(0L)).set(num);
    }

    public void setServiceKey(RegistryEvent.ServiceType type, String applicationName, String serviceKey, int num) {
        if (!skStats.containsKey(type)) {
            return;
        }
        skStats.get(type).computeIfAbsent(new ServiceKeyMetric(applicationName, serviceKey), k -> new AtomicLong(0L)).set(num);
    }

    public void increment(RegistryEvent.ApplicationType type, String applicationName) {
        incrementSize(type, applicationName, 1);
    }

    public void incrementServiceKey(RegistryEvent.ServiceType type, String applicationName, String serviceKey, int size) {
        if (!skStats.containsKey(type)) {
            return;
        }
        skStats.get(type).computeIfAbsent(new ServiceKeyMetric(applicationName, serviceKey), k -> new AtomicLong(0L)).getAndAdd(size);
    }

    public void incrementSize(RegistryEvent.ApplicationType type, String applicationName, int size) {
        if (!applicationNumStats.containsKey(type)) {
            return;
        }
        applicationNumStats.get(type).computeIfAbsent(applicationName, k -> new AtomicLong(0L)).getAndAdd(size);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void calcApplicationRt(String applicationName, String registryOpType, Long responseTime) {
        for (LongContainer container : appRtStats.stream().filter(longContainer -> longContainer.specifyType(registryOpType)).collect(Collectors.toList())) {
            Number current = (Number) ConcurrentHashMapUtils.computeIfAbsent(container, applicationName, container.getInitFunc());
            container.getConsumerFunc().accept(responseTime, current);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void calcServiceKeyRt(String applicationName, String serviceKey, String registryOpType, Long responseTime) {
        for (LongContainer container : serviceRtStats.stream().filter(longContainer -> longContainer.specifyType(registryOpType)).collect(Collectors.toList())) {
            Number current = (Number) ConcurrentHashMapUtils.computeIfAbsent(container, applicationName + "_" + serviceKey, container.getInitFunc());
            container.getConsumerFunc().accept(responseTime, current);
        }
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public List<GaugeMetricSample> exportNumMetrics() {
        List<GaugeMetricSample> list = new ArrayList<>();
        for (RegistryEvent.ApplicationType type : applicationNumStats.keySet()) {
            Map<String, AtomicLong> stringAtomicLongMap = applicationNumStats.get(type);
            for (String applicationName : stringAtomicLongMap.keySet()) {
                list.add(convertToSample(applicationName, type, MetricsCategory.REGISTRY, stringAtomicLongMap.get(applicationName)));
            }
        }
        return list;
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public List<GaugeMetricSample> exportRtMetrics() {
        List<GaugeMetricSample> result = new ArrayList<>();
        doExportRt(result, appRtStats, ApplicationMetric::getTagsByName);
        doExportRt(result, serviceRtStats, ApplicationMetric::getServiceTags);
        return result;
    }

    @SuppressWarnings({"rawtypes"})
    private void doExportRt(List<GaugeMetricSample> list, List<LongContainer<? extends Number>> rtStats, Function<String, Map<String, String>> tagNameFunc) {
        for (LongContainer<? extends Number> rtContainer : rtStats) {
            MetricsKeyWrapper metricsKeyWrapper = rtContainer.getMetricsKeyWrapper();
            for (Map.Entry<String, ? extends Number> entry : rtContainer.entrySet()) {
                list.add(new GaugeMetricSample<>(metricsKeyWrapper.targetKey(), metricsKeyWrapper.targetDesc(), tagNameFunc.apply(entry.getKey()), MetricsCategory.RT, entry.getKey().intern(), value -> rtContainer.getValueSupplier().apply(value.intern())));
            }
        }
    }

    @SuppressWarnings({"rawtypes"})
    public List<GaugeMetricSample> exportSkMetrics() {
        List<GaugeMetricSample> list = new ArrayList<>();
        for (RegistryEvent.ServiceType type : skStats.keySet()) {
            Map<ServiceKeyMetric, AtomicLong> stringAtomicLongMap = skStats.get(type);
            for (ServiceKeyMetric serviceKeyMetric : stringAtomicLongMap.keySet()) {
                list.add(new GaugeMetricSample<>(type.getMetricsKey(), serviceKeyMetric.getTags(), MetricsCategory.REGISTRY, stringAtomicLongMap, value -> value.get(serviceKeyMetric).get()));
            }
        }
        return list;
    }

    @SuppressWarnings({"rawtypes"})
    public GaugeMetricSample convertToSample(String applicationName, RegistryEvent.ApplicationType type, MetricsCategory category, AtomicLong targetNumber) {
        return new GaugeMetricSample<>(type.getMetricsKey(), ApplicationMetric.getTagsByName(applicationName), category, targetNumber, AtomicLong::get);
    }
}
