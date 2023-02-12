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
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metrics.collector.ApplicationMetricsCollector;
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.MetricsEventMulticaster;
import org.apache.dubbo.metrics.listener.MetricsLifeListener;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.registry.collector.stat.RegistryStatComposite;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.metrics.registry.event.RegistryMetricsEventMulticaster;
import org.apache.dubbo.registry.client.migration.MigrationInvoker;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.directory.AbstractDirectory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Registry implementation of {@link MetricsCollector}
 */
@Activate
public class RegistryMetricsCollector implements ApplicationMetricsCollector<RegistryEvent.Type>, MetricsLifeListener<RegistryEvent> {

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

    public boolean isCollectEnabled() {
        if (collectEnabled == null) {
            ConfigManager configManager = applicationModel.getApplicationConfigManager();
            configManager.getMetrics().ifPresent(metricsConfig -> setCollectEnabled(metricsConfig.getEnableRegistry()));
        }
        return Optional.ofNullable(collectEnabled).orElse(false);
    }

    public void setNum(RegistryEvent.Type registryType, String applicationName, Map<String, Integer> lastNumMap) {
        lastNumMap.forEach((serviceKey, num) ->
            this.stats.setServiceKey(registryType, applicationName, serviceKey, num));
    }


    @Override
    public void increment(RegistryEvent.Type registryType, String applicationName) {
        this.stats.increment(registryType, applicationName);
    }

    @Override
    public void addRT(String applicationName, String registryOpType, Long responseTime) {
        stats.calcRt(applicationName, registryOpType, responseTime);
    }

    @Override
    public List<MetricSample> collect() {
        if (!isCollectEnabled()) {
            new ArrayList<>();
        }
        List<MetricSample> list = new ArrayList<>();
        list.addAll(stats.exportNumMetrics());
        list.addAll(stats.exportRtMetrics());
        //Dictionary url statistics
        statsDictionary();
        list.addAll(stats.exportSkMetrics());

        return list;
    }

    private void statsDictionary() {
        Collection<ConsumerModel> consumerModels = ApplicationModel.allConsumerModels();
        for (ConsumerModel consumerModel : consumerModels) {
            ReferenceConfigBase<?> referenceConfig = consumerModel.getReferenceConfig();
            if (!(referenceConfig instanceof ReferenceConfig)) {
                continue;
            }
            ReferenceConfig config = (ReferenceConfig) referenceConfig;
            Invoker invoker = config.getInvoker();
            if (!(invoker instanceof MigrationInvoker)) {
                continue;
            }
            AbstractDirectory directory = (AbstractDirectory) ((MigrationInvoker) invoker).getDirectory();
            this.stats.setServiceKey(RegistryEvent.Type.D_TOTAL, applicationModel.getApplicationName(), consumerModel.getServiceKey(), directory.getAllInvokers().size());
            this.stats.setServiceKey(RegistryEvent.Type.D_VALID, applicationModel.getApplicationName(), consumerModel.getServiceKey(), directory.getValidInvokers().size());
            this.stats.setServiceKey(RegistryEvent.Type.D_UN_VALID, applicationModel.getApplicationName(), consumerModel.getServiceKey(), directory.getDisabledInvokers().size());
        }
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
