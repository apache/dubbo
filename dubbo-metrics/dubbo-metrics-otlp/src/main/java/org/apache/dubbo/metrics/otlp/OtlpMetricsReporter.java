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
package org.apache.dubbo.metrics.otlp;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.MetricsConstants;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.nested.OtlpMetricConfig;
import org.apache.dubbo.metrics.report.AbstractMetricsReporter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;

/**
 * Metrics reporter for Otlp.
 */
public class OtlpMetricsReporter extends AbstractMetricsReporter {

    private final OtlpMeterRegistry otlpMeterRegistry;

    public OtlpMetricsReporter(final URL url, ApplicationModel applicationModel) {
        super(url, applicationModel);
        Optional<MetricsConfig> configOptional =
                applicationModel.getApplicationConfigManager().getMetrics();
        // If no specific metrics type is configured and there is no Prometheus dependency in the dependencies.
        MetricsConfig metricsConfig = configOptional.orElse(new MetricsConfig(applicationModel));
        OtlpMetricConfig otlpMetricConfig = metricsConfig.getOtlp();
        // check protocol whether is otlp
        if (otlpMetricConfig == null || !MetricsConstants.PROTOCOL_OTLP.equals(metricsConfig.getProtocol())) {
            throw new IllegalStateException(
                    "Otlp metrics reporter config is required oltp protocol but real does not match.");
        }

        OtlpConfig config = new OtlpWrapperConfig(otlpMetricConfig, applicationModel);
        this.otlpMeterRegistry = new OtlpMeterRegistry(config, Clock.SYSTEM);
    }

    @Override
    public void doInit() {
        addMeterRegistry(this.otlpMeterRegistry);
    }

    @Override
    public String getResponse() {
        return null;
    }

    @Override
    public void doDestroy() {
        if (this.otlpMeterRegistry != null) {
            this.otlpMeterRegistry.close();
        }
    }

    public static class OtlpWrapperConfig implements OtlpConfig {

        private final OtlpMetricConfig otlpMetricConfig;
        private final ApplicationModel applicationModel;

        public OtlpWrapperConfig(OtlpMetricConfig otlpMetricConfig, ApplicationModel applicationModel) {
            this.otlpMetricConfig = otlpMetricConfig;
            this.applicationModel = applicationModel;
        }

        @Override
        public String get(String key) {
            // just use default value
            return OtlpConfig.DEFAULT.get(key);
        }

        @Override
        public String url() {
            if (this.otlpMetricConfig.getEndpoint() == null) {
                return OtlpConfig.super.url();
            }
            return this.otlpMetricConfig.getEndpoint();
        }

        @Override
        public Duration step() {
            if (this.otlpMetricConfig.getStep() == null) {
                return OtlpConfig.super.step();
            }
            return this.otlpMetricConfig.getStep();
        }

        @Override
        public Map<String, String> resourceAttributes() {
            Map<String, String> resourceAttributes = this.otlpMetricConfig.getResourceAttributes();
            if (resourceAttributes == null) {
                resourceAttributes = OtlpConfig.super.resourceAttributes();
            }
            // set service.name
            resourceAttributes.computeIfAbsent("service.name", (key) -> getApplicationName());
            return resourceAttributes;
        }

        @Override
        public AggregationTemporality aggregationTemporality() {
            return OtlpConfig.super.aggregationTemporality();
        }

        @Override
        public Map<String, String> headers() {
            Map<String, String> headers = this.otlpMetricConfig.getHeaders();
            if (headers == null) {
                headers = OtlpConfig.super.headers();
            }
            return headers;
        }

        @Override
        public TimeUnit baseTimeUnit() {
            return this.otlpMetricConfig.getBaseTimeUnit();
        }

        private String getApplicationName() {
            return this.applicationModel.getApplicationName();
        }
    }
}
