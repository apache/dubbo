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
package org.apache.dubbo.metrics.report;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.metrics.MetricsGlobalRegistry;
import org.apache.dubbo.metrics.collector.AggregateMetricsCollector;
import org.apache.dubbo.metrics.collector.HistogramMetricsCollector;
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION;
import static org.apache.dubbo.common.constants.MetricsConstants.COLLECTOR_SYNC_PERIOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.ENABLE_COLLECTOR_SYNC_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.ENABLE_JVM_METRICS_KEY;

/**
 * AbstractMetricsReporter.
 */
public abstract class AbstractMetricsReporter implements MetricsReporter {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractMetricsReporter.class);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    protected final URL url;

    @SuppressWarnings("rawtypes")
    protected final List<MetricsCollector> collectors = new ArrayList<>();
    // Avoid instances being gc due to weak references
    protected final List<MeterBinder> instanceHolder = new ArrayList<>();
    protected final CompositeMeterRegistry compositeRegistry;

    private final ApplicationModel applicationModel;

    private ScheduledExecutorService collectorSyncJobExecutor = null;

    private static final int DEFAULT_SCHEDULE_INITIAL_DELAY = 5;
    private static final int DEFAULT_SCHEDULE_PERIOD = 60;

    protected AbstractMetricsReporter(URL url, ApplicationModel applicationModel) {
        this.url = url;
        this.applicationModel = applicationModel;
        this.compositeRegistry = MetricsGlobalRegistry.getCompositeRegistry(applicationModel);
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

            @SuppressWarnings("java:S2095")
            // Do not change JvmGcMetrics to try-with-resources as the JvmGcMetrics will not be available after
            // (auto-)closing.
            // See https://github.com/micrometer-metrics/micrometer/issues/1492
            JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
            jvmGcMetrics.bindTo(compositeRegistry);
            Runtime.getRuntime().addShutdownHook(new Thread(jvmGcMetrics::close));

            bindTo(new ProcessorMetrics());
            new JvmThreadMetrics().bindTo(compositeRegistry);
            bindTo(new UptimeMetrics());
        }
    }

    private void bindTo(MeterBinder binder) {
        binder.bindTo(compositeRegistry);
        instanceHolder.add(binder);
    }

    @SuppressWarnings("rawtypes")
    private void initCollectors() {
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        beanFactory.getOrRegisterBean(AggregateMetricsCollector.class);
        beanFactory.getOrRegisterBean(HistogramMetricsCollector.class);
        List<MetricsCollector> otherCollectors = beanFactory.getBeansOfType(MetricsCollector.class);
        collectors.addAll(otherCollectors);
    }

    private void scheduleMetricsCollectorSyncJob() {
        boolean enableCollectorSync = url.getParameter(ENABLE_COLLECTOR_SYNC_KEY, true);
        if (enableCollectorSync) {
            int collectSyncPeriod = url.getParameter(COLLECTOR_SYNC_PERIOD_KEY, DEFAULT_SCHEDULE_PERIOD);

            NamedThreadFactory threadFactory = new NamedThreadFactory("metrics-collector-sync-job", true);
            collectorSyncJobExecutor = Executors.newScheduledThreadPool(1, threadFactory);
            collectorSyncJobExecutor.scheduleWithFixedDelay(
                    this::resetIfSamplesChanged, DEFAULT_SCHEDULE_INITIAL_DELAY, collectSyncPeriod, TimeUnit.SECONDS);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void resetIfSamplesChanged() {
        collectors.forEach(collector -> {
            if (!collector.calSamplesChanged()) {
                // Metrics has not been changed since last time, no need to reload
                return;
            }
            // Collect all the samples and register them to the micrometer registry
            List<MetricSample> samples = collector.collect();
            for (MetricSample sample : samples) {
                try {
                    registerSample(sample);
                } catch (Exception e) {
                    logger.error(
                            COMMON_METRICS_COLLECTOR_EXCEPTION,
                            "",
                            "",
                            "error occurred when synchronize metrics collector.",
                            e);
                }
            }
        });
    }

    @SuppressWarnings({"rawtypes"})
    private void registerSample(MetricSample sample) {
        switch (sample.getType()) {
            case GAUGE:
                registerGaugeSample((GaugeMetricSample) sample);
                break;
            case COUNTER:
                registerCounterSample((CounterMetricSample) sample);
            case TIMER:
            case LONG_TASK_TIMER:
            case DISTRIBUTION_SUMMARY:
                // TODO
                break;
            default:
                break;
        }
    }

    @SuppressWarnings({"rawtypes"})
    private void registerCounterSample(CounterMetricSample sample) {
        FunctionCounter.builder(sample.getName(), sample.getValue(), Number::doubleValue)
                .description(sample.getDescription())
                .tags(getTags(sample))
                .register(compositeRegistry);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerGaugeSample(GaugeMetricSample sample) {
        Gauge.builder(sample.getName(), sample.getValue(), sample.getApply())
                .description(sample.getDescription())
                .tags(getTags(sample))
                .register(compositeRegistry);
    }

    private static List<Tag> getTags(MetricSample gaugeSample) {
        List<Tag> tags = new ArrayList<>();
        gaugeSample.getTags().forEach((k, v) -> {
            if (v == null) {
                v = "";
            }

            tags.add(Tag.of(k, v));
        });
        return tags;
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
