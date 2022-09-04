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

import org.apache.dubbo.common.metrics.event.MetricsEvent;
import org.apache.dubbo.common.metrics.event.RTEvent;
import org.apache.dubbo.common.metrics.event.RequestEvent;
import org.apache.dubbo.common.metrics.listener.MetricsListener;
import org.apache.dubbo.common.metrics.model.MethodMetric;
import org.apache.dubbo.common.metrics.model.MetricsKey;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

import static org.apache.dubbo.common.metrics.model.MetricsCategory.REQUESTS;
import static org.apache.dubbo.common.metrics.model.MetricsCategory.RT;

/**
 * Default implementation of {@link MetricsCollector}
 */
public class DefaultMetricsCollector implements MetricsCollector {

    private AtomicBoolean collectEnabled = new AtomicBoolean(false);
    private final List<MetricsListener> listeners = new ArrayList<>();
    private final ApplicationModel applicationModel;
    private final String applicationName;

    private final Map<MethodMetric, AtomicLong> totalRequests = new ConcurrentHashMap<>();
    private final Map<MethodMetric, AtomicLong> succeedRequests = new ConcurrentHashMap<>();
    private final Map<MethodMetric, AtomicLong> failedRequests = new ConcurrentHashMap<>();
    private final Map<MethodMetric, AtomicLong> processingRequests = new ConcurrentHashMap<>();

    private final Map<MethodMetric, AtomicLong> lastRT = new ConcurrentHashMap<>();
    private final Map<MethodMetric, LongAccumulator> minRT = new ConcurrentHashMap<>();
    private final Map<MethodMetric, LongAccumulator> maxRT = new ConcurrentHashMap<>();
    private final Map<MethodMetric, AtomicLong> avgRT = new ConcurrentHashMap<>();
    private final Map<MethodMetric, AtomicLong> totalRT = new ConcurrentHashMap<>();
    private final Map<MethodMetric, AtomicLong> rtCount = new ConcurrentHashMap<>();

    public DefaultMetricsCollector(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.applicationName = applicationModel.getApplicationName();
    }

    public void setCollectEnabled(Boolean collectEnabled) {
        this.collectEnabled.compareAndSet(isCollectEnabled(), collectEnabled);
    }

    public Boolean isCollectEnabled() {
        return collectEnabled.get();
    }

    public void addListener(MetricsListener listener) {
        listeners.add(listener);
    }

    public void increaseTotalRequests(String interfaceName, String methodName, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, group, version);
            AtomicLong count = totalRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();

            publishEvent(new RequestEvent(metric, RequestEvent.Type.TOTAL));
        }
    }

    public void increaseSucceedRequests(String interfaceName, String methodName, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, group, version);
            AtomicLong count = succeedRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();

            publishEvent(new RequestEvent(metric, RequestEvent.Type.SUCCEED));
        }
    }

    public void increaseFailedRequests(String interfaceName, String methodName, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, group, version);
            AtomicLong count = failedRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();

            publishEvent(new RequestEvent(metric, RequestEvent.Type.FAILED));
        }
    }

    public void increaseProcessingRequests(String interfaceName, String methodName, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, group, version);
            AtomicLong count = processingRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();
        }
    }

    public void decreaseProcessingRequests(String interfaceName, String methodName, String group, String version) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, group, version);
            AtomicLong count = processingRequests.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.decrementAndGet();
        }
    }

    public void addRT(String interfaceName, String methodName, String group, String version, Long responseTime) {
        if (isCollectEnabled()) {
            MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, group, version);

            AtomicLong last = lastRT.computeIfAbsent(metric, k -> new AtomicLong());
            last.set(responseTime);

            LongAccumulator min = minRT.computeIfAbsent(metric, k -> new LongAccumulator(Long::min, Long.MAX_VALUE));
            min.accumulate(responseTime);

            LongAccumulator max = maxRT.computeIfAbsent(metric, k -> new LongAccumulator(Long::max, Long.MIN_VALUE));
            max.accumulate(responseTime);

            AtomicLong total = totalRT.computeIfAbsent(metric, k -> new AtomicLong());
            total.addAndGet(responseTime);

            AtomicLong count = rtCount.computeIfAbsent(metric, k -> new AtomicLong());
            count.incrementAndGet();

            avgRT.computeIfAbsent(metric, k -> new AtomicLong());

            publishEvent(new RTEvent(metric, responseTime));
        }
    }

    private void publishEvent(MetricsEvent event) {
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
        totalRequests.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_REQUESTS_TOTAL, k.getTags(), REQUESTS, v::get)));
        succeedRequests.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_REQUESTS_SUCCEED, k.getTags(), REQUESTS, v::get)));
        failedRequests.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_REQUESTS_FAILED, k.getTags(), REQUESTS, v::get)));
        processingRequests.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_REQUESTS_PROCESSING, k.getTags(), REQUESTS, v::get)));
    }

    private void collectRT(List<MetricSample> list) {
        lastRT.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_RT_LAST, k.getTags(), RT, v::get)));
        minRT.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_RT_MIN, k.getTags(), RT, v::get)));
        maxRT.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.METRIC_RT_MAX, k.getTags(), RT, v::get)));

        totalRT.forEach((k, v) -> {
            list.add(new GaugeMetricSample(MetricsKey.METRIC_RT_TOTAL, k.getTags(), RT, v::get));

            AtomicLong avg = avgRT.get(k);
            AtomicLong count = rtCount.get(k);
            avg.set(v.get() / count.get());
            list.add(new GaugeMetricSample(MetricsKey.METRIC_RT_AVG, k.getTags(), RT, avg::get));
        });
    }
}
