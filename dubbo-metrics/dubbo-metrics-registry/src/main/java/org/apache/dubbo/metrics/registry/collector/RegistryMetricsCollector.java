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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metrics.collector.ApplicationMetricsCollector;
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.MetricsEventMulticaster;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.collector.stat.RegistryStatComposite;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.metrics.registry.event.RegistryMetricsEventMulticaster;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Registry implementation of {@link MetricsCollector}
 */
@Activate
public class RegistryMetricsCollector implements ApplicationMetricsCollector<RegistryEvent.Type, RegistryEvent> {

    private Boolean collectEnabled = null;
    private final RegistryStatComposite stats;
    private final MetricsEventMulticaster registryMulticaster;
    private final ApplicationModel applicationModel;

    public RegistryMetricsCollector(ApplicationModel applicationModel) {
        this.stats = new RegistryStatComposite();
        this.registryMulticaster = new RegistryMetricsEventMulticaster();
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
            configManager.getMetrics().ifPresent(metricsConfig -> setCollectEnabled(metricsConfig.getEnableRegistryMetrics()));
        }
        return Optional.ofNullable(collectEnabled).orElse(false);
    }

    public void setNum(RegistryEvent.Type registryType, String applicationName, Map<String, Integer> lastNumMap) {
        lastNumMap.forEach((serviceKey, num) ->
            this.stats.setServiceKey(registryType, applicationName, serviceKey, num));
    }

    public void setNum(RegistryEvent.Type registryType, String applicationName, Integer num) {
        this.stats.setApplicationKey(registryType, applicationName, num);
    }


    @Override
    public void increment(String applicationName, RegistryEvent.Type registryType) {
        this.stats.increment(registryType, applicationName);
    }

    @Override
    public void addRT(String applicationName, String registryOpType, Long responseTime) {
        stats.calcRt(applicationName, registryOpType, responseTime);
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        if (!isCollectEnabled()) {
           return list;
        }
        list.addAll(stats.exportNumMetrics());
        list.addAll(stats.exportRtMetrics());
        list.addAll(stats.exportSkMetrics());

        return list;
    }

    @Override
    public boolean isSupport(MetricsEvent event) {
        return event instanceof RegistryEvent;
    }

    @Override
    public void onEvent(RegistryEvent event) {
        registryMulticaster.publishEvent(event);
    }


    @Override
    public void onEventFinish(RegistryEvent event) {
        registryMulticaster.publishFinishEvent(event);
    }

    @Override
    public void onEventError(RegistryEvent event) {
        registryMulticaster.publishErrorEvent(event);
    }
}
