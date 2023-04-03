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

import java.util.Map;

public class CounterMetricSample<T extends Number>  extends MetricSample {

    private final T value;

    public CounterMetricSample(String name, String description, Map<String, String> tags,
                               MetricsCategory category, T value ) {
        super(name, description, tags, Type.COUNTER, category);
        this.value = value;
    }

    public CounterMetricSample(String name, String description, Map<String, String> tags,   MetricsCategory category,
                               String baseUnit, T value) {
        super(name, description, tags, Type.COUNTER, category, baseUnit);
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
