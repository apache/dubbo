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
import org.apache.dubbo.metrics.sample.TimerMetricSample;

import java.util.ArrayList;
import java.util.List;

public class TimerMetricRegister implements MetricRegister<TimerMetricSample, Timer> {

    private final MeterRegistry registry;

    public TimerMetricRegister(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Timer register(TimerMetricSample sample) {
        List<Tag> tags = new ArrayList<>();
        sample.getTags().forEach((k, v) -> {
            if (v == null) {
                v = "";
            }

            tags.add(Tag.of(k, v));
        });

        return Timer.builder(sample.getName()).description(sample.getDescription()).tags(tags)
            .publishPercentileHistogram(true).register(registry);
    }
}
