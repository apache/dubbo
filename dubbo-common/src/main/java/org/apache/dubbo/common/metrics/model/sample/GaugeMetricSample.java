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

package org.apache.dubbo.common.metrics.model.sample;

import java.util.Map;
import java.util.function.Supplier;

/**
 * GaugeMetricSample.
 */
public class GaugeMetricSample extends MetricSample {

    private Supplier<Number> supplier;

    public GaugeMetricSample(String name, String description, Map<String, String> tags, Supplier<Number> supplier) {
        super(name, description, tags, Type.GAUGE);
        this.supplier = supplier;
    }

    public GaugeMetricSample(String name, String description, Map<String, String> tags, String baseUnit, Supplier<Number> supplier) {
        super(name, description, tags, Type.GAUGE, baseUnit);
        this.supplier = supplier;
    }

    public Supplier<Number> getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier<Number> supplier) {
        this.supplier = supplier;
    }
}
