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
package org.apache.dubbo.metrics.collector.sample;

import org.apache.dubbo.metrics.model.Metric;

import java.util.function.Function;

public class MetricsCountSampleConfigurer<S, K, M extends Metric> {

    public S source;

    public K metricName;

    public M metric;

    public void setSource(S source) {
        this.source = source;
    }

    public MetricsCountSampleConfigurer<S, K, M> setMetricsName(K metricName) {
        this.metricName = metricName;
        return this;
    }

    public MetricsCountSampleConfigurer<S, K, M> configureMetrics(
            Function<MetricsCountSampleConfigurer<S, K, M>, M> builder) {
        this.metric = builder.apply(this);
        return this;
    }

    public S getSource() {
        return source;
    }

    public M getMetric() {
        return metric;
    }
}
