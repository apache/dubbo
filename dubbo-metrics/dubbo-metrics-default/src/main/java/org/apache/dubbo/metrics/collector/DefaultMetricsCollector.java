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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metrics.DefaultConstants;
import org.apache.dubbo.metrics.MetricsConstants;
import org.apache.dubbo.metrics.collector.sample.MetricsCountSampleConfigurer;
import org.apache.dubbo.metrics.collector.sample.MetricsSampler;
import org.apache.dubbo.metrics.collector.sample.SimpleMetricsCountSampler;
import org.apache.dubbo.metrics.collector.sample.ThreadPoolMetricsSampler;
import org.apache.dubbo.metrics.data.BaseStatComposite;
import org.apache.dubbo.metrics.data.MethodStatComposite;
import org.apache.dubbo.metrics.data.RtStatComposite;
import org.apache.dubbo.metrics.event.DefaultSubDispatcher;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.MetricsInitEvent;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.metrics.DefaultConstants.INIT_DEFAULT_METHOD_KEYS;
import static org.apache.dubbo.metrics.model.MetricsCategory.APPLICATION;
import static org.apache.dubbo.metrics.model.key.MetricsKey.APPLICATION_METRIC_INFO;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED;

/**
 * Default implementation of {@link MetricsCollector}
 */
@Activate
public class DefaultMetricsCollector extends CombMetricsCollector<RequestEvent> {

    private boolean collectEnabled = false;

    private volatile boolean threadpoolCollectEnabled = false;

    private volatile boolean metricsInitEnabled = true;

    private final ThreadPoolMetricsSampler threadPoolSampler = new ThreadPoolMetricsSampler(this);
    private String applicationName;
    private final ApplicationModel applicationModel;
    private final List<MetricsSampler> samplers = new ArrayList<>();

    private final List<MetricsCollector> collectors = new ArrayList<>();

    private final AtomicBoolean initialized = new AtomicBoolean();

    private final AtomicBoolean samplesChanged = new AtomicBoolean();

    public DefaultMetricsCollector(ApplicationModel applicationModel) {
        super(new BaseStatComposite(applicationModel) {
            @Override
            protected void init(MethodStatComposite methodStatComposite) {
                super.init(methodStatComposite);
                methodStatComposite.initWrapper(DefaultConstants.METHOD_LEVEL_KEYS);
            }

            @Override
            protected void init(RtStatComposite rtStatComposite) {
                super.init(rtStatComposite);
                rtStatComposite.init(
                        MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.METHOD),
                        MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD));
            }
        });
        super.setEventMulticaster(new DefaultSubDispatcher(this));
        samplers.add(applicationSampler);
        samplers.add(threadPoolSampler);
        samplesChanged.set(true);
        this.applicationModel = applicationModel;
    }

    public void addSampler(MetricsSampler sampler) {
        samplers.add(sampler);
        samplesChanged.set(true);
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

    public void setCollectEnabled(Boolean collectEnabled) {
        this.collectEnabled = collectEnabled;
    }

    public boolean isCollectEnabled() {
        return collectEnabled;
    }

    public boolean isThreadpoolCollectEnabled() {
        return threadpoolCollectEnabled;
    }

    public void setThreadpoolCollectEnabled(boolean threadpoolCollectEnabled) {
        this.threadpoolCollectEnabled = threadpoolCollectEnabled;
    }

    public boolean isMetricsInitEnabled() {
        return metricsInitEnabled;
    }

    public void setMetricsInitEnabled(boolean metricsInitEnabled) {
        this.metricsInitEnabled = metricsInitEnabled;
    }

    public void collectApplication() {
        this.setApplicationName(applicationModel.getApplicationName());
        applicationSampler.inc(applicationName, MetricsEvent.Type.APPLICATION_INFO);
    }

    public void registryDefaultSample() {
        this.threadPoolSampler.registryDefaultSampleThreadPoolExecutor();
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        if (!isCollectEnabled()) {
            return list;
        }

        for (MetricsSampler sampler : samplers) {
            List<MetricSample> sample = sampler.sample();
            list.addAll(sample);
        }
        list.addAll(super.export(MetricsCategory.REQUESTS));
        return list;
    }

    @Override
    public boolean isSupport(MetricsEvent event) {
        return event instanceof RequestEvent || event instanceof MetricsInitEvent;
    }

    @Override
    public void onEvent(TimeCounterEvent event) {
        if (event instanceof MetricsInitEvent) {
            if (!metricsInitEnabled) {
                return;
            }
            if (initialized.compareAndSet(false, true)) {
                collectors.addAll(applicationModel.getBeanFactory().getBeansOfType(MetricsCollector.class));
            }
            collectors.stream().forEach(collector -> collector.initMetrics(event));
            return;
        }
        super.onEvent(event);
    }

    @Override
    public void initMetrics(MetricsEvent event) {
        MetricsPlaceValue dynamicPlaceType =
                MetricsPlaceValue.of(event.getAttachmentValue(MetricsConstants.INVOCATION_SIDE), MetricsLevel.METHOD);
        INIT_DEFAULT_METHOD_KEYS.stream()
                .forEach(key -> MetricsSupport.init(key, dynamicPlaceType, (MethodMetricsCollector) this, event));
        MetricsSupport.init(
                METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED,
                MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD),
                (MethodMetricsCollector) this,
                event);
    }

    public SimpleMetricsCountSampler<String, MetricsEvent.Type, ApplicationMetric> applicationSampler =
            new SimpleMetricsCountSampler<String, MetricsEvent.Type, ApplicationMetric>() {
                @Override
                public List<MetricSample> sample() {
                    List<MetricSample> samples = new ArrayList<>();
                    this.getCount(MetricsEvent.Type.APPLICATION_INFO)
                            .filter(e -> !e.isEmpty())
                            .ifPresent(map -> map.forEach((k, v) -> samples.add(new CounterMetricSample<>(
                                    APPLICATION_METRIC_INFO.getName(),
                                    APPLICATION_METRIC_INFO.getDescription(),
                                    k.getTags(),
                                    APPLICATION,
                                    v))));
                    return samples;
                }

                @Override
                protected void countConfigure(
                        MetricsCountSampleConfigurer<String, MetricsEvent.Type, ApplicationMetric> sampleConfigure) {
                    sampleConfigure.configureMetrics(configure -> new ApplicationMetric(applicationModel));
                }

                @Override
                public boolean calSamplesChanged() {
                    return false;
                }
            };

    @Override
    public boolean calSamplesChanged() {
        // CAS to get and reset the flag in an atomic operation
        boolean changed = samplesChanged.compareAndSet(true, false);
        // Should ensure that all the sampler's samplesChanged have been compareAndSet, and cannot flip the `or` logic
        changed = stats.calSamplesChanged() || changed;
        for (MetricsSampler sampler : samplers) {
            changed = sampler.calSamplesChanged() || changed;
        }
        return changed;
    }
}
