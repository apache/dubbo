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

public class MetricsCat {

    private MetricsPlaceValue placeType;
    private final Function<CombMetricsCollector, AbstractMetricsKeyListener> eventFunc;

    public MetricsCat(MetricsKey metricsKey, BiFunction<MetricsKey, CombMetricsCollector, AbstractMetricsKeyListener> biFunc) {
        this.eventFunc = collector -> biFunc.apply(metricsKey, collector);
    }

    /**
     * @param metricsKey The key that the current category listens to，not necessarily the export key(export key may be dynamic)
     * @param tpFunc     Build the func that outputs the MetricsListener by listen metricsKey
     */
    public MetricsCat(MetricsKey metricsKey, TpFunction<MetricsKey, MetricsPlaceValue, CombMetricsCollector, AbstractMetricsKeyListener> tpFunc) {
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
