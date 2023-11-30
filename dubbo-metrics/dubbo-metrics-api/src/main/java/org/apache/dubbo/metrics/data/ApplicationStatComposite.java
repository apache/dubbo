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
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.report.AbstractMetricsExport;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application-level data container, for the initialized MetricsKey,
 * different from the null value of the Map type (the key is not displayed when there is no data),
 * the key is displayed and the initial data is 0 value of the AtomicLong type
 */
public class ApplicationStatComposite extends AbstractMetricsExport {

    public ApplicationStatComposite(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    private final Map<MetricsKey, AtomicLong> applicationNumStats = new ConcurrentHashMap<>();

    private final AtomicBoolean samplesChanged = new AtomicBoolean(true);

    public void init(List<MetricsKey> appKeys) {
        if (CollectionUtils.isEmpty(appKeys)) {
            return;
        }
        appKeys.forEach(appKey -> {
            applicationNumStats.put(appKey, new AtomicLong(0L));
        });
        samplesChanged.set(true);
    }

    public void incrementSize(MetricsKey metricsKey, int size) {
        if (!applicationNumStats.containsKey(metricsKey)) {
            return;
        }
        applicationNumStats.get(metricsKey).getAndAdd(size);
    }

    public List<MetricSample> export(MetricsCategory category) {
        List<MetricSample> list = new ArrayList<>();
        for (MetricsKey type : applicationNumStats.keySet()) {
            list.add(convertToSample(type, category, applicationNumStats.get(type)));
        }
        return list;
    }

    @SuppressWarnings({"rawtypes"})
    private GaugeMetricSample convertToSample(MetricsKey type, MetricsCategory category, AtomicLong targetNumber) {
        return new GaugeMetricSample<>(
                type, MetricsSupport.applicationTags(getApplicationModel()), category, targetNumber, AtomicLong::get);
    }

    public Map<MetricsKey, AtomicLong> getApplicationNumStats() {
        return applicationNumStats;
    }

    @Override
    public boolean calSamplesChanged() {
        // CAS to get and reset the flag in an atomic operation
        return samplesChanged.compareAndSet(true, false);
    }
}
