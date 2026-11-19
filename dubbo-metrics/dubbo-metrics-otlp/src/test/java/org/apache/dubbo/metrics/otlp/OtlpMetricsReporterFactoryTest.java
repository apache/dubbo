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
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.config.nested.OtlpMetricConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.report.MetricsReporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_OTLP;

public class OtlpMetricsReporterFactoryTest {

    private MockWebServer otlpMockServer;
    private CopyOnWriteArrayList<byte[]> capturedRequests;
    private ApplicationModel applicationModel;
    private MetricsConfig metricsConfig;
    private FrameworkModel frameworkModel;

    @BeforeEach
    public void setup() {
        otlpMockServer = new MockWebServer();
        for (int i = 0; i < 100; i++) {
            otlpMockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{}")
                    .addHeader("Content-Type", "application/json"));
        }
        applicationModel = ApplicationModel.defaultModel();
        ConfigManager applicationConfigManager = applicationModel.getApplicationConfigManager();
        metricsConfig = new MetricsConfig();
        metricsConfig.setEnableJvm(true);
        metricsConfig.setProtocol(PROTOCOL_OTLP);
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setEnabled(false);
        metricsConfig.setAggregation(aggregationConfig);
        OtlpMetricConfig otlpMetricsConfig = new OtlpMetricConfig();
        otlpMetricsConfig.setStep(Duration.ofSeconds(1));
        metricsConfig.setOtlp(otlpMetricsConfig);
        otlpMetricsConfig.setUrl(otlpMockServer.url("/v1/metrics").toString());
        applicationConfigManager.setMetrics(metricsConfig);
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("test_app_name");
        applicationConfigManager.setApplication(applicationConfig);
        frameworkModel = FrameworkModel.defaultModel();
        frameworkModel.getBeanFactory().getOrRegisterBean(DefaultMetricsCollector.class);
    }

    @AfterEach
    public void teardown() throws IOException {
        applicationModel.destroy();
        if (otlpMockServer != null) {
            otlpMockServer.shutdown();
        }
    }

    @Test
    public void test_MetricsReporter() {

        OtlpMetricsReporterFactory factory = new OtlpMetricsReporterFactory(applicationModel);
        MetricsReporter reporter = factory.createMetricsReporter(metricsConfig.toUrl());
        Assertions.assertTrue(reporter instanceof OtlpMetricsReporter);
        applicationModel.destroy();
    }

    @Test
    public void test_MetricsReporter_with_not_match_protocol() {
        OtlpMetricsReporterFactory factory = new OtlpMetricsReporterFactory(applicationModel);
        try {
            factory.createMetricsReporter(metricsConfig.toUrl());
        } catch (Exception ex) {
            Assertions.assertInstanceOf(IllegalStateException.class, ex);
        }
    }

    @Test
    public void export_Test() throws IOException, InterruptedException {

        OtlpMetricsReporter reporter = new OtlpMetricsReporter(metricsConfig.toUrl(), applicationModel);
        reporter.init();
        try {

            List<String> requestBodies = new ArrayList<>();

            long endTime = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < endTime) {
                RecordedRequest request = otlpMockServer.takeRequest(500, TimeUnit.MILLISECONDS);
                if (request != null) {
                    String body = request.getBody().readString(StandardCharsets.UTF_8);
                    requestBodies.add(body);
                }
            }

            Assertions.assertFalse(requestBodies.isEmpty(), "Expected OTLP metrics to be pushed");

            boolean hasJvmMetrics = requestBodies.stream()
                    .anyMatch(body -> body.contains("jvm")
                            || body.contains("JVM")
                            || body.contains("memory")
                            || body.contains("gc")
                            || body.contains("threads"));

            Assertions.assertTrue(
                    hasJvmMetrics,
                    "Expected JVM metrics to be present in OTLP export.  Captured bodies: " + requestBodies.size());

        } finally {
            reporter.destroy();
        }
    }
}
