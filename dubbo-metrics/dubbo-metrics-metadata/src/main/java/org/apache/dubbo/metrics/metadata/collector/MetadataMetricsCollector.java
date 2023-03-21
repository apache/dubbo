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

package org.apache.dubbo.metrics.metadata.collector;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metrics.collector.ApplicationMetricsCollector;
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.MetricsEventMulticaster;
import org.apache.dubbo.metrics.metadata.collector.stat.MetadataStatComposite;
import org.apache.dubbo.metrics.metadata.event.MetadataEvent;
import org.apache.dubbo.metrics.metadata.event.MetadataMetricsEventMulticaster;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Registry implementation of {@link MetricsCollector}
 */
@Activate
public class MetadataMetricsCollector implements ApplicationMetricsCollector<MetadataEvent.Type, MetadataEvent> {

    private Boolean collectEnabled = null;
    private final MetadataStatComposite stats;
    private final MetricsEventMulticaster metadataEventMulticaster;
    private final ApplicationModel applicationModel;

    public MetadataMetricsCollector(ApplicationModel applicationModel) {
        this.stats = new MetadataStatComposite();
        this.metadataEventMulticaster = new MetadataMetricsEventMulticaster();
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

    @Override
    public void increment(String applicationName, MetadataEvent.Type registryType) {
        this.stats.increment(registryType, applicationName);
    }

    @Override
    public void addApplicationRT(String applicationName, String registryOpType, Long responseTime) {
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

        return list;
    }

    @Override
    public boolean isSupport(MetricsEvent event) {
        return event instanceof MetadataEvent;
    }

    @Override
    public void onEvent(MetadataEvent event) {
        metadataEventMulticaster.publishEvent(event);
    }


    @Override
    public void onEventFinish(MetadataEvent event) {
        metadataEventMulticaster.publishFinishEvent(event);
    }

    @Override
    public void onEventError(MetadataEvent event) {
        metadataEventMulticaster.publishErrorEvent(event);
    }
}
