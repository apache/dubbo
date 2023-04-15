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
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.report.MetricsExport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ApplicationStatComposite implements MetricsExport {

    private final Map<MetricsKey, Map<String, AtomicLong>> applicationNumStats = new ConcurrentHashMap<>();

    public void init(List<MetricsKey> appKeys) {
        if (CollectionUtils.isEmpty(appKeys)) {
            return;
        }
        appKeys.forEach(appKey -> applicationNumStats.put(appKey, new ConcurrentHashMap<>()));
    }

    public void incrementSize(MetricsKey metricsKey, String applicationName, int size) {
        if (!applicationNumStats.containsKey(metricsKey)) {
            return;
        }
        applicationNumStats.get(metricsKey).computeIfAbsent(applicationName, k -> new AtomicLong(0L)).getAndAdd(size);
    }

    public void setApplicationKey(MetricsKey metricsKey, String applicationName, int num) {
        if (!applicationNumStats.containsKey(metricsKey)) {
            return;
        }
        applicationNumStats.get(metricsKey).computeIfAbsent(applicationName, k -> new AtomicLong(0L)).set(num);
    }


    @SuppressWarnings({"rawtypes"})
    public List<GaugeMetricSample> export(MetricsCategory category) {
        List<GaugeMetricSample> list = new ArrayList<>();
        for (MetricsKey type : applicationNumStats.keySet()) {
            Map<String, AtomicLong> stringAtomicLongMap = applicationNumStats.get(type);
            for (String applicationName : stringAtomicLongMap.keySet()) {
                list.add(convertToSample(applicationName, type, category, stringAtomicLongMap.get(applicationName)));
            }
        }
        return list;
    }

    @SuppressWarnings({"rawtypes"})
    private GaugeMetricSample convertToSample(String applicationName, MetricsKey type, MetricsCategory category, AtomicLong targetNumber) {
        return new GaugeMetricSample<>(type, MetricsSupport.applicationTags(applicationName), category, targetNumber, AtomicLong::get);
    }

    public Map<MetricsKey, Map<String, AtomicLong>> getApplicationNumStats() {
        return applicationNumStats;
    }

}
