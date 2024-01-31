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

import org.apache.dubbo.metrics.collector.CombMetricsCollector;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;

/**
 * App-level listener type, in most cases, can use the static method
 * to produce an anonymous listener for general monitoring
 */
public class MetricsApplicationListener extends AbstractMetricsKeyListener {

    public MetricsApplicationListener(MetricsKey metricsKey) {
        super(metricsKey);
    }

    /**
     * Perform auto-increment on the monitored key,
     * Can use a custom listener instead of this generic operation
     *
     * @param metricsKey Monitor key
     * @param collector  Corresponding collector
     */
    public static AbstractMetricsKeyListener onPostEventBuild(
            MetricsKey metricsKey, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onEvent(metricsKey, event -> collector.increment(metricsKey));
    }

    /**
     * To end the monitoring normally, in addition to increasing the number of corresponding indicators,
     * use the introspection method to calculate the relevant rt indicators
     *
     * @param metricsKey Monitor key
     * @param collector  Corresponding collector
     */
    public static AbstractMetricsKeyListener onFinishEventBuild(
            MetricsKey metricsKey, MetricsPlaceValue placeType, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onFinish(metricsKey, event -> {
            collector.increment(metricsKey);
            collector.addApplicationRt(placeType.getType(), event.getTimePair().calc());
        });
    }

    /**
     * Similar to onFinishEventBuild
     */
    public static AbstractMetricsKeyListener onErrorEventBuild(
            MetricsKey metricsKey, MetricsPlaceValue placeType, CombMetricsCollector<?> collector) {
        return AbstractMetricsKeyListener.onError(metricsKey, event -> {
            collector.increment(metricsKey);
            collector.addApplicationRt(placeType.getType(), event.getTimePair().calc());
        });
    }
}
