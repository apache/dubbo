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
import org.apache.dubbo.metrics.data.ApplicationStatComposite;
import org.apache.dubbo.metrics.data.BaseStatComposite;
import org.apache.dubbo.metrics.data.RtStatComposite;
import org.apache.dubbo.metrics.data.ServiceStatComposite;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.MetricsEventMulticaster;
import org.apache.dubbo.metrics.metadata.MetadataMetricsConstants;
import org.apache.dubbo.metrics.metadata.event.MetadataEvent;
import org.apache.dubbo.metrics.metadata.event.MetadataMetricsEventMulticaster;
import org.apache.dubbo.metrics.metadata.type.ApplicationType;
import org.apache.dubbo.metrics.metadata.type.ServiceType;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_PUSH;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_STORE_PROVIDER_INTERFACE;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_SUBSCRIBE;


/**
 * Registry implementation of {@link MetricsCollector}
 */
@Activate
public class MetadataMetricsCollector implements ApplicationMetricsCollector<ApplicationType, MetadataEvent> {

    private Boolean collectEnabled = null;
    private final BaseStatComposite stats;
    private final MetricsEventMulticaster metadataEventMulticaster;
    private final ApplicationModel applicationModel;

    public MetadataMetricsCollector(ApplicationModel applicationModel) {
        this.stats = new BaseStatComposite() {
            @Override
            protected void init(ApplicationStatComposite applicationStatComposite, ServiceStatComposite serviceStatComposite, RtStatComposite rtStatComposite) {
                applicationStatComposite.init(MetadataMetricsConstants.appKeys);
                serviceStatComposite.init(MetadataMetricsConstants.serviceKeys);
                rtStatComposite.init(OP_TYPE_PUSH, OP_TYPE_SUBSCRIBE, OP_TYPE_STORE_PROVIDER_INTERFACE);
            }
        };
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
            configManager.getMetrics().ifPresent(metricsConfig -> setCollectEnabled(metricsConfig.getEnableMetadata()));
        }
        return Optional.ofNullable(collectEnabled).orElse(true);
    }

    @Override
    public void increment(String applicationName, ApplicationType registryType) {
        this.stats.incrementApp(registryType.getMetricsKey(), applicationName,1);
    }

    public void incrementServiceKey(String applicationName, String serviceKey, ServiceType registryType, int size) {
        this.stats.incrementServiceKey(registryType.getMetricsKey(), applicationName, serviceKey, size);
    }

    @Override
    public void addApplicationRT(String applicationName, String registryOpType, Long responseTime) {
        stats.calcApplicationRt(applicationName, registryOpType, responseTime);
    }

    public void addServiceKeyRT(String applicationName, String serviceKey, String registryOpType, Long responseTime) {
        stats.calcServiceKeyRt(applicationName, serviceKey, registryOpType, responseTime);
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        if (!isCollectEnabled()) {
            return list;
        }
        list.addAll(stats.export(MetricsCategory.METADATA));
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
