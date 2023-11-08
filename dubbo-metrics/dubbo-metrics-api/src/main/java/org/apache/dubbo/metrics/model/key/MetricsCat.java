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
package org.apache.dubbo.metrics.model.key;

import org.apache.dubbo.metrics.collector.CombMetricsCollector;
import org.apache.dubbo.metrics.listener.AbstractMetricsKeyListener;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The behavior wrapper class of MetricsKey,
 * which saves the complete content of the key {@link MetricsPlaceValue},
 * the corresponding collector {@link CombMetricsCollector},
 * and the event listener (generate function) at the key level {@link AbstractMetricsKeyListener}
 */
public class MetricsCat {

    private MetricsPlaceValue placeType;
    private final Function<CombMetricsCollector, AbstractMetricsKeyListener> eventFunc;

    /**
     * @param metricsKey The key corresponding to the listening event, not necessarily the export key(export key may be dynamic)
     * @param biFunc Binary function, corresponding to MetricsKey with less content, corresponding to post event
     */
    public MetricsCat(
            MetricsKey metricsKey, BiFunction<MetricsKey, CombMetricsCollector, AbstractMetricsKeyListener> biFunc) {
        this.eventFunc = collector -> biFunc.apply(metricsKey, collector);
    }

    /**
     * @param tpFunc   Ternary function, corresponding to finish and error events, because an additional record rt is required, and the type of metricsKey is required
     */
    public MetricsCat(
            MetricsKey metricsKey,
            TpFunction<MetricsKey, MetricsPlaceValue, CombMetricsCollector, AbstractMetricsKeyListener> tpFunc) {
        this.eventFunc = collector -> tpFunc.apply(metricsKey, placeType, collector);
    }

    public MetricsCat setPlaceType(MetricsPlaceValue placeType) {
        this.placeType = placeType;
        return this;
    }

    public Function<CombMetricsCollector, AbstractMetricsKeyListener> getEventFunc() {
        return eventFunc;
    }

    @FunctionalInterface
    public interface TpFunction<T, U, K, R> {
        R apply(T t, U u, K k);
    }
}
