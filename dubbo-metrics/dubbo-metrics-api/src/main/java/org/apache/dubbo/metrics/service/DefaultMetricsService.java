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

package org.apache.dubbo.metrics.service;

import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.service.MetricResponse;
import org.apache.dubbo.common.metrics.service.MetricsService;
import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.common.metrics.collector.MetricsCollector;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.collector.AggregateMetricsCollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link MetricsService}
 */
public class DefaultMetricsService implements MetricsService {

    protected final List<MetricsCollector> collectors = new ArrayList<>();

    public DefaultMetricsService() {
        collectors.add(DefaultMetricsCollector.getInstance());
        collectors.add(AggregateMetricsCollector.getInstance());
    }

    @Override
    public Map<String, List<MetricResponse>> getMetricsByPrefix(List<String> prefixes) {
        return getMetricsByPrefix(null, prefixes);
    }

    @Override
    public Map<String, List<MetricResponse>> getMetricsByPrefix(String serviceUniqueName, List<String> prefixes) {
        return getMetricsByPrefix(serviceUniqueName, null, null, prefixes);
    }

    @Override
    public Map<String, List<MetricResponse>> getMetricsByPrefix(String serviceUniqueName, String methodName, Class<?>[] parameterTypes, List<String> prefixes) {
        Map<String, List<MetricResponse>> result = new HashMap<>();
        List<MetricSample> samples = getMetrics();
        for (String prefix : prefixes) {
            for (MetricSample sample : samples) {
                if (sample.getName().startsWith(prefix)) {
                    List<MetricResponse> responseList = result.computeIfAbsent(prefix, k -> new ArrayList<>());
                    responseList.add(sampleToResponse(sample));
                }
            }
        }

        return result;
    }

    private MetricResponse sampleToResponse(MetricSample sample) {
        MetricResponse response = new MetricResponse();

        response.setTags(sample.getTags());
        switch (sample.getType()) {
            case GAUGE:
                GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
                response.setValue(gaugeSample.getSupplier().get());
                break;
            case COUNTER:
            case LONG_TASK_TIMER:
            case TIMER:
            case DISTRIBUTION_SUMMARY:
            default:
                break;
        }

        return response;
    }

    private List<MetricSample> getMetrics() {
        List<MetricSample> samples = new ArrayList<>();
        for (MetricsCollector collector : collectors) {
            samples.addAll(collector.collect());
        }

        return samples;
    }
}
