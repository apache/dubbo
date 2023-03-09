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

package org.apache.dubbo.metrics.collector;

import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.ConfigCenterMetric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.metrics.model.MetricsCategory.CONFIGCENTER;

/**
 * @author guiyi.yuan
 * @date 2/8/23 10:06 PM
 * @description Metrics collector for Config-Center
 */
public class ConfigCenterMetricsCollector implements MetricsCollector {

    private Boolean collectEnabled = null;
    private final ApplicationModel applicationModel;

    private final Map<ConfigCenterMetric, AtomicLong> updatedMetrics = new ConcurrentHashMap<>();

    public ConfigCenterMetricsCollector(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
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
            configManager.getMetrics().ifPresent(metricsConfig -> setCollectEnabled(metricsConfig.getEnableMetadataMetrics()));
        }
        return Optional.ofNullable(collectEnabled).orElse(false);
    }

    public void increase4Initialized(String key, String group, String protocol, String applicationName, int count) {
        if (count <= 0) {
            return;
        }
        ConfigCenterMetric metric = new ConfigCenterMetric(applicationName, key, group, protocol, ConfigChangeType.ADDED.name());
        AtomicLong aLong = updatedMetrics.computeIfAbsent(metric, k -> new AtomicLong(0L));
        aLong.addAndGet(count);
    }

    public void increaseUpdated(String protocol, String applicationName, ConfigChangedEvent event) {
        ConfigCenterMetric metric = new ConfigCenterMetric(applicationName, event.getKey(), event.getGroup(), protocol, event.getChangeType().name());
        AtomicLong count = updatedMetrics.computeIfAbsent(metric, k -> new AtomicLong(0L));
        count.incrementAndGet();
    }

    @Override
    public List<MetricSample> collect() {
        // Add metrics to reporter
        List<MetricSample> list = new ArrayList<>();
        collect(list);
        return list;
    }

    private void collect(List<MetricSample> list) {
        updatedMetrics.forEach((k, v) -> list.add(new GaugeMetricSample<>(MetricsKey.CONFIGCENTER_METRIC_TOTAL, k.getTags(), CONFIGCENTER, v, AtomicLong::get)));
    }

}
