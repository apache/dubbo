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
import org.apache.dubbo.metrics.data.ApplicationStatComposite;
import org.apache.dubbo.metrics.data.BaseStatComposite;
import org.apache.dubbo.metrics.data.RtStatComposite;
import org.apache.dubbo.metrics.data.ServiceStatComposite;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.MetricsEventMulticaster;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.RegistryMetricsConstants;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.metrics.registry.event.RegistryMetricsEventMulticaster;
import org.apache.dubbo.metrics.registry.event.type.ApplicationType;
import org.apache.dubbo.metrics.registry.event.type.ServiceType;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_NOTIFY;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE_SERVICE;


/**
 * Registry implementation of {@link MetricsCollector}
 */
@Activate
public class RegistryMetricsCollector implements ApplicationMetricsCollector<ApplicationType, RegistryEvent> {

    private Boolean collectEnabled = null;
    private final BaseStatComposite stats;
    private final MetricsEventMulticaster registryMulticaster;
    private final ApplicationModel applicationModel;

    public RegistryMetricsCollector(ApplicationModel applicationModel) {
        this.stats = new BaseStatComposite() {
            @Override
            protected void init(ApplicationStatComposite applicationStatComposite, ServiceStatComposite serviceStatComposite, RtStatComposite rtStatComposite) {
                applicationStatComposite.init(RegistryMetricsConstants.appKeys);
                serviceStatComposite.init(RegistryMetricsConstants.serviceKeys);
                rtStatComposite.init(OP_TYPE_REGISTER, OP_TYPE_SUBSCRIBE, OP_TYPE_NOTIFY, OP_TYPE_REGISTER_SERVICE, OP_TYPE_SUBSCRIBE_SERVICE);
            }
        };
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
            configManager.getMetrics().ifPresent(metricsConfig -> setCollectEnabled(metricsConfig.getEnableRegistry()));
        }
        return Optional.ofNullable(collectEnabled).orElse(true);
    }

    public void setNum(ServiceType registryType, String applicationName, Map<String, Integer> lastNumMap) {
        lastNumMap.forEach((serviceKey, num) ->
            this.stats.setServiceKey(registryType.getMetricsKey(), applicationName, serviceKey, num));
    }

    public void setNum(ApplicationType registryType, String applicationName, Integer num) {
        this.stats.setApplicationKey(registryType.getMetricsKey(), applicationName, num);
    }

    @Override
    public void increment(String applicationName, ApplicationType registryType) {
        this.stats.incrementApp(registryType.getMetricsKey(), applicationName, 1);
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
        list.addAll(stats.export(MetricsCategory.REGISTRY));
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
