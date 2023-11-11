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
package org.apache.dubbo.tracing.tracer.otel;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.nested.BaggageConfig;
import org.apache.dubbo.config.nested.ExporterConfig;
import org.apache.dubbo.config.nested.PropagationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.tracing.tracer.TracerProvider;
import org.apache.dubbo.tracing.tracer.TracerProviderFactory;
import org.apache.dubbo.tracing.utils.PropagationType;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenTelemetryProviderTest {

    @Test
    void testGetTracer() {
        TracingConfig tracingConfig = new TracingConfig();
        tracingConfig.setEnabled(true);
        ExporterConfig exporterConfig = new ExporterConfig();
        exporterConfig.setZipkinConfig(new ExporterConfig.ZipkinConfig(""));
        tracingConfig.setTracingExporter(exporterConfig);
        TracerProvider tracerProvider1 =
                TracerProviderFactory.getProvider(ApplicationModel.defaultModel(), tracingConfig);
        Assert.notNull(tracerProvider1, "TracerProvider should not be null.");
        Tracer tracer1 = tracerProvider1.getTracer();
        assertEquals(OtelTracer.class, tracer1.getClass());

        tracingConfig.setBaggage(new BaggageConfig(false));
        TracerProvider tracerProvider2 =
                TracerProviderFactory.getProvider(ApplicationModel.defaultModel(), tracingConfig);
        Assert.notNull(tracerProvider2, "TracerProvider should not be null.");
        Tracer tracer2 = tracerProvider2.getTracer();
        assertEquals(OtelTracer.class, tracer2.getClass());
    }

    @Test
    void testGetPropagator() {
        PropagationConfig propagationConfig = mock(PropagationConfig.class);
        BaggageConfig baggageConfig = mock(BaggageConfig.class);
        OtelCurrentTraceContext otelCurrentTraceContext = mock(OtelCurrentTraceContext.class);

        when(baggageConfig.getEnabled()).thenReturn(Boolean.FALSE);

        when(propagationConfig.getType()).thenReturn(PropagationType.B3.getValue());
        TextMapPropagator b3PropagatorWithoutBaggage = OpenTelemetryProvider.PropagatorFactory.getPropagator(
                propagationConfig, baggageConfig, otelCurrentTraceContext);
        assertEquals(B3Propagator.class, b3PropagatorWithoutBaggage.getClass());

        when(propagationConfig.getType()).thenReturn(PropagationType.W3C.getValue());
        TextMapPropagator w3cPropagatorWithoutBaggage = OpenTelemetryProvider.PropagatorFactory.getPropagator(
                propagationConfig, baggageConfig, otelCurrentTraceContext);
        assertEquals(W3CTraceContextPropagator.class, w3cPropagatorWithoutBaggage.getClass());

        when(baggageConfig.getEnabled()).thenReturn(Boolean.TRUE);
        TextMapPropagator propagatorWithBaggage = OpenTelemetryProvider.PropagatorFactory.getPropagator(
                propagationConfig, baggageConfig, otelCurrentTraceContext);
        Assert.notNull(propagatorWithBaggage, "PropagatorWithBaggage should not be null");
    }
}
