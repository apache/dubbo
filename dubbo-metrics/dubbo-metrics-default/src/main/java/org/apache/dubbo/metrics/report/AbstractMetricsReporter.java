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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.constants.MetricsConstants;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metrics.DubboMetrics;
import org.apache.dubbo.metrics.collector.AggregateMetricsCollector;
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION;
import static org.apache.dubbo.common.constants.MetricsConstants.ENABLE_JVM_METRICS_KEY;

/**
 * AbstractMetricsReporter.
 */
public abstract class AbstractMetricsReporter implements MetricsReporter {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractMetricsReporter.class);

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean addGlobalRegistry = new AtomicBoolean(false);

    protected final URL url;
    protected final List<MetricsCollector> collectors = new ArrayList<>();
    protected final CompositeMeterRegistry compositeRegistry = new CompositeMeterRegistry();

    private final ApplicationModel applicationModel;


    protected AbstractMetricsReporter(URL url, ApplicationModel applicationModel) {
        this.url = url;
        this.applicationModel = applicationModel;
    }

    @Override
    public void init() {
        if (initialized.compareAndSet(false, true)) {
            addJvmMetrics();
            initCollectors();

            doInit();

            registerDubboShutdownHook();
        }
    }

    protected void addMeterRegistry(MeterRegistry registry) {
        compositeRegistry.add(registry);
    }
    private void addDubboMeterRegistry(){
        MeterRegistry globalRegistry = DubboMetrics.globalRegistry;
        if(globalRegistry != null && !addGlobalRegistry.get()){
            compositeRegistry.add(globalRegistry);
            addGlobalRegistry.set(true);
        }
    }

    protected ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    private void addJvmMetrics() {
        boolean enableJvmMetrics = url.getParameter(ENABLE_JVM_METRICS_KEY, false);
        if (enableJvmMetrics) {
            Tags extraTags = Tags.of(MetricsConstants.TAG_APPLICATION_NAME,
                Optional.ofNullable(applicationModel.getApplicationName()).orElse(""));
            new ClassLoaderMetrics(extraTags).bindTo(compositeRegistry);
            new JvmMemoryMetrics(extraTags).bindTo(compositeRegistry);

            @SuppressWarnings("java:S2095")
            // Do not change JvmGcMetrics to try-with-resources as the JvmGcMetrics will not be available after (auto-)closing.
            // See https://github.com/micrometer-metrics/micrometer/issues/1492
            JvmGcMetrics jvmGcMetrics = new JvmGcMetrics(extraTags);
            jvmGcMetrics.bindTo(compositeRegistry);
            Runtime.getRuntime().addShutdownHook(new Thread(jvmGcMetrics::close));

            new ProcessorMetrics(extraTags).bindTo(compositeRegistry);
            new JvmThreadMetrics(extraTags).bindTo(compositeRegistry);
            new UptimeMetrics(extraTags).bindTo(compositeRegistry);
        }
    }

    private void initCollectors() {
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        beanFactory.getOrRegisterBean(AggregateMetricsCollector.class);
        List<MetricsCollector> otherCollectors = beanFactory.getBeansOfType(MetricsCollector.class);
        collectors.addAll(otherCollectors);
    }

    public void refreshData() {
        addDubboMeterRegistry();
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
                    logger.error(COMMON_METRICS_COLLECTOR_EXCEPTION, "", "", "error occurred when synchronize metrics collector.", e);
                }
            }
        });
    }

    private void registerDubboShutdownHook() {
        applicationModel.getBeanFactory().getBean(ShutdownHookCallbacks.class).addCallback(this::destroy);
    }

    public void destroy() {
        doDestroy();
    }

    protected abstract void doInit();

    protected abstract void doDestroy();
}
