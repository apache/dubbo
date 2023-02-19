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

import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.TimeCounter;
import org.apache.dubbo.metrics.metadata.collector.MetadataMetricsCollector;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.TimePair;
import org.apache.dubbo.rpc.model.ApplicationModel;

/**
 * Registry related events
 */
public class MetadataEvent extends MetricsEvent implements TimeCounter {
    private final TimePair timePair;
    private final MetadataMetricsCollector collector;
    private final boolean available;

    public MetadataEvent(ApplicationModel applicationModel, TimePair timePair) {
        super(applicationModel);
        this.timePair = timePair;
        this.collector = applicationModel.getBeanFactory().getBean(MetadataMetricsCollector.class);
        this.available = this.collector != null && collector.isCollectEnabled();
    }

    public ApplicationModel getSource() {
        return (ApplicationModel) source;
    }

    public MetadataMetricsCollector getCollector() {
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
        P_TOTAL(MetricsKey.METADATA_PUSH_METRIC_NUM),
        P_SUCCEED(MetricsKey.METADATA_PUSH_METRIC_NUM_SUCCEED),
        P_FAILED(MetricsKey.METADATA_PUSH_METRIC_NUM_FAILED),

        S_TOTAL(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM),
        S_SUCCEED(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED),
        S_FAILED(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_FAILED),

        ;


        private final MetricsKey metricsKey;
        private final boolean isIncrement;


        Type(MetricsKey metricsKey) {
            this(metricsKey, true);
        }

        Type(MetricsKey metricsKey, boolean isIncrement) {
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

        public PushEvent(ApplicationModel applicationModel, TimePair timePair) {
            super(applicationModel, timePair);
        }

    }

    public static class SubscribeEvent extends MetadataEvent {

        public SubscribeEvent(ApplicationModel applicationModel, TimePair timePair) {
            super(applicationModel, timePair);
        }

    }

}
