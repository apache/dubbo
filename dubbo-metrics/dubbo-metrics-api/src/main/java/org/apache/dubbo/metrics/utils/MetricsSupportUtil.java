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

    private static final String[] LEGACY_PROMETHEUS_STACK = {
        "io.micrometer.prometheus.PrometheusConfig",
        "io.micrometer.prometheus.PrometheusMeterRegistry",
        "io.prometheus.client.exporter.PushGateway",
        "io.prometheus.client.exporter.BasicAuthHttpConnectionFactory"
    };

    private static final String[] NEW_PROMETHEUS_STACK = {
        "io.micrometer.prometheusmetrics.PrometheusConfig",
        "io.micrometer.prometheusmetrics.PrometheusMeterRegistry",
        "io.prometheus.metrics.exporter.pushgateway.PushGateway"
    };

    public static boolean isSupportMetrics() {
        return isClassPresent("io.micrometer.core.instrument.MeterRegistry");
    }

    public static boolean isSupportPrometheus() {
        return isAllClassPresent(LEGACY_PROMETHEUS_STACK) || isAllClassPresent(NEW_PROMETHEUS_STACK);
    }

    public static boolean isSupportLegacyPrometheus() {
        return isAllClassPresent(LEGACY_PROMETHEUS_STACK);
    }

    public static boolean isSupportNewPrometheus() {
        return isAllClassPresent(NEW_PROMETHEUS_STACK);
    }

    private static boolean isClassPresent(String className) {
        return ClassUtils.isPresent(className, MetricsSupportUtil.class.getClassLoader());
    }

    private static boolean isAllClassPresent(String... classNames) {
        for (String className : classNames) {
            if (!isClassPresent(className)) {
                return false;
            }
        }
        return true;
    }
}
