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

package org.apache.dubbo.metrics;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.metrics.MetricsReporter;
import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.common.metrics.collector.MetricsCollector;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.metrics.collector.AggregateMetricsCollector;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AbstractMetricsReporter.
 */
public abstract class AbstractMetricsReporter implements MetricsReporter {

    protected final URL url;
    protected final List<MetricsCollector> collectors = new ArrayList<>();
    protected final CompositeMeterRegistry compositeRegistry = new CompositeMeterRegistry();

    private static final int DEFAULT_SCHEDULE_INITIAL_DELAY = 5;
    private static final int DEFAULT_SCHEDULE_PERIOD = 30;

    protected AbstractMetricsReporter(URL url) {
        this.url = url;

        collectors.add(DefaultMetricsCollector.getInstance());
        collectors.add(AggregateMetricsCollector.getInstance());
    }

    protected void addMeterRegistry(MeterRegistry registry) {
        compositeRegistry.add(registry);
    }

    @Override
    public void init() {
        scheduleMetricsCollectorSyncJob();

        doInit();
    }

    private void scheduleMetricsCollectorSyncJob() {
        NamedThreadFactory threadFactory = new NamedThreadFactory("metrics-collector-sync-job", true);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, threadFactory);
        executor.scheduleAtFixedRate(() -> {
            collectors.forEach(collector -> {
                List<MetricSample> samples = collector.collect();
                for (MetricSample sample : samples) {
                    switch (sample.getType()) {
                        case GAUGE:
                            GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
                            List<Tag> tags = new ArrayList<>();
                            gaugeSample.getTags().forEach((k, v) -> tags.add(Tag.of(k, v)));

                            Gauge.builder(gaugeSample.getName(), gaugeSample.getSupplier())
                                .description(gaugeSample.getDescription()).tags(tags).register(compositeRegistry);
                            break;
                        case TIMER:
                        case COUNTER:
                        case LONG_TASK_TIMER:
                        case DISTRIBUTION_SUMMARY:
                            // TODO
                            break;
                        default:
                            break;
                    }
                }
            });
        }, DEFAULT_SCHEDULE_INITIAL_DELAY, DEFAULT_SCHEDULE_PERIOD, TimeUnit.SECONDS);
    }

    protected abstract void doInit();
}
