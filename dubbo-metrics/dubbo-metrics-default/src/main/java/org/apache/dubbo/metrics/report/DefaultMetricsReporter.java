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

package org.apache.dubbo.metrics.report;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.TimeUnit;

public class DefaultMetricsReporter extends AbstractMetricsReporter {

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    protected DefaultMetricsReporter(URL url, ApplicationModel applicationModel) {
        super(url, applicationModel);
    }

    @Override
    public String getResponse() {
        return null;
    }

    @Override
    public String getResponseWithName(String metricsName) {
        //name->tags->value
        Map<String, Map<String, Object>> result = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        meterRegistry.getMeters().stream().filter(meter -> {
            if (meter == null || meter.getId() == null || meter.getId().getName() == null) {
                return false;
            }
            if (metricsName != null) {
                return meter.getId().getName().contains(metricsName);
            }
            return true;
        }).forEach(meter -> {
            result.putIfAbsent(meter.getId().getName(), new HashMap<>());
            Object value = null;
            if (meter instanceof Counter) {
                Counter counter = (Counter) meter;
                value = counter.count();
            }
            if (meter instanceof Gauge) {
                Gauge gauge = (Gauge) meter;
                value = gauge.value();
            }
            if (meter instanceof Timer) {
                Timer timer = (Timer) meter;
                value = timer.totalTime(TimeUnit.MILLISECONDS);
            }
            result.get(meter.getId().getName()).put(meter.getId().getTags().toString(), value);
        });
        result.forEach((name, tags) -> {
            sb.append("##metrics name=").append(name).append(System.lineSeparator());
            tags.forEach((tag, value) -> {
                sb.append(" #tags:").append(tag).append(System.lineSeparator()).append(" #value：").append(value)
                    .append(System.lineSeparator());
            });
        });
        return sb.toString();
    }

    @Override
    protected void doInit() {
        addMeterRegistry(meterRegistry);
    }

    @Override
    protected void doDestroy() {

    }
}
