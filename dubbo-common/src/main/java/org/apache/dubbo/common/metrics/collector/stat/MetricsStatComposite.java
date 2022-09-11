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

package org.apache.dubbo.common.metrics.collector.stat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.metrics.event.MetricsEvent;
import org.apache.dubbo.common.metrics.event.RequestEvent;
import org.apache.dubbo.common.metrics.listener.MetricsListener;
import org.apache.dubbo.common.metrics.model.MethodMetric;
public class MetricsStatComposite{

    public Map<StatType, MetricsStatHandler> stats = new ConcurrentHashMap<>();
    private final String applicationName;
    private final List<MetricsListener> listeners;
    private static volatile MetricsStatComposite INSTANCE;

    public MetricsStatComposite(String applicationName, List<MetricsListener> listeners){
        this.applicationName = applicationName;
        this.listeners = listeners;
        this.init();
    }

    public MetricsStatHandler getHandler(StatType statType) {
        return stats.get(statType);
    }

    private void init() {
        stats.put(StatType.TOTAL, new DefaultMetricStatHandler(applicationName){
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.TOTAL));
            }
        });

        stats.put(StatType.SUCCEED, new DefaultMetricStatHandler(applicationName) {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.SUCCEED));
            }
        });

        stats.put(StatType.FAILED, new DefaultMetricStatHandler(applicationName) {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.FAILED));
            }
        });

        stats.put(StatType.BUSINESS_FAILED, new DefaultMetricStatHandler(applicationName) {
            @Override
            public void doNotify(MethodMetric metric) {
                publishEvent(new RequestEvent(metric, RequestEvent.Type.BUSINESS_FAILED));
            }
        });

        stats.put(StatType.PROCESSING, new DefaultMetricStatHandler(applicationName));
    }

    private void publishEvent(MetricsEvent event) {
        for (MetricsListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
