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
package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.report.DefaultMetricsReporterFactory;
import org.apache.dubbo.metrics.report.MetricsReporter;
import org.apache.dubbo.metrics.report.MetricsReporterFactory;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;
import org.apache.dubbo.metrics.utils.MetricsSupportUtil;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Optional;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_DEFAULT;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;
import static org.apache.dubbo.metrics.utils.MetricsSupportUtil.isSupportPrometheus;

@Activate(order = -900)
public class MetricsInitializeLifecycle implements ApplicationLifecycle {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MetricsInitializeLifecycle.class);

    private MetricsServiceExporter metricsServiceExporter;

    /**
     * If this lifecycle need to initialize.
     */
    @Override
    public boolean needInitialize(ApplicationContext context) {
        return true;
    }

    /**
     * Initialize.
     */
    @Override
    public void initialize(ApplicationContext applicationContext) {
        initMetricsReporter(applicationContext);
        initMetricsService(applicationContext.getModel());
    }

    private void initMetricsReporter(ApplicationContext applicationContext) {
        ApplicationModel applicationModel = applicationContext.getModel();

        if (!MetricsSupportUtil.isSupportMetrics()) {
            return;
        }

        DefaultMetricsCollector collector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
        Optional<MetricsConfig> configOptional = applicationModel.getApplicationConfigManager().getMetrics();

        //If no specific metrics type is configured and there is no Prometheus dependency in the dependencies.
        MetricsConfig metricsConfig = configOptional.orElse(new MetricsConfig(applicationModel));

        if (StringUtils.isBlank(metricsConfig.getProtocol())) {
            metricsConfig.setProtocol(isSupportPrometheus() ? PROTOCOL_PROMETHEUS : PROTOCOL_DEFAULT);
        }

        collector.setCollectEnabled(true);
        collector.collectApplication();
        collector.setThreadpoolCollectEnabled(Optional.ofNullable(metricsConfig.getEnableThreadpool()).orElse(true));

        MetricsReporterFactory metricsReporterFactory = applicationModel.getExtensionLoader(MetricsReporterFactory.class).getAdaptiveExtension();
        MetricsReporter metricsReporter;

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

        //If the protocol is not the default protocol, the default protocol is also initialized.
        if (!PROTOCOL_DEFAULT.equals(metricsConfig.getProtocol())) {
            DefaultMetricsReporterFactory defaultMetricsReporterFactory = new DefaultMetricsReporterFactory(applicationModel);
            MetricsReporter defaultMetricsReporter = defaultMetricsReporterFactory.createMetricsReporter(metricsConfig.toUrl());
            defaultMetricsReporter.init();
            applicationModel.getBeanFactory().registerBean(defaultMetricsReporter);
        }
    }

    private void initMetricsService(ApplicationModel applicationModel) {
        this.metricsServiceExporter = applicationModel.getBeanFactory().getBean(MetricsServiceExporter.class);

        if (this.metricsServiceExporter == null) {
            this.metricsServiceExporter = applicationModel.getExtensionLoader(MetricsServiceExporter.class).getDefaultExtension();
            applicationModel.getBeanFactory().registerBean(this.metricsServiceExporter);
        }
        this.metricsServiceExporter.init();
    }


}
