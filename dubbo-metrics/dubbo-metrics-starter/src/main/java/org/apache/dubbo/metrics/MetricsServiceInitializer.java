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

import org.apache.dubbo.common.event.AbstractDubboListener;
import org.apache.dubbo.common.event.DubboListener;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.deploy.event.ApplicationLoadedEvent;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.report.DefaultMetricsReporterFactory;
import org.apache.dubbo.metrics.report.MetricsReporter;
import org.apache.dubbo.metrics.report.MetricsReporterFactory;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;
import org.apache.dubbo.metrics.utils.MetricsSupportUtil;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.tracing.DubboObservationRegistry;
import org.apache.dubbo.tracing.utils.ObservationSupportUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_DEFAULT;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

/**
 * Initialize the metrics service.
 */
public class MetricsServiceInitializer {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(MetricsServiceInitializer.class);

    private final ApplicationModel applicationModel;

    private final MetricsServiceExporter metricsServiceExporter;

    private final DefaultMetricsCollector defaultCollector;

    private final List<DubboListener<?>> dubboListeners;

    public MetricsServiceInitializer(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.defaultCollector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
        this.metricsServiceExporter = applicationModel
                .getExtensionLoader(MetricsServiceExporter.class)
                .getDefaultExtension();
        dubboListeners = Arrays.asList(new AbstractDubboListener<ApplicationLoadedEvent>() {
            @Override
            public void onEvent(ApplicationLoadedEvent event) {

                if (MetricsSupportUtil.isSupportMetrics()) {
                    MetricsConfig metricsConfig = initMetricsConfig();
                    if (!PROTOCOL_PROMETHEUS.equals(metricsConfig.getProtocol())
                            || MetricsSupportUtil.isSupportPrometheus()) {
                        initDefaultMetricsCollector(metricsConfig);
                        initMetricsReporter(metricsConfig);
                    }
                }

                initMetricsService();

                // @since 3.2.3
                initObservationRegistry();
            }
        });
    }

    public List<DubboListener<?>> getDubboListeners() {
        return dubboListeners;
    }

    private void initMetricsService() {
        this.applicationModel.getBeanFactory().registerBean(this.metricsServiceExporter);
        this.metricsServiceExporter.init();
    }

    private MetricsConfig initMetricsConfig() {
        Optional<MetricsConfig> configOptional =
                applicationModel.getApplicationConfigManager().getMetrics();
        // If no specific metrics type is configured and there is no Prometheus dependency in the dependencies.
        MetricsConfig metricsConfig = configOptional.orElse(new MetricsConfig(applicationModel));
        if (StringUtils.isBlank(metricsConfig.getProtocol())) {
            metricsConfig.setProtocol(
                    MetricsSupportUtil.isSupportPrometheus() ? PROTOCOL_PROMETHEUS : PROTOCOL_DEFAULT);
        }
        return metricsConfig;
    }

    private void initDefaultMetricsCollector(MetricsConfig metricsConfig) {
        defaultCollector.setCollectEnabled(true);
        defaultCollector.collectApplication();
        defaultCollector.setThreadpoolCollectEnabled(
                Optional.ofNullable(metricsConfig.getEnableThreadpool()).orElse(true));
        defaultCollector.setMetricsInitEnabled(
                Optional.ofNullable(metricsConfig.getEnableMetricsInit()).orElse(true));
    }

    private void initMetricsReporter(MetricsConfig metricsConfig) {
        MetricsReporterFactory metricsReporterFactory = applicationModel
                .getExtensionLoader(MetricsReporterFactory.class)
                .getAdaptiveExtension();
        MetricsReporter metricsReporter = null;
        try {
            metricsReporter = metricsReporterFactory.createMetricsReporter(metricsConfig.toUrl());
        } catch (IllegalStateException e) {
            if (e.getMessage().startsWith("No such extension org.apache.dubbo.metrics.report.MetricsReporterFactory")) {
                logger.warn(COMMON_METRICS_COLLECTOR_EXCEPTION, "", "", e.getMessage());
                return;
            } else {
                throw e;
            }
        }
        metricsReporter.init();
        applicationModel.getBeanFactory().registerBean(metricsReporter);
        // If the protocol is not the default protocol, the default protocol is also initialized.
        if (!PROTOCOL_DEFAULT.equals(metricsConfig.getProtocol())) {
            DefaultMetricsReporterFactory defaultMetricsReporterFactory =
                    new DefaultMetricsReporterFactory(applicationModel);
            MetricsReporter defaultMetricsReporter =
                    defaultMetricsReporterFactory.createMetricsReporter(metricsConfig.toUrl());
            defaultMetricsReporter.init();
            applicationModel.getBeanFactory().registerBean(defaultMetricsReporter);
        }
    }

    /**
     * init ObservationRegistry(Micrometer)
     */
    private void initObservationRegistry() {
        if (!ObservationSupportUtil.isSupportObservation()) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Not found micrometer-observation or plz check the version of micrometer-observation version if already introduced, need > 1.10.0");
            }
            return;
        }
        if (!ObservationSupportUtil.isSupportTracing()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not found micrometer-tracing dependency, skip init ObservationRegistry.");
            }
            return;
        }
        Optional<TracingConfig> configOptional =
                applicationModel.getApplicationConfigManager().getTracing();
        if (!configOptional.isPresent() || !configOptional.get().getEnabled()) {
            return;
        }

        DubboObservationRegistry dubboObservationRegistry =
                new DubboObservationRegistry(applicationModel, configOptional.get());
        dubboObservationRegistry.initObservationRegistry();
    }
}
