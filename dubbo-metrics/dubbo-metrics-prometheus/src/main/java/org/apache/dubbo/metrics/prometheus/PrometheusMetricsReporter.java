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

package org.apache.dubbo.metrics.prometheus;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.BasicAuthHttpConnectionFactory;
import io.prometheus.client.exporter.PushGateway;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metrics.report.AbstractMetricsReporter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION;
import static org.apache.dubbo.common.constants.MetricsConstants.PROMETHEUS_DEFAULT_JOB_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.PROMETHEUS_DEFAULT_PUSH_INTERVAL;
import static org.apache.dubbo.common.constants.MetricsConstants.PROMETHEUS_PUSHGATEWAY_BASE_URL_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.PROMETHEUS_PUSHGATEWAY_ENABLED_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.PROMETHEUS_PUSHGATEWAY_JOB_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.PROMETHEUS_PUSHGATEWAY_PASSWORD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.PROMETHEUS_PUSHGATEWAY_PUSH_INTERVAL_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.PROMETHEUS_PUSHGATEWAY_USERNAME_KEY;

/**
 * Metrics reporter for prometheus.
 */
public class PrometheusMetricsReporter extends AbstractMetricsReporter {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(PrometheusMetricsReporter.class);

    private final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    private ScheduledExecutorService pushJobExecutor = null;

    public PrometheusMetricsReporter(URL url, ApplicationModel applicationModel) {
        super(url, applicationModel);
    }

    @Override
    public void doInit() {
        addMeterRegistry(prometheusRegistry);
        schedulePushJob();
    }

    public String getResponse() {
        return prometheusRegistry.scrape();
    }

    private void schedulePushJob() {
        boolean pushEnabled = url.getParameter(PROMETHEUS_PUSHGATEWAY_ENABLED_KEY, false);
        if (pushEnabled) {
            String baseUrl = url.getParameter(PROMETHEUS_PUSHGATEWAY_BASE_URL_KEY);
            String job = url.getParameter(PROMETHEUS_PUSHGATEWAY_JOB_KEY, PROMETHEUS_DEFAULT_JOB_NAME);
            int pushInterval = url.getParameter(PROMETHEUS_PUSHGATEWAY_PUSH_INTERVAL_KEY, PROMETHEUS_DEFAULT_PUSH_INTERVAL);
            String username = url.getParameter(PROMETHEUS_PUSHGATEWAY_USERNAME_KEY);
            String password = url.getParameter(PROMETHEUS_PUSHGATEWAY_PASSWORD_KEY);

            NamedThreadFactory threadFactory = new NamedThreadFactory("prometheus-push-job", true);
            pushJobExecutor = Executors.newScheduledThreadPool(1, threadFactory);
            PushGateway pushGateway = new PushGateway(baseUrl);
            if (!StringUtils.isBlank(username)) {
                pushGateway.setConnectionFactory(new BasicAuthHttpConnectionFactory(username, password));
            }

            pushJobExecutor.scheduleWithFixedDelay(() -> push(pushGateway, job), pushInterval, pushInterval, TimeUnit.SECONDS);
        }
    }

    protected void push(PushGateway pushGateway, String job) {
        try {
            refreshData();
            pushGateway.pushAdd(prometheusRegistry.getPrometheusRegistry(), job);
        } catch (IOException e) {
            logger.error(COMMON_METRICS_COLLECTOR_EXCEPTION, "", "", "Error occurred when pushing metrics to prometheus: ", e);
        }
    }

    @Override
    public void doDestroy() {

        if (pushJobExecutor != null) {
            pushJobExecutor.shutdownNow();
        }
    }

    /**
     * ut only
     */
    @Deprecated
    public ScheduledExecutorService getPushJobExecutor() {
        return pushJobExecutor;
    }

    /**
     * ut only
     */
    @Deprecated
    public PrometheusMeterRegistry getPrometheusRegistry() {
        return prometheusRegistry;
    }
}
