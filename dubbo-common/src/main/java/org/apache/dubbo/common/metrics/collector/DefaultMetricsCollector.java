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
import org.apache.dubbo.common.metrics.event.NewRTEvent;
import org.apache.dubbo.common.metrics.event.NewRequestEvent;
import org.apache.dubbo.common.metrics.listener.MetricsListener;
import org.apache.dubbo.common.metrics.model.MethodMetric;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.common.metrics.model.MetricsCategory.REQUESTS;
import static org.apache.dubbo.common.metrics.model.MetricsCategory.RT;

/**
 * Default implementation of {@link MetricsCollector}
 */
public class DefaultMetricsCollector implements MetricsCollector {

    private Boolean collectEnabled = false;
    private final List<MetricsListener> listeners = new ArrayList<>();
    private final ApplicationModel applicationModel;
    private final String applicationName;

    private final Map<MethodMetric, AtomicLong> totalRequests = new HashMap<>();
    private final Map<MethodMetric, AtomicLong> succeedRequests = new HashMap<>();
    private final Map<MethodMetric, AtomicLong> failedRequests = new HashMap<>();
    private final Map<MethodMetric, AtomicLong> processingRequests = new HashMap<>();

    private final Map<MethodMetric, AtomicLong> lastRT = new HashMap<>();
    private final Map<MethodMetric, AtomicLong> minRT = new HashMap<>();
    private final Map<MethodMetric, AtomicLong> maxRT = new HashMap<>();
    private final Map<MethodMetric, AtomicLong> avgRT = new HashMap<>();
    private final Map<MethodMetric, AtomicLong> totalRT = new HashMap<>();

    public DefaultMetricsCollector(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.applicationName = applicationModel.tryGetApplicationName();
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

    public void increaseTotalRequests(String interfaceName, String methodName, String parameterTypesDesc, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, parameterTypesDesc, group, version);
            AtomicLong count = totalRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();

            publishEvent(new NewRequestEvent(metric, NewRequestEvent.Type.TOTAL));
        }
    }

    public void increaseSucceedRequests(String interfaceName, String methodName, String parameterTypesDesc, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, parameterTypesDesc, group, version);
            AtomicLong count = succeedRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();

            publishEvent(new NewRequestEvent(metric, NewRequestEvent.Type.SUCCEED));
        }
    }

    public void increaseFailedRequests(String interfaceName, String methodName, String parameterTypesDesc, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, parameterTypesDesc, group, version);
            AtomicLong count = failedRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();

            publishEvent(new NewRequestEvent(metric, NewRequestEvent.Type.FAILED));
        }
    }

    public void increaseProcessingRequests(String interfaceName, String methodName, String parameterTypesDesc, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, parameterTypesDesc, group, version);
            AtomicLong count = processingRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();
        }
    }

    public void decreaseProcessingRequests(String interfaceName, String methodName, String parameterTypesDesc, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, parameterTypesDesc, group, version);
            AtomicLong count = processingRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.decrementAndGet();
        }
    }

    public void setRT(String interfaceName, String methodName, String parameterTypesDesc, String group, String version, Long responseTime) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, parameterTypesDesc, group, version);

            AtomicLong last = lastRT.computeIfAbsent(metric, k -> new AtomicLong());
            last.set(responseTime);

            AtomicLong min = minRT.computeIfAbsent(metric, k -> new AtomicLong(Long.MAX_VALUE));
            if (responseTime < min.longValue()) {
                min.set(responseTime);
            }

            AtomicLong max = maxRT.computeIfAbsent(metric, k -> new AtomicLong(Long.MIN_VALUE));
            if (responseTime > max.longValue()) {
                max.set(responseTime);
            }

            AtomicLong total = totalRT.computeIfAbsent(metric, k -> new AtomicLong());
            long newTotal = total.addAndGet(responseTime);

            AtomicLong avg = avgRT.computeIfAbsent(metric, k -> new AtomicLong());
            long newAvg = newTotal / totalRequests.getOrDefault(metric, new AtomicLong(1L)).longValue();
            avg.set(newAvg);

            publishEvent(new NewRTEvent(metric, responseTime));
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
        totalRequests.forEach((k, v) -> list.add(new GaugeMetricSample("requests.total", "Total Requests", k.getTags(), REQUESTS, v::get)));
        succeedRequests.forEach((k, v) -> list.add(new GaugeMetricSample("requests.succeed", "Succeed Requests", k.getTags(), REQUESTS, v::get)));
        failedRequests.forEach((k, v) -> list.add(new GaugeMetricSample("requests.failed", "Failed Requests", k.getTags(), REQUESTS, v::get)));
        processingRequests.forEach((k, v) -> list.add(new GaugeMetricSample("requests.processing", "Processing Requests", k.getTags(), REQUESTS, v::get)));
    }

    private void collectRT(List<MetricSample> list) {
        lastRT.forEach((k, v) -> list.add(new GaugeMetricSample("rt.last", "Last Response Time", k.getTags(), RT, v::get)));
        minRT.forEach((k, v) -> list.add(new GaugeMetricSample("rt.min", "Min Response Time", k.getTags(), RT, v::get)));
        maxRT.forEach((k, v) -> list.add(new GaugeMetricSample("rt.max", "Max Response Time", k.getTags(), RT, v::get)));
        avgRT.forEach((k, v) -> list.add(new GaugeMetricSample("rt.avg", "Avg Response Time", k.getTags(), RT, v::get)));
        totalRT.forEach((k, v) -> list.add(new GaugeMetricSample("rt.total", "Total Response Time", k.getTags(), RT, v::get)));
    }
}
