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

import java.util.Map;

/**
 * Registry related events
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

    public enum ApplicationType {
        R_TOTAL(MetricsKey.REGISTER_METRIC_REQUESTS),
        R_SUCCEED(MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED),
        R_FAILED(MetricsKey.REGISTER_METRIC_REQUESTS_FAILED),

        S_TOTAL(MetricsKey.SUBSCRIBE_METRIC_NUM),
        S_SUCCEED(MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED),
        S_FAILED(MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED),

        D_VALID(MetricsKey.DIRECTORY_METRIC_NUM_VALID),
        D_UN_VALID(MetricsKey.DIRECTORY_METRIC_NUM_UN_VALID),
        D_DISABLE(MetricsKey.DIRECTORY_METRIC_NUM_DISABLE),
        D_CURRENT(MetricsKey.DIRECTORY_METRIC_NUM_CURRENT, false),
        D_RECOVER_DISABLE(MetricsKey.DIRECTORY_METRIC_NUM_RECOVER_DISABLE),

        N_TOTAL(MetricsKey.NOTIFY_METRIC_REQUESTS),
        ;

        private final MetricsKey metricsKey;
        private final boolean isIncrement;


        ApplicationType(MetricsKey metricsKey) {
            this(metricsKey, true);
        }

        ApplicationType(MetricsKey metricsKey, boolean isIncrement) {
            this.metricsKey = metricsKey;
            this.isIncrement = isIncrement;
        }

        public MetricsKey getMetricsKey() {
            return metricsKey;
        }

        public boolean isIncrement() {
            return isIncrement;
        }
    }

    public enum ServiceType {

        N_LAST_NUM(MetricsKey.NOTIFY_METRIC_NUM_LAST),

        R_SERVICE_TOTAL(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS),
        R_SERVICE_SUCCEED(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED),
        R_SERVICE_FAILED(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED),

        S_SERVICE_TOTAL(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM),
        S_SERVICE_SUCCEED(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED),
        S_SERVICE_FAILED(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED),
        ;

        private final MetricsKey metricsKey;
        private final boolean isIncrement;


        ServiceType(MetricsKey metricsKey) {
            this(metricsKey, true);
        }

        ServiceType(MetricsKey metricsKey, boolean isIncrement) {
            this.metricsKey = metricsKey;
            this.isIncrement = isIncrement;
        }

        public MetricsKey getMetricsKey() {
            return metricsKey;
        }

        public boolean isIncrement() {
            return isIncrement;
        }
    }

    public static class MetricsApplicationRegisterEvent extends RegistryEvent {

        public MetricsApplicationRegisterEvent(ApplicationModel applicationModel, TimePair timePair) {
            super(applicationModel, timePair);
        }

    }

    public static class MetricsSubscribeEvent extends RegistryEvent {

        public MetricsSubscribeEvent(ApplicationModel applicationModel, TimePair timePair) {
            super(applicationModel, timePair);
        }

    }

    public static class MetricsNotifyEvent extends RegistryEvent {

        private final Map<String, Integer> lastNumMap;

        public MetricsNotifyEvent(ApplicationModel applicationModel, TimePair timePair, Map<String, Integer> lastNumMap) {
            super(applicationModel, timePair);
            this.lastNumMap = lastNumMap;
        }

        public Map<String, Integer> getLastNotifyNum() {
            return lastNumMap;
        }
    }

    public static class MetricsDirectoryEvent extends RegistryEvent {

        private final ApplicationType type;
        private final int size;

        public MetricsDirectoryEvent(ApplicationModel applicationModel, ApplicationType type) {
            this(applicationModel, type, 1);
        }

        public MetricsDirectoryEvent(ApplicationModel applicationModel, ApplicationType type, int size) {
            super(applicationModel, TimePair.empty());
            this.type = type;
            this.size = size;
        }

        public ApplicationType getType() {
            return type;
        }

        public int getSize() {
            return size;
        }
    }

    public static class MetricsServiceRegisterEvent extends RegistryEvent {

        private final int size;
        private final String serviceKey;

        public MetricsServiceRegisterEvent(ApplicationModel applicationModel, TimePair timePair, String serviceKey, int size) {
            super(applicationModel, timePair);
            this.size = size;
            this.serviceKey = serviceKey;
        }

        public int getSize() {
            return size;
        }

        public String getServiceKey() {
            return serviceKey;
        }
    }

    public static class MetricsServiceSubscribeEvent extends RegistryEvent {

        private final String uniqueServiceName;

        public MetricsServiceSubscribeEvent(ApplicationModel applicationModel, TimePair timePair, String uniqueServiceName) {
            super(applicationModel, timePair);
            this.uniqueServiceName = uniqueServiceName;
        }

        public String getUniqueServiceName() {
            return uniqueServiceName;
        }
    }
}
