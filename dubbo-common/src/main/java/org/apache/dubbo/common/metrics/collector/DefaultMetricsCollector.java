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

package org.apache.dubbo.common.metrics.collector;

import org.apache.dubbo.common.metrics.event.BaseMetricsEvent;
import org.apache.dubbo.common.metrics.event.RTChangedEvent;
import org.apache.dubbo.common.metrics.event.RequestChangedEvent;
import org.apache.dubbo.common.metrics.listener.MetricsListener;
import org.apache.dubbo.common.metrics.model.MethodMetric;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link MetricsCollector}
 */
public class DefaultMetricsCollector implements MetricsCollector {

    private Boolean collectEnabled = false;
    private final List<MetricsListener> listeners = new ArrayList<>();

    private final Map<MethodMetric, AtomicLong> totalRequests = new HashMap<>();
    private final Map<MethodMetric, AtomicLong> succeedRequests = new HashMap<>();
    private final Map<MethodMetric, AtomicLong> failedRequests = new HashMap<>();
    private final Map<MethodMetric, AtomicLong> processingRequests = new HashMap<>();
    private final Map<MethodMetric, Long> lastRT = new HashMap<>();

    private static final DefaultMetricsCollector INSTANCE = new DefaultMetricsCollector();

    private DefaultMetricsCollector() {

    }

    public static DefaultMetricsCollector getInstance() {
        return INSTANCE;
    }

    public void setCollectEnabled(Boolean collectEnabled) {
        this.collectEnabled = collectEnabled;
    }

    public Boolean isCollectEnabled() {
        return collectEnabled;
    }

    public void addListener(MetricsListener listener) {
        listeners.add(listener);
    }

    public void increaseTotalRequests(String interfaceName, String methodName, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(interfaceName, methodName, group, version);
            AtomicLong count = totalRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();

            publishEvent(new RequestChangedEvent(metric, RequestChangedEvent.Type.TOTAL));
        }
    }

    public void increaseSucceedRequests(String interfaceName, String methodName, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(interfaceName, methodName, group, version);
            AtomicLong count = succeedRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();

            publishEvent(new RequestChangedEvent(metric, RequestChangedEvent.Type.SUCCEED));
        }
    }

    public void increaseFailedRequests(String interfaceName, String methodName, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(interfaceName, methodName, group, version);
            AtomicLong count = failedRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();

            publishEvent(new RequestChangedEvent(metric, RequestChangedEvent.Type.FAILED));
        }
    }

    public void increaseProcessingRequests(String interfaceName, String methodName, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(interfaceName, methodName, group, version);
            AtomicLong count = processingRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();
        }
    }

    public void decreaseProcessingRequests(String interfaceName, String methodName, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(interfaceName, methodName, group, version);
            AtomicLong count = processingRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.decrementAndGet();
        }
    }

    public void setRT(String interfaceName, String methodName, String group, String version, Long responseTime) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(interfaceName, methodName, group, version);
            lastRT.put(metric, responseTime);

            publishEvent(new RTChangedEvent(metric, responseTime));
        }
    }

    private void publishEvent(BaseMetricsEvent event) {
        for (MetricsListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        collectRequests(list);
        collectRT(list);

        return list;
    }

    private void collectRequests(List<MetricSample> list) {
        totalRequests.forEach((k, v) -> list.add(new GaugeMetricSample("request_total", "Total Requests", k.getTags(), v::get)));
        succeedRequests.forEach((k, v) -> list.add(new GaugeMetricSample("request_succeed", "Succeed Requests", k.getTags(), v::get)));
        failedRequests.forEach((k, v) -> list.add(new GaugeMetricSample("request_failed", "Failed Requests", k.getTags(), v::get)));
        processingRequests.forEach((k, v) -> list.add(new GaugeMetricSample("request_processing", "Processing Requests", k.getTags(), v::get)));
    }

    private void collectRT(List<MetricSample> list) {
        lastRT.forEach((k, v) -> list.add(new GaugeMetricSample("rt_last", "Last Response Time", k.getTags(), v::longValue)));
    }
}
