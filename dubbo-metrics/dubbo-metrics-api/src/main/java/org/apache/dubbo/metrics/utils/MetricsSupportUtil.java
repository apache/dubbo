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

    public static boolean isSupportMetrics() {
        return isClassPresent("io.micrometer.core.instrument.MeterRegistry");
    }

    public static boolean isSupportPrometheus() {
        return isClassPresent("io.micrometer.prometheus.PrometheusConfig")
                && isClassPresent("io.prometheus.client.exporter.BasicAuthHttpConnectionFactory")
                && isClassPresent("io.prometheus.client.exporter.HttpConnectionFactory")
                && isClassPresent("io.prometheus.client.exporter.PushGateway");
    }

    private static boolean isClassPresent(String className) {
        return ClassUtils.isPresent(className, MetricsSupportUtil.class.getClassLoader());
    }
}
