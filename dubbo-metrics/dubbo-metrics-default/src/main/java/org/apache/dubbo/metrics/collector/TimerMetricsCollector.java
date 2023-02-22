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

package org.apache.dubbo.metrics.collector;

import io.micrometer.core.instrument.Timer;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.metrics.MetricsGlobalRegistry;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RTEvent;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.register.TimerMetricRegister;
import org.apache.dubbo.metrics.sample.TimerMetricSample;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.metrics.model.MetricsCategory.RT;

public class TimerMetricsCollector implements MetricsListener, MetricsCollector {

    private final ConcurrentHashMap<MethodMetric, Timer> rt = new ConcurrentHashMap<>();
    private final TimerMetricRegister metricRegister;

    public TimerMetricsCollector() {
        metricRegister = new TimerMetricRegister(MetricsGlobalRegistry.getCompositeRegistry());
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();

        rt.forEach((k, v) -> {
            list.add(new TimerMetricSample(MetricsKey.GENERIC_METRIC_RT_P95, k.getTags(), RT));
        });

        return list;
    }

    @Override
    public void onEvent(MetricsEvent event) {
        if (event instanceof RTEvent) {
            onRTEvent((RTEvent) event);
        }
    }

    private void onRTEvent(RTEvent event) {
        MethodMetric metric = (MethodMetric) event.getSource();
        Long responseTime = event.getRt();

        TimerMetricSample sample = new TimerMetricSample(MetricsKey.PROVIDER_METRIC_RT_HISTOGRAM, metric.getTags(), RT);

        Timer timer = ConcurrentHashMapUtils.computeIfAbsent(rt, metric, k -> metricRegister.register(sample));
        timer.record(responseTime, TimeUnit.MILLISECONDS);
    }
}
