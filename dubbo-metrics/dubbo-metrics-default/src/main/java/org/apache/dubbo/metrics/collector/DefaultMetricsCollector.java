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

import org.apache.dubbo.common.Version;
import org.apache.dubbo.metrics.collector.sample.MethodMetricsSampler;
import org.apache.dubbo.metrics.collector.sample.MetricsCountSampleConfigurer;
import org.apache.dubbo.metrics.collector.sample.MetricsSampler;
import org.apache.dubbo.metrics.collector.sample.SimpleMetricsCountSampler;
import org.apache.dubbo.metrics.collector.sample.ThreadPoolMetricsSampler;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.SimpleMetricsEventMulticaster;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.metrics.model.MetricsCategory.APPLICATION;
import static org.apache.dubbo.metrics.model.MetricsKey.APPLICATION_METRIC_INFO;

/**
 * Default implementation of {@link MetricsCollector}
 */
public class DefaultMetricsCollector implements MetricsCollector {

    private AtomicBoolean collectEnabled = new AtomicBoolean(false);
    private final SimpleMetricsEventMulticaster eventMulticaster;
    private MethodMetricsSampler methodSampler = new MethodMetricsSampler(this);
    private ThreadPoolMetricsSampler threadPoolSampler = new ThreadPoolMetricsSampler(this);
    private String applicationName;
    private ApplicationModel applicationModel;
    private List<MetricsSampler> samplers = new ArrayList<>();

    public DefaultMetricsCollector() {
        this.eventMulticaster = SimpleMetricsEventMulticaster.getInstance();
        samplers.add(methodSampler);
        samplers.add(applicationSampler);
        samplers.add(threadPoolSampler);
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationName() {
        return this.applicationName;
    }

    public ApplicationModel getApplicationModel() {
        return this.applicationModel;
    }

    public SimpleMetricsEventMulticaster getEventMulticaster() {
        return this.eventMulticaster;
    }

    public void setCollectEnabled(Boolean collectEnabled) {
        this.collectEnabled.compareAndSet(isCollectEnabled(), collectEnabled);
    }

    public Boolean isCollectEnabled() {
        return collectEnabled.get();
    }

    public MethodMetricsSampler getMethodSampler() {
        return this.methodSampler;
    }

    public void collectApplication(ApplicationModel applicationModel) {
        this.setApplicationName(applicationModel.getApplicationName());
        this.applicationModel = applicationModel;
        applicationSampler.inc(applicationName, MetricsEvent.Type.APPLICATION_INFO);
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        for (MetricsSampler sampler : samplers) {
            List<MetricSample> sample = sampler.sample();
            list.addAll(sample);
        }
        return list;
    }

    public void addListener(MetricsListener listener) {
        this.eventMulticaster.addListener(listener);
    }

    public SimpleMetricsCountSampler<String, MetricsEvent.Type, ApplicationMetric> applicationSampler = new SimpleMetricsCountSampler<String, MetricsEvent.Type, ApplicationMetric>() {
        @Override
        public List<MetricSample> sample() {
            List<MetricSample> samples = new ArrayList<>();
            this.getCount(MetricsEvent.Type.APPLICATION_INFO).filter(e -> !e.isEmpty())
                .ifPresent(map -> map.forEach((k, v) -> samples.add(new GaugeMetricSample(APPLICATION_METRIC_INFO, k.getTags(),
                    APPLICATION, v::get))));
            return samples;
        }

        @Override
        protected void countConfigure(
            MetricsCountSampleConfigurer<String, MetricsEvent.Type, ApplicationMetric> sampleConfigure) {
            sampleConfigure.configureMetrics(configure -> new ApplicationMetric(sampleConfigure.getSource(),
                Version.getVersion()));
        }
    };
}
