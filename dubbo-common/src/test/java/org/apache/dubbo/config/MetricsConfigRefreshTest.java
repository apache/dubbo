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
package org.apache.dubbo.config;

import org.apache.dubbo.config.context.ConfigMode;
import org.apache.dubbo.config.nested.AggregationConfig;
import org.apache.dubbo.config.nested.PrometheusConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies nested metrics properties follow the configuration refresh mode.
 *
 * @date 2026-10-18
 */
class MetricsConfigRefreshTest {

    private final String previousConfigMode = System.getProperty(ConfigKeys.DUBBO_CONFIG_MODE);

    private FrameworkModel frameworkModel;

    @AfterEach
    void tearDown() {
        try {
            if (frameworkModel != null) {
                frameworkModel.destroy();
            }
        } finally {
            if (previousConfigMode == null) {
                System.clearProperty(ConfigKeys.DUBBO_CONFIG_MODE);
            } else {
                System.setProperty(ConfigKeys.DUBBO_CONFIG_MODE, previousConfigMode);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = ConfigMode.class,
            names = {"STRICT", "OVERRIDE_ALL", "OVERRIDE_IF_ABSENT"})
    void refreshRespectsConfigModeForNestedProperties(ConfigMode mode) {
        Map<String, String> external = new HashMap<>();
        external.put("dubbo.metrics.enable-jvm", "false");
        external.put("dubbo.metrics.aggregation.enabled", "true");
        external.put("dubbo.metrics.aggregation.bucket-num", "10");
        external.put("dubbo.metrics.aggregation.time-window-seconds", "120");
        MetricsConfig metrics = newMetricsConfig(mode, external);
        metrics.setEnableJvm(true);
        AggregationConfig aggregation = new AggregationConfig();
        aggregation.setEnabled(false);
        aggregation.setBucketNum(5);
        metrics.setAggregation(aggregation);

        metrics.refresh();

        boolean preserveExisting = mode == ConfigMode.OVERRIDE_IF_ABSENT;
        assertEquals(preserveExisting, metrics.getEnableJvm());
        assertEquals(!preserveExisting, metrics.getAggregation().getEnabled());
        assertEquals(preserveExisting ? 5 : 10, metrics.getAggregation().getBucketNum());
        assertEquals(120, metrics.getAggregation().getTimeWindowSeconds());
    }

    @ParameterizedTest
    @EnumSource(
            value = ConfigMode.class,
            names = {"STRICT", "OVERRIDE_ALL", "OVERRIDE_IF_ABSENT"})
    void refreshRespectsConfigModeForDeeplyNestedProperties(ConfigMode mode) {
        Map<String, String> external = new HashMap<>();
        external.put("dubbo.metrics.prometheus.exporter.enabled", "true");
        external.put("dubbo.metrics.prometheus.exporter.enable-http-service-discovery", "true");
        MetricsConfig metrics = newMetricsConfig(mode, external);
        PrometheusConfig prometheus = new PrometheusConfig();
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        exporter.setEnabled(false);
        prometheus.setExporter(exporter);
        metrics.setPrometheus(prometheus);

        metrics.refresh();

        assertEquals(
                mode != ConfigMode.OVERRIDE_IF_ABSENT,
                metrics.getPrometheus().getExporter().getEnabled());
        assertEquals(true, metrics.getPrometheus().getExporter().getEnableHttpServiceDiscovery());
    }

    @ParameterizedTest
    @EnumSource(
            value = ConfigMode.class,
            names = {"STRICT", "OVERRIDE_ALL", "OVERRIDE_IF_ABSENT"})
    void refreshInitializesMissingNestedConfig(ConfigMode mode) {
        Map<String, String> external = new HashMap<>();
        external.put("dubbo.metrics.aggregation.bucket-num", "10");
        external.put("dubbo.metrics.prometheus.exporter.enabled", "true");
        MetricsConfig metrics = newMetricsConfig(mode, external);

        metrics.refresh();

        assertEquals(10, metrics.getAggregation().getBucketNum());
        assertEquals(true, metrics.getPrometheus().getExporter().getEnabled());
    }

    private MetricsConfig newMetricsConfig(ConfigMode mode, Map<String, String> external) {
        System.setProperty(ConfigKeys.DUBBO_CONFIG_MODE, mode.name());
        frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        applicationModel.modelEnvironment().setExternalConfigMap(external);
        MetricsConfig metrics = new MetricsConfig(applicationModel);
        assertEquals(mode, metrics.getConfigMode());
        return metrics;
    }
}
