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
package org.apache.dubbo.metrics.registry.collector;

import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.RegistryMetricsConstants;
import org.apache.dubbo.metrics.report.AbstractMetricsExport;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.metrics.MetricsConstants.SELF_INCREMENT_SIZE;

public class RegistryStatComposite extends AbstractMetricsExport {

    private final Map<MetricsKey, Map<ApplicationMetric, AtomicLong>> appStats = new ConcurrentHashMap<>();

    public RegistryStatComposite(ApplicationModel applicationModel) {
        super(applicationModel);
        init(RegistryMetricsConstants.REGISTER_LEVEL_APP_KEYS);
    }

    public void init(List<MetricsKey> appKeys) {
        if (CollectionUtils.isEmpty(appKeys)) {
            return;
        }
        appKeys.forEach(appKey -> appStats.put(appKey, new ConcurrentHashMap<>()));
    }

    @Override
    public List<MetricSample> export(MetricsCategory category) {
        List<MetricSample> list = new ArrayList<>();
        for (MetricsKey metricsKey : appStats.keySet()) {
            Map<ApplicationMetric, AtomicLong> stringAtomicLongMap = appStats.get(metricsKey);
            for (ApplicationMetric registerKeyMetric : stringAtomicLongMap.keySet()) {
                list.add(new GaugeMetricSample<>(metricsKey, registerKeyMetric.getTags(), category, stringAtomicLongMap, value -> value.get(registerKeyMetric).get()));
            }
        }
        return list;
    }

    public void incrMetricsNum(MetricsKey metricsKey, String name) {
        if (!appStats.containsKey(metricsKey)) {
            return;
        }
        ApplicationMetric applicationMetric = new ApplicationMetric(getApplicationModel());
        applicationMetric.setExtraInfo(MetricsSupport.customExtraInfo(RegistryConstants.REGISTRY_CLUSTER_KEY.toLowerCase(), name));
        appStats.get(metricsKey).computeIfAbsent(applicationMetric, k -> new AtomicLong(0L)).getAndAdd(SELF_INCREMENT_SIZE);
        MetricsSupport.fillZero(appStats);
    }

    public Map<MetricsKey, Map<ApplicationMetric, AtomicLong>> getAppStats() {
        return appStats;
    }
}
