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

public class MetricsSupportUtil {

    /**
     * Legacy Micrometer Prometheus config class, present in
     * {@code micrometer-registry-prometheus-simpleclient} and in
     * {@code micrometer-registry-prometheus} prior to Micrometer 1.13.
     */
    private static final String LEGACY_PROMETHEUS_CONFIG_CLASS = "io.micrometer.prometheus.PrometheusConfig";

    /**
     * New Micrometer Prometheus config class, introduced in Micrometer 1.13
     * (Spring Boot 3.3+) when the {@code micrometer-registry-prometheus}
     * artifact was re-homed under the {@code io.micrometer.prometheusmetrics}
     * package.
     */
    private static final String NEW_PROMETHEUS_CONFIG_CLASS = "io.micrometer.prometheusmetrics.PrometheusConfig";

    public static boolean isSupportMetrics() {
        return isClassPresent("io.micrometer.core.instrument.MeterRegistry");
    }

    /**
     * Detect whether a Micrometer Prometheus registry is available on the
     * classpath. Either the legacy {@code io.micrometer.prometheus} package
     * (Micrometer &lt; 1.13 and {@code micrometer-registry-prometheus-simpleclient})
     * or the new {@code io.micrometer.prometheusmetrics} package
     * (Micrometer 1.13+, Spring Boot 3.3+) is accepted so that users on
     * recent Spring Boot releases are not blocked from initializing the
     * Prometheus reporter.
     *
     * <p>Previously this method also required the legacy PushGateway
     * classes under {@code io.prometheus.client.exporter.*}. Those are only
     * needed when Pushgateway mode is enabled, and {@code
     * PrometheusMetricsReporterFactory} already guards against their
     * absence via {@code NoClassDefFoundError} handling, so a hard
     * requirement here produced false negatives for scrape-only
     * deployments shipped with {@code dubbo-observability-spring-boot-starter}
     * in Dubbo 3.3.x.
     */
    public static boolean isSupportPrometheus() {
        return isClassPresent(LEGACY_PROMETHEUS_CONFIG_CLASS) || isClassPresent(NEW_PROMETHEUS_CONFIG_CLASS);
    }

    private static boolean isClassPresent(String className) {
        return ClassUtils.isPresent(className, MetricsSupportUtil.class.getClassLoader());
    }
}
