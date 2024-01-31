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
package org.apache.dubbo.metrics.config.collector;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metrics.collector.CombMetricsCollector;
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.config.event.ConfigCenterEvent;
import org.apache.dubbo.metrics.config.event.ConfigCenterSubDispatcher;
import org.apache.dubbo.metrics.model.ConfigCenterMetric;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.metrics.model.MetricsCategory.CONFIGCENTER;

/**
 * Config center implementation of {@link MetricsCollector}
 */
@Activate
public class ConfigCenterMetricsCollector extends CombMetricsCollector<ConfigCenterEvent> {

    private Boolean collectEnabled = null;
    private final ApplicationModel applicationModel;
    private final AtomicBoolean samplesChanged = new AtomicBoolean(true);

    private final Map<ConfigCenterMetric, AtomicLong> updatedMetrics = new ConcurrentHashMap<>();

    public ConfigCenterMetricsCollector(ApplicationModel applicationModel) {
        super(null);
        this.applicationModel = applicationModel;
        super.setEventMulticaster(new ConfigCenterSubDispatcher(this));
    }

    public void setCollectEnabled(Boolean collectEnabled) {
        if (collectEnabled != null) {
            this.collectEnabled = collectEnabled;
        }
    }

    @Override
    public boolean isCollectEnabled() {
        if (collectEnabled == null) {
            ConfigManager configManager = applicationModel.getApplicationConfigManager();
            configManager.getMetrics().ifPresent(metricsConfig -> setCollectEnabled(metricsConfig.getEnableMetadata()));
        }
        return Optional.ofNullable(collectEnabled).orElse(true);
    }

    public void increase(String key, String group, String protocol, String changeTypeName, int size) {
        if (!isCollectEnabled()) {
            return;
        }
        ConfigCenterMetric metric =
                new ConfigCenterMetric(applicationModel.getApplicationName(), key, group, protocol, changeTypeName);
        AtomicLong metrics = updatedMetrics.get(metric);
        if (metrics == null) {
            metrics = updatedMetrics.computeIfAbsent(metric, k -> new AtomicLong(0L));
            samplesChanged.set(true);
        }
        metrics.addAndGet(size);
    }

    @Override
    public List<MetricSample> collect() {
        // Add metrics to reporter
        List<MetricSample> list = new ArrayList<>();
        if (!isCollectEnabled()) {
            return list;
        }
        updatedMetrics.forEach((k, v) -> list.add(new GaugeMetricSample<>(
                MetricsKey.CONFIGCENTER_METRIC_TOTAL, k.getTags(), CONFIGCENTER, v, AtomicLong::get)));
        return list;
    }

    @Override
    public boolean calSamplesChanged() {
        // CAS to get and reset the flag in an atomic operation
        return samplesChanged.compareAndSet(true, false);
    }
}
