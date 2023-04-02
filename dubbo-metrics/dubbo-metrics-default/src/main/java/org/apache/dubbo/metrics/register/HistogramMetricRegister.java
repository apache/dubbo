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

package org.apache.dubbo.metrics.register;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.apache.dubbo.config.nested.HistogramConfig;
import org.apache.dubbo.metrics.sample.HistogramMetricSample;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HistogramMetricRegister implements MetricRegister<HistogramMetricSample, Timer> {

    private final MeterRegistry registry;
    private final HistogramConfig config;

    public HistogramMetricRegister(MeterRegistry registry, HistogramConfig config) {
        this.registry = registry;
        this.config = config;
    }

    @Override
    public Timer register(HistogramMetricSample sample) {
        List<Tag> tags = new ArrayList<>();
        sample.getTags().forEach((k, v) -> {
            if (v == null) {
                v = "";
            }

            tags.add(Tag.of(k, v));
        });

        Timer.Builder builder = Timer.builder(sample.getName()).description(sample.getDescription()).tags(tags);

        if (Boolean.TRUE.equals(config.getEnabledPercentiles())) {
            builder.publishPercentileHistogram(true);
        }

        if (config.getPercentiles() != null) {
            builder.publishPercentiles(config.getPercentiles());
        }

        if (config.getBucketsMs() != null) {
            builder.serviceLevelObjectives(Arrays.stream(config.getBucketsMs())
                .map(Duration::ofMillis).toArray(Duration[]::new));
        }

        if (config.getMinExpectedMs() != null) {
            builder.minimumExpectedValue(Duration.ofMillis(config.getMinExpectedMs()));
        }

        if (config.getMaxExpectedMs() != null) {
            builder.maximumExpectedValue(Duration.ofMillis(config.getMaxExpectedMs()));
        }

        if (config.getDistributionStatisticExpiryMin() != null) {
            builder.distributionStatisticExpiry(Duration.ofMinutes(config.getDistributionStatisticExpiryMin()));
        }

        return builder.register(registry);
    }
}
