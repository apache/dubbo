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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.OtlpMetricConfig;
import org.apache.dubbo.metrics.report.MetricsReporter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OtlpMetricsReporterFactoryTest {

    @Test
    public void test_MetricsReporter() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ConfigManager applicationConfigManager = applicationModel.getApplicationConfigManager();
        MetricsConfig config = new MetricsConfig();
        config.setProtocol("otlp");
        OtlpMetricConfig otlpMetricsConfig = new OtlpMetricConfig();
        otlpMetricsConfig.setUrl("http://localhost:4318/v1/metrics");
        config.setOtlp(otlpMetricsConfig);
        applicationConfigManager.setMetrics(config);
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("test_app_name");
        applicationConfigManager.setApplication(applicationConfig);
        OtlpMetricsReporterFactory factory = new OtlpMetricsReporterFactory(applicationModel);
        MetricsReporter reporter = factory.createMetricsReporter(config.toUrl());
        Assertions.assertTrue(reporter instanceof OtlpMetricsReporter);
    }

    @Test
    public void test_MetricsReporter_with_not_match_protocol() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        OtlpMetricsReporterFactory factory = new OtlpMetricsReporterFactory(applicationModel);
        MetricsConfig config = new MetricsConfig();
        OtlpMetricConfig otlpMetricsConfig = new OtlpMetricConfig();
        otlpMetricsConfig.setUrl("http://localhost:4318/v1/metrics");
        config.setOtlp(otlpMetricsConfig);
        try {
            factory.createMetricsReporter(config.toUrl());
        } catch (Exception ex) {
            Assertions.assertInstanceOf(IllegalStateException.class, ex);
        }
    }
}
