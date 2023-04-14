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

package org.apache.dubbo.metrics.listener;

import org.apache.dubbo.metrics.collector.ServiceMetricsCollector;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsPlaceType;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.MetricsConstants.SELF_INCREMENT_SIZE;

public class MetricsServiceListener extends AbstractMetricsListener {

    public MetricsServiceListener(MetricsKey metricsKey) {
        super(metricsKey);
    }

    public static AbstractMetricsListener onPostEventBuild(MetricsKey metricsKey, ServiceMetricsCollector<TimeCounterEvent> collector) {
        return AbstractMetricsListener.onEvent(metricsKey,
            event -> collector.increment(event.appName(), event.getAttachmentValue(ATTACHMENT_KEY_SERVICE), metricsKey, SELF_INCREMENT_SIZE)
        );
    }

    public static AbstractMetricsListener onFinishEventBuild(MetricsKey metricsKey, MetricsPlaceType placeType, ServiceMetricsCollector<TimeCounterEvent> collector) {
        return AbstractMetricsListener.onFinish(metricsKey,
            event -> incrAndAddRt(metricsKey, placeType, collector, event)
        );
    }

    public static AbstractMetricsListener onErrorEventBuild(MetricsKey metricsKey, MetricsPlaceType placeType, ServiceMetricsCollector<TimeCounterEvent> collector) {
        return AbstractMetricsListener.onError(metricsKey,
            event -> incrAndAddRt(metricsKey, placeType, collector, event)
        );
    }

    private static void incrAndAddRt(MetricsKey metricsKey, MetricsPlaceType placeType, ServiceMetricsCollector<TimeCounterEvent> collector, TimeCounterEvent event) {
        collector.increment(event.appName(), event.getAttachmentValue(ATTACHMENT_KEY_SERVICE), metricsKey, SELF_INCREMENT_SIZE);
        collector.addRt(event.appName(), event.getAttachmentValue(ATTACHMENT_KEY_SERVICE), placeType.getType(), event.getTimePair().calc());
    }
}
