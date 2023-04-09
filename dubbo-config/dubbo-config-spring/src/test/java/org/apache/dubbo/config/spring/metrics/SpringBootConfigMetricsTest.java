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
package org.apache.dubbo.config.spring.metrics;

import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

@SpringBootTest(
    properties = {
        "dubbo.application.NAME = dubbo-demo-application",
        "dubbo.module.name = dubbo-demo-module",
        "dubbo.registry.address = zookeeper://localhost:2181",
        "dubbo.protocol.name=dubbo",
        "dubbo.protocol.port=20880",
        "dubbo.metrics.protocol=prometheus",
        "dubbo.metrics.export-service-protocol=tri",
        "dubbo.metrics.export-service-port=9999",
        "dubbo.metrics.enable-jvm=true",
        "dubbo.metrics.prometheus.exporter.enabled=true",
        "dubbo.metrics.prometheus.exporter.enable-http-service-discovery=true",
        "dubbo.metrics.prometheus.exporter.http-service-discovery-url=localhost:8080",
        "dubbo.metrics.prometheus.exporter.metrics-port=20888",
        "dubbo.metrics.prometheus.exporter.metrics-path=/metrics",
        "dubbo.metrics.aggregation.enabled=true",
        "dubbo.metrics.aggregation.bucket-num=5",
        "dubbo.metrics.aggregation.time-window-seconds=120",
        "dubbo.metrics.histogram.enabled=true",
        "dubbo.metadata-report.address=${zookeeper.connection.address.2}"
    },
    classes = {
        SpringBootConfigMetricsTest.class
    }
)
@Configuration
@ComponentScan
@EnableDubbo
public class SpringBootConfigMetricsTest {

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
    }

    @Autowired
    private ConfigManager configManager;

    @Test
    public void testMetrics() {
        MetricsConfig metricsConfig = configManager.getMetrics().get();

        Assertions.assertEquals(PROTOCOL_PROMETHEUS, metricsConfig.getProtocol());
        Assertions.assertTrue(metricsConfig.getEnableJvm());
        Assertions.assertEquals("tri",metricsConfig.getExportServiceProtocol());
        Assertions.assertEquals(9999, metricsConfig.getExportServicePort());
        Assertions.assertTrue(metricsConfig.getPrometheus().getExporter().getEnabled());
        Assertions.assertTrue(metricsConfig.getPrometheus().getExporter().getEnableHttpServiceDiscovery());
        Assertions.assertEquals("localhost:8080", metricsConfig.getPrometheus().getExporter().getHttpServiceDiscoveryUrl());
        Assertions.assertEquals(20888, metricsConfig.getPrometheus().getExporter().getMetricsPort());
        Assertions.assertEquals("/metrics", metricsConfig.getPrometheus().getExporter().getMetricsPath());
        Assertions.assertEquals(5, metricsConfig.getAggregation().getBucketNum());
        Assertions.assertEquals(120, metricsConfig.getAggregation().getTimeWindowSeconds());
        Assertions.assertTrue(metricsConfig.getAggregation().getEnabled());
        Assertions.assertTrue(metricsConfig.getHistogram().getEnabled());
    }

}
