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
package org.apache.dubbo.tracing.utils;

import org.apache.dubbo.common.utils.ClassUtils;

public class ObservationSupportUtil {

    public static boolean isSupportObservation() {
        return isClassPresent("io.micrometer.observation.Observation")
                && isClassPresent("io.micrometer.observation.ObservationRegistry")
                && isClassPresent("io.micrometer.observation.ObservationHandler");
    }

    public static boolean isSupportTracing() {
        return isClassPresent("io.micrometer.tracing.Tracer")
                && isClassPresent("io.micrometer.tracing.propagation.Propagator");
    }

    public static boolean isSupportOTelTracer() {
        return isClassPresent("io.micrometer.tracing.otel.bridge.OtelTracer")
                && isClassPresent("io.opentelemetry.sdk.trace.SdkTracerProvider")
                && isClassPresent("io.opentelemetry.api.OpenTelemetry");
    }

    public static boolean isSupportBraveTracer() {
        return isClassPresent("io.micrometer.tracing.Tracer")
                && isClassPresent("io.micrometer.tracing.brave.bridge.BraveTracer")
                && isClassPresent("brave.Tracing");
    }

    public static boolean isSupportBraveURLSender() {
        return isClassPresent("zipkin2.reporter.urlconnection.URLConnectionSender");
    }

    private static boolean isClassPresent(String className) {
        return ClassUtils.isPresent(className, ObservationSupportUtil.class.getClassLoader());
    }
}
