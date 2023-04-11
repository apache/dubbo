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

package org.apache.dubbo.metrics.model.sample;

import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.key.MetricsKey;

import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/**
 * GaugeMetricSample.
 */
public class GaugeMetricSample<T> extends MetricSample {

    private final T value;

    private final ToDoubleFunction<T> apply;

    public GaugeMetricSample(MetricsKey metricsKey, Map<String, String> tags, MetricsCategory category, T value, ToDoubleFunction<T> apply) {
        this(metricsKey.getName(), metricsKey.getDescription(), tags, category, null, value, apply);
    }

    public GaugeMetricSample(String name, String description, Map<String, String> tags, MetricsCategory category, T value, ToDoubleFunction<T> apply) {
        this(name, description, tags, category, null, value, apply);
    }

    public GaugeMetricSample(String name, String description, Map<String, String> tags, MetricsCategory category, String baseUnit, T value, ToDoubleFunction<T> apply) {
        super(name, description, tags, Type.GAUGE, category, baseUnit);
        this.value = Objects.requireNonNull(value, "The GaugeMetricSample value cannot be null");
        this.apply = Objects.requireNonNull(apply, "The GaugeMetricSample apply cannot be null");
    }

    public T getValue() {
        return this.value;
    }

    public ToDoubleFunction<T> getApply() {
        return this.apply;
    }

    public long applyAsLong() {
        return (long) getApply().applyAsDouble(getValue());
    }

    public double applyAsDouble() {
        return getApply().applyAsDouble(getValue());
    }
}
