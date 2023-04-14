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
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.HistogramConfig;
import org.apache.dubbo.metrics.MetricsGlobalRegistry;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.RTEvent;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.register.HistogramMetricRegister;
import org.apache.dubbo.metrics.sample.HistogramMetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.metrics.model.MetricsCategory.RT;

public class HistogramMetricsCollector implements MetricsListener {

    private final ConcurrentHashMap<MethodMetric, Timer> rt = new ConcurrentHashMap<>();
    private HistogramMetricRegister metricRegister;
    private final ApplicationModel applicationModel;

    private static final Integer[] DEFAULT_BUCKETS_MS = new Integer[]{100, 300, 500, 1000, 3000, 5000, 10000};

    public HistogramMetricsCollector(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;

        ConfigManager configManager = applicationModel.getApplicationConfigManager();
        MetricsConfig config = configManager.getMetrics().orElse(null);
        if (config != null && config.getHistogram() != null && Boolean.TRUE.equals(config.getHistogram().getEnabled())) {
            registerListener();

            HistogramConfig histogram = config.getHistogram();
            if (!Boolean.TRUE.equals(histogram.getEnabledPercentiles()) && histogram.getBucketsMs() == null) {
                histogram.setBucketsMs(DEFAULT_BUCKETS_MS);
            }

            metricRegister = new HistogramMetricRegister(MetricsGlobalRegistry.getCompositeRegistry(), histogram);
        }
    }

    private void registerListener() {
        applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class).addListener(this);
    }

    @Override
    public void onEvent(MetricsEvent event) {
        if (event instanceof RTEvent) {
            onRTEvent((RTEvent) event);
        }
    }

    private void onRTEvent(RTEvent event) {
        if (metricRegister != null) {
            MethodMetric metric = (MethodMetric) event.getMetric();
            Long responseTime = event.getRt();

            HistogramMetricSample sample = new HistogramMetricSample(MetricsKey.METRIC_RT_HISTOGRAM.getNameByType(metric.getSide()),
                MetricsKey.METRIC_RT_HISTOGRAM.getDescription(), metric.getTags(), RT);

            Timer timer = ConcurrentHashMapUtils.computeIfAbsent(rt, metric, k -> metricRegister.register(sample));
            timer.record(responseTime, TimeUnit.MILLISECONDS);
        }
    }
}
