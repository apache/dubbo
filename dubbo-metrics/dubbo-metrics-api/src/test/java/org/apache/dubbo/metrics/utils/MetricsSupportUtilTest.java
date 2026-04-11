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
package org.apache.dubbo.metrics.utils;

import org.apache.dubbo.common.utils.ClassUtils;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class MetricsSupportUtilTest {

    private static final String LEGACY_PROMETHEUS_CONFIG = "io.micrometer.prometheus.PrometheusConfig";
    private static final String NEW_PROMETHEUS_CONFIG = "io.micrometer.prometheusmetrics.PrometheusConfig";
    private static final String METER_REGISTRY = "io.micrometer.core.instrument.MeterRegistry";

    @Test
    void isSupportMetricsResolvesOnRealClasspath() {
        // micrometer-core is a compile-scope dep of dubbo-metrics-api, so the real
        // classpath must report MeterRegistry as present.
        assertTrue(MetricsSupportUtil.isSupportMetrics());
    }

    @Test
    void isSupportPrometheusAcceptsLegacyMicrometerPackage() {
        // Simulate Spring Boot <= 3.2 / Micrometer < 1.13, where only the old
        // io.micrometer.prometheus.* package is on the classpath.
        try (MockedStatic<ClassUtils> mocked = mockStatic(ClassUtils.class)) {
            mocked.when(() -> ClassUtils.isPresent(eq(LEGACY_PROMETHEUS_CONFIG), any(ClassLoader.class)))
                    .thenReturn(true);
            mocked.when(() -> ClassUtils.isPresent(eq(NEW_PROMETHEUS_CONFIG), any(ClassLoader.class)))
                    .thenReturn(false);
            assertTrue(MetricsSupportUtil.isSupportPrometheus());
        }
    }

    @Test
    void isSupportPrometheusAcceptsNewMicrometerPackage() {
        // Simulate Spring Boot 3.3+ / Micrometer 1.13+, where the package was
        // renamed to io.micrometer.prometheusmetrics.*. This was the regression
        // reported in apache/dubbo#16109.
        try (MockedStatic<ClassUtils> mocked = mockStatic(ClassUtils.class)) {
            mocked.when(() -> ClassUtils.isPresent(eq(LEGACY_PROMETHEUS_CONFIG), any(ClassLoader.class)))
                    .thenReturn(false);
            mocked.when(() -> ClassUtils.isPresent(eq(NEW_PROMETHEUS_CONFIG), any(ClassLoader.class)))
                    .thenReturn(true);
            assertTrue(MetricsSupportUtil.isSupportPrometheus());
        }
    }

    @Test
    void isSupportPrometheusAcceptsBothMicrometerPackages() {
        // Simulate a transitional build where both the legacy and the new
        // registries happen to be on the classpath.
        try (MockedStatic<ClassUtils> mocked = mockStatic(ClassUtils.class)) {
            mocked.when(() -> ClassUtils.isPresent(eq(LEGACY_PROMETHEUS_CONFIG), any(ClassLoader.class)))
                    .thenReturn(true);
            mocked.when(() -> ClassUtils.isPresent(eq(NEW_PROMETHEUS_CONFIG), any(ClassLoader.class)))
                    .thenReturn(true);
            assertTrue(MetricsSupportUtil.isSupportPrometheus());
        }
    }

    @Test
    void isSupportPrometheusReturnsFalseWhenNoMicrometerPrometheusOnClasspath() {
        // Simulate an application that depends on dubbo-metrics-api without any
        // Micrometer Prometheus registry. The check must return false so the
        // reporter stays disabled rather than crashing later.
        try (MockedStatic<ClassUtils> mocked = mockStatic(ClassUtils.class)) {
            mocked.when(() -> ClassUtils.isPresent(eq(LEGACY_PROMETHEUS_CONFIG), any(ClassLoader.class)))
                    .thenReturn(false);
            mocked.when(() -> ClassUtils.isPresent(eq(NEW_PROMETHEUS_CONFIG), any(ClassLoader.class)))
                    .thenReturn(false);
            assertFalse(MetricsSupportUtil.isSupportPrometheus());
        }
    }

    @Test
    void isSupportPrometheusDoesNotRequireLegacyPushGatewayClasses() {
        // Before apache/dubbo#16109 the check also required
        // io.prometheus.client.exporter.* classes. Verify that their absence no
        // longer suppresses Prometheus support when Micrometer is available.
        try (MockedStatic<ClassUtils> mocked = mockStatic(ClassUtils.class)) {
            mocked.when(() -> ClassUtils.isPresent(eq(NEW_PROMETHEUS_CONFIG), any(ClassLoader.class)))
                    .thenReturn(true);
            mocked.when(() -> ClassUtils.isPresent(eq(LEGACY_PROMETHEUS_CONFIG), any(ClassLoader.class)))
                    .thenReturn(false);
            // Everything else (including io.prometheus.client.*) is still reported
            // absent by default because we have not stubbed it.
            assertTrue(MetricsSupportUtil.isSupportPrometheus());
        }
    }

    @Test
    void isSupportMetricsDelegatesToClassUtils() {
        // Guard against a future refactor dropping the MeterRegistry probe.
        try (MockedStatic<ClassUtils> mocked = mockStatic(ClassUtils.class)) {
            mocked.when(() -> ClassUtils.isPresent(eq(METER_REGISTRY), any(ClassLoader.class)))
                    .thenReturn(false);
            assertFalse(MetricsSupportUtil.isSupportMetrics());
        }
    }
}
