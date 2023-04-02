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

package org.apache.dubbo.metrics.metadata.event;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.metadata.collector.MetadataMetricsCollector;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.rpc.model.ApplicationModel;

/**
 * Registry related events
 */
public class MetadataEvent extends TimeCounterEvent {
    private final MetadataMetricsCollector collector;

    public MetadataEvent(ApplicationModel applicationModel) {
        super(applicationModel);
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        if (beanFactory.isDestroyed()) {
            this.collector = null;
        } else {
            this.collector = beanFactory.getBean(MetadataMetricsCollector.class);
            super.setAvailable(this.collector != null && collector.isCollectEnabled());
        }
    }

    public ApplicationModel getSource() {
        return source;
    }

    public MetadataMetricsCollector getCollector() {
        return collector;
    }

    public enum ApplicationType {
        P_TOTAL(MetricsKey.METADATA_PUSH_METRIC_NUM),
        P_SUCCEED(MetricsKey.METADATA_PUSH_METRIC_NUM_SUCCEED),
        P_FAILED(MetricsKey.METADATA_PUSH_METRIC_NUM_FAILED),

        S_TOTAL(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM),
        S_SUCCEED(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED),
        S_FAILED(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_FAILED),

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

        S_P_TOTAL(MetricsKey.STORE_PROVIDER_METADATA),
        S_P_SUCCEED(MetricsKey.STORE_PROVIDER_METADATA_SUCCEED),
        S_P_FAILED(MetricsKey.STORE_PROVIDER_METADATA_FAILED),

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

    public static class PushEvent extends MetadataEvent {

        public PushEvent(ApplicationModel applicationModel) {
            super(applicationModel);
        }

    }

    public static class SubscribeEvent extends MetadataEvent {

        public SubscribeEvent(ApplicationModel applicationModel) {
            super(applicationModel);
        }

    }

    public static class StoreProviderMetadataEvent extends MetadataEvent {
        private final String serviceKey;

        public StoreProviderMetadataEvent(ApplicationModel applicationModel, String serviceKey) {
            super(applicationModel);
            this.serviceKey = serviceKey;
        }

        public String getServiceKey() {
            return serviceKey;
        }

    }

}
