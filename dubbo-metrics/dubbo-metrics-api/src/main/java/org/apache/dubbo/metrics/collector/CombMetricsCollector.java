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

import org.apache.dubbo.metrics.data.BaseStatComposite;
import org.apache.dubbo.metrics.event.MetricsEventMulticaster;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.listener.AbstractMetricsListener;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;

import java.util.List;

import static org.apache.dubbo.metrics.MetricsConstants.SELF_INCREMENT_SIZE;

public abstract class CombMetricsCollector<E extends TimeCounterEvent> extends AbstractMetricsListener<E> implements ApplicationMetricsCollector<E>, ServiceMetricsCollector<E> {

    private final BaseStatComposite stats;
    private MetricsEventMulticaster eventMulticaster;


    public CombMetricsCollector(BaseStatComposite stats) {
        this.stats = stats;
    }

    protected void setEventMulticaster(MetricsEventMulticaster eventMulticaster) {
        this.eventMulticaster = eventMulticaster;
    }

    @Override
    public void setNum(MetricsKey metricsKey, String applicationName, String serviceKey, int num) {
        this.stats.setServiceKey(metricsKey, applicationName, serviceKey, num);
    }

    @Override
    public void increment(String applicationName, MetricsKey metricsKey) {
        this.stats.incrementApp(metricsKey, applicationName, SELF_INCREMENT_SIZE);
    }

    public void increment(String applicationName, String serviceKey, MetricsKeyWrapper metricsKeyWrapper, int size) {
        this.stats.incrementServiceKey(metricsKeyWrapper, applicationName, serviceKey, size);
    }

    @Override
    public void addRt(String applicationName, String registryOpType, Long responseTime) {
        stats.calcApplicationRt(applicationName, registryOpType, responseTime);
    }

    public void addRt(String applicationName, String serviceKey, String registryOpType, Long responseTime) {
        stats.calcServiceKeyRt(applicationName, serviceKey, registryOpType, responseTime);
    }

    @SuppressWarnings({"rawtypes"})
    protected List<GaugeMetricSample> export(MetricsCategory category) {
        return stats.export(category);
    }

    public MetricsEventMulticaster getEventMulticaster() {
        return eventMulticaster;
    }

    @Override
    public void onEvent(TimeCounterEvent event) {
        eventMulticaster.publishEvent(event);
    }


    @Override
    public void onEventFinish(TimeCounterEvent event) {
        eventMulticaster.publishFinishEvent(event);
    }

    @Override
    public void onEventError(TimeCounterEvent event) {
        eventMulticaster.publishErrorEvent(event);
    }
}

