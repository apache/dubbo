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
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metrics.collector.CombMetricsCollector;
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.data.ApplicationStatComposite;
import org.apache.dubbo.metrics.data.BaseStatComposite;
import org.apache.dubbo.metrics.data.RtStatComposite;
import org.apache.dubbo.metrics.data.ServiceStatComposite;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.ServiceKeyMetric;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.RegistryMetricsConstants;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.metrics.registry.event.RegistrySubDispatcher;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.Collections;
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
public class RegistryMetricsCollector extends CombMetricsCollector<RegistryEvent> {

    private Boolean collectEnabled = null;
    private final ApplicationModel applicationModel;
    private final RegistryStatComposite internalStat;

    public RegistryMetricsCollector(ApplicationModel applicationModel) {
        super(new BaseStatComposite(applicationModel) {
            @Override
            protected void init(ApplicationStatComposite applicationStatComposite) {
                super.init(applicationStatComposite);
                applicationStatComposite.init(RegistryMetricsConstants.APP_LEVEL_KEYS);
            }

            @Override
            protected void init(ServiceStatComposite serviceStatComposite) {
                super.init(serviceStatComposite);
                serviceStatComposite.initWrapper(RegistryMetricsConstants.SERVICE_LEVEL_KEYS);
            }

            @Override
            protected void init(RtStatComposite rtStatComposite) {
                super.init(rtStatComposite);
                rtStatComposite.init(
                        OP_TYPE_REGISTER,
                        OP_TYPE_SUBSCRIBE,
                        OP_TYPE_NOTIFY,
                        OP_TYPE_REGISTER_SERVICE,
                        OP_TYPE_SUBSCRIBE_SERVICE);
            }
        });
        super.setEventMulticaster(new RegistrySubDispatcher(this));
        internalStat = new RegistryStatComposite(applicationModel);
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

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        if (!isCollectEnabled()) {
            return list;
        }
        list.addAll(super.export(MetricsCategory.REGISTRY));
        list.addAll(internalStat.export(MetricsCategory.REGISTRY));
        return list;
    }

    public void incrMetricsNum(MetricsKey metricsKey, List<String> registryClusterNames) {
        registryClusterNames.forEach(name -> internalStat.incrMetricsNum(metricsKey, name));
    }

    public void incrRegisterFinishNum(
            MetricsKey metricsKey, String registryOpType, List<String> registryClusterNames, Long responseTime) {
        registryClusterNames.forEach(name -> {
            ApplicationMetric applicationMetric = new ApplicationMetric(applicationModel);
            applicationMetric.setExtraInfo(
                    Collections.singletonMap(RegistryConstants.REGISTRY_CLUSTER_KEY.toLowerCase(), name));
            internalStat.incrMetricsNum(metricsKey, name);
            getStats().getRtStatComposite().calcServiceKeyRt(registryOpType, responseTime, applicationMetric);
        });
    }

    public void incrServiceRegisterNum(
            MetricsKeyWrapper wrapper, String serviceKey, List<String> registryClusterNames, int size) {
        registryClusterNames.forEach(name -> stats.incrementServiceKey(
                wrapper,
                serviceKey,
                Collections.singletonMap(RegistryConstants.REGISTRY_CLUSTER_KEY.toLowerCase(), name),
                size));
    }

    public void incrServiceRegisterFinishNum(
            MetricsKeyWrapper wrapper,
            String serviceKey,
            List<String> registryClusterNames,
            int size,
            Long responseTime) {
        registryClusterNames.forEach(name -> {
            Map<String, String> extraInfo =
                    Collections.singletonMap(RegistryConstants.REGISTRY_CLUSTER_KEY.toLowerCase(), name);
            ServiceKeyMetric serviceKeyMetric = new ServiceKeyMetric(applicationModel, serviceKey);
            serviceKeyMetric.setExtraInfo(extraInfo);
            stats.incrementServiceKey(wrapper, serviceKey, extraInfo, size);
            getStats().getRtStatComposite().calcServiceKeyRt(wrapper.getType(), responseTime, serviceKeyMetric);
        });
    }

    public void setNum(MetricsKeyWrapper metricsKey, String serviceKey, int num, Map<String, String> attachments) {
        this.stats.setServiceKey(metricsKey, serviceKey, num, attachments);
    }

    @Override
    public boolean calSamplesChanged() {
        // Should ensure that all the stat's samplesChanged have been compareAndSet, and cannot flip the `or` logic
        boolean changed = stats.calSamplesChanged();
        changed = internalStat.calSamplesChanged() || changed;
        return changed;
    }
}
