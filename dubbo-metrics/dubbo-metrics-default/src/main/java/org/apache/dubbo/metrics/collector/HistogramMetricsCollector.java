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

import org.apache.dubbo.common.event.AbstractDubboLifecycleListener;
import org.apache.dubbo.common.event.DubboEventBus;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.HistogramConfig;
import org.apache.dubbo.metrics.MetricsConstants;
import org.apache.dubbo.metrics.MetricsGlobalRegistry;
import org.apache.dubbo.metrics.event.RequestMetricsEvent;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.register.HistogramMetricRegister;
import org.apache.dubbo.metrics.sample.HistogramMetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Timer;

import static org.apache.dubbo.metrics.model.MetricsCategory.RT;

public class HistogramMetricsCollector implements MetricsCollector {

    private final ConcurrentHashMap<MethodMetric, Timer> rt = new ConcurrentHashMap<>();
    private HistogramMetricRegister metricRegister;
    private final ApplicationModel applicationModel;

    private static final Integer[] DEFAULT_BUCKETS_MS = new Integer[] {100, 300, 500, 1000, 3000, 5000, 10000};

    private boolean serviceLevel;

    public HistogramMetricsCollector(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;

        ConfigManager configManager = applicationModel.getApplicationConfigManager();
        MetricsConfig config = configManager.getMetrics().orElse(null);
        if (config == null
                || config.getHistogram() == null
                || config.getHistogram().getEnabled() == null
                || Boolean.TRUE.equals(config.getHistogram().getEnabled())) {
            DubboEventBus.addListener(applicationModel, new HistogramMetricsEventListener(this));

            HistogramConfig histogram;
            if (config == null || config.getHistogram() == null) {
                histogram = new HistogramConfig();
            } else {
                histogram = config.getHistogram();
            }

            if (!Boolean.TRUE.equals(histogram.getEnabledPercentiles()) && histogram.getBucketsMs() == null) {
                histogram.setBucketsMs(DEFAULT_BUCKETS_MS);
            }

            metricRegister = new HistogramMetricRegister(
                    MetricsGlobalRegistry.getCompositeRegistry(applicationModel), histogram);
            this.serviceLevel = MethodMetric.isServiceLevel(applicationModel);
        }
    }

    public static class HistogramMetricsEventListener extends AbstractDubboLifecycleListener<RequestMetricsEvent> {

        private final HistogramMetricsCollector collector;

        public HistogramMetricsEventListener(HistogramMetricsCollector collector) {
            this.collector = collector;
        }

        @Override
        public void onEventFinish(RequestMetricsEvent event) {
            onRTEvent(event);
        }

        @Override
        public void onEventError(RequestMetricsEvent event) {
            onRTEvent(event);
        }

        private void onRTEvent(RequestMetricsEvent event) {
            if (collector.metricRegister != null) {
                MethodMetric metric = new MethodMetric(
                        collector.applicationModel,
                        event.getAttachmentValue(MetricsConstants.INVOCATION),
                        collector.serviceLevel);
                long responseTime = event.getTimePair().calc();

                HistogramMetricSample sample = new HistogramMetricSample(
                        MetricsKey.METRIC_RT_HISTOGRAM.getNameByType(metric.getSide()),
                        MetricsKey.METRIC_RT_HISTOGRAM.getDescription(),
                        metric.getTags(),
                        RT);

                Timer timer = ConcurrentHashMapUtils.computeIfAbsent(
                        collector.rt, metric, k -> collector.metricRegister.register(sample));
                timer.record(responseTime, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public List<MetricSample> collect() {
        return new ArrayList<>();
    }

    @Override
    public boolean calSamplesChanged() {
        // Histogram is directly register micrometer
        return false;
    }
}
