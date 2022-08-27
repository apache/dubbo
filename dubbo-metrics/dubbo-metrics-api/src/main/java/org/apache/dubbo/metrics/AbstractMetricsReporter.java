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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.metrics.MetricsReporter;
import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.common.metrics.collector.MetricsCollector;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.metrics.collector.AggregateMetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.MetricsConstants.ENABLE_JVM_METRICS_KEY;

/**
 * AbstractMetricsReporter.
 */
public abstract class AbstractMetricsReporter implements MetricsReporter {

    private final Logger logger = LoggerFactory.getLogger(AbstractMetricsReporter.class);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    protected final URL url;
    protected final List<MetricsCollector> collectors = new ArrayList<>();
    protected final CompositeMeterRegistry compositeRegistry = new CompositeMeterRegistry();

    private final ApplicationModel applicationModel;
    private ScheduledExecutorService collectorSyncJobExecutor = null;

    private static final int DEFAULT_SCHEDULE_INITIAL_DELAY = 5;
    private static final int DEFAULT_SCHEDULE_PERIOD = 30;

    protected AbstractMetricsReporter(URL url, ApplicationModel applicationModel) {
        this.url = url;
        this.applicationModel = applicationModel;
    }

    @Override
    public void init() {
        if (initialized.compareAndSet(false, true)) {
            addJvmMetrics();
            initCollectors();
            scheduleMetricsCollectorSyncJob();

            doInit();

            registerDubboShutdownHook();
        }
    }

    protected void addMeterRegistry(MeterRegistry registry) {
        compositeRegistry.add(registry);
    }

    protected ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    private void addJvmMetrics() {
        boolean enableJvmMetrics = url.getParameter(ENABLE_JVM_METRICS_KEY, false);
        if (enableJvmMetrics) {
            new ClassLoaderMetrics().bindTo(compositeRegistry);
            new JvmMemoryMetrics().bindTo(compositeRegistry);
            new JvmGcMetrics().bindTo(compositeRegistry);
            new ProcessorMetrics().bindTo(compositeRegistry);
            new JvmThreadMetrics().bindTo(compositeRegistry);
        }
    }

    private void initCollectors() {
        applicationModel.getBeanFactory().getOrRegisterBean(AggregateMetricsCollector.class);

        collectors.add(applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class));
        collectors.add(applicationModel.getBeanFactory().getBean(AggregateMetricsCollector.class));
    }

    private void scheduleMetricsCollectorSyncJob() {
        NamedThreadFactory threadFactory = new NamedThreadFactory("metrics-collector-sync-job", true);
        collectorSyncJobExecutor = Executors.newScheduledThreadPool(1, threadFactory);
        collectorSyncJobExecutor.scheduleWithFixedDelay(() -> {
            collectors.forEach(collector -> {
                List<MetricSample> samples = collector.collect();
                for (MetricSample sample : samples) {
                    try {
                        switch (sample.getType()) {
                            case GAUGE:
                                GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
                                List<Tag> tags = new ArrayList<>();
                                gaugeSample.getTags().forEach((k, v) -> {
                                    if (v == null) {
                                        v = "";
                                    }

                                    tags.add(Tag.of(k, v));
                                });

                                Gauge.builder(gaugeSample.getName(), gaugeSample.getSupplier())
                                    .description(gaugeSample.getDescription()).tags(tags).register(compositeRegistry);
                                break;
                            case COUNTER:
                            case TIMER:
                            case LONG_TASK_TIMER:
                            case DISTRIBUTION_SUMMARY:
                                // TODO
                                break;
                            default:
                                break;
                        }
                    } catch (Exception e) {
                        logger.error("error occurred when synchronize metrics collector.", e);
                    }
                }
            });
        }, DEFAULT_SCHEDULE_INITIAL_DELAY, DEFAULT_SCHEDULE_PERIOD, TimeUnit.SECONDS);
    }

    private void registerDubboShutdownHook() {
        applicationModel.getBeanFactory().getBean(ShutdownHookCallbacks.class).addCallback(this::destroy);
    }

    public void destroy() {
        if (collectorSyncJobExecutor != null) {
            collectorSyncJobExecutor.shutdownNow();
        }

        doDestroy();
    }

    protected abstract void doInit();

    protected abstract void doDestroy();
}
