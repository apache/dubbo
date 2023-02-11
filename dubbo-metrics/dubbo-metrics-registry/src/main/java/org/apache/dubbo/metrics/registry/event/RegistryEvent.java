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

package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.TimeCounter;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.TimePair;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;

/**
 * Registry related events
 *
 * @param <S> context type
 */
public class RegistryEvent extends MetricsEvent implements TimeCounter {
    private final TimePair timePair;
    private final RegistryMetricsCollector collector;
    private final boolean available;

    public RegistryEvent(ApplicationModel applicationModel, TimePair timePair) {
        super(applicationModel);
        this.timePair = timePair;
        this.collector = applicationModel.getBeanFactory().getBean(RegistryMetricsCollector.class);
        this.available = this.collector != null && collector.isCollectEnabled();
    }

    public ApplicationModel getSource() {
        return (ApplicationModel) source;
    }

    public RegistryMetricsCollector getCollector() {
        return collector;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public TimePair getTimePair() {
        return timePair;
    }

    public enum Type {
        R_TOTAL(MetricsKey.REGISTER_METRIC_REQUESTS),
        R_SUCCEED(MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED),
        R_FAILED(MetricsKey.REGISTER_METRIC_REQUESTS_FAILED),

        S_TOTAL(MetricsKey.SUBSCRIBE_METRIC_REQUESTS),
        S_SUCCEED(MetricsKey.SUBSCRIBE_METRIC_REQUESTS_SUCCEED),
        S_FAILED(MetricsKey.SUBSCRIBE_METRIC_REQUESTS_FAILED),

        N_TOTAL(MetricsKey.NOTIFY_METRIC_REQUESTS),
        ;


        private final MetricsKey metricsKey;


        Type(MetricsKey metricsKey) {
            this.metricsKey = metricsKey;
        }

        public MetricsKey getMetricsKey() {
            return metricsKey;
        }
    }
}
