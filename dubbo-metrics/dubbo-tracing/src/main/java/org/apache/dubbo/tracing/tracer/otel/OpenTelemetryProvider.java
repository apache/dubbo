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

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.nested.BaggageConfig;
import org.apache.dubbo.config.nested.PropagationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.tracing.exporter.TraceExporterFactory;
import org.apache.dubbo.tracing.tracer.TracerProvider;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.CompositeSpanExporter;
import io.micrometer.tracing.otel.bridge.EventListener;
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenTelemetryProvider implements TracerProvider {

    private static final String DEFAULT_APPLICATION_NAME = "dubbo-application";
    private final ApplicationModel applicationModel;
    private final TracingConfig tracingConfig;

    private OTelEventPublisher publisher;
    private OtelCurrentTraceContext otelCurrentTraceContext;

    public OpenTelemetryProvider(ApplicationModel applicationModel, TracingConfig tracingConfig) {
        this.applicationModel = applicationModel;
        this.tracingConfig = tracingConfig;
    }

    @Override
    public Tracer getTracer() {
        // [OTel component] SpanExporter is a component that gets called when a span is finished.
        List<SpanExporter> spanExporters = TraceExporterFactory.getSpanExporters(applicationModel, tracingConfig.getTracingExporter());

        String applicationName = applicationModel.getApplicationConfigManager().getApplication()
                .map(ApplicationConfig::getName)
                .orElse(DEFAULT_APPLICATION_NAME);

        this.publisher = new OTelEventPublisher(getEventListeners());

        // [Micrometer Tracing component] A Micrometer Tracing wrapper for OTel
        this.otelCurrentTraceContext = createCurrentTraceContext();

        // [OTel component] SdkTracerProvider is an SDK implementation for TracerProvider
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .setSampler(getSampler())
                .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName)))
                .addSpanProcessor(BatchSpanProcessor
                        .builder(new CompositeSpanExporter(spanExporters, null, null, null))
                        .build())
                .build();

        ContextPropagators otelContextPropagators = createOtelContextPropagators();

        // [OTel component] The SDK implementation of OpenTelemetry
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(otelContextPropagators)
                .build();

        // [OTel component] Tracer is a component that handles the life-cycle of a span
        io.opentelemetry.api.trace.Tracer otelTracer = openTelemetrySdk.getTracerProvider()
                .get("org.apache.dubbo", Version.getVersion());

        OTelPropagatorProvider.createMicrometerPropagator(otelContextPropagators, otelTracer);

        // [Micrometer Tracing component] A Micrometer Tracing wrapper for OTel's Tracer.
        return new OtelTracer(otelTracer, otelCurrentTraceContext, publisher,
                new OtelBaggageManager(otelCurrentTraceContext,
                        tracingConfig.getBaggage().getRemoteFields(),
                        Collections.emptyList()));
    }

    /**
     * sampler with probability
     *
     * @return sampler
     */
    private Sampler getSampler() {
        Sampler rootSampler = Sampler.traceIdRatioBased(tracingConfig.getSampling().getProbability());
        return Sampler.parentBased(rootSampler);
    }

    private List<EventListener> getEventListeners() {
        List<EventListener> listeners = new ArrayList<>();

        // [Micrometer Tracing component] A Micrometer Tracing listener for setting up MDC.
        Slf4JEventListener slf4JEventListener = new Slf4JEventListener();
        listeners.add(slf4JEventListener);

        if (tracingConfig.getBaggage().getEnabled()) {
            // [Micrometer Tracing component] A Micrometer Tracing listener for setting Baggage in MDC.
            // Customizable with correlation fields.
            Slf4JBaggageEventListener slf4JBaggageEventListener = new Slf4JBaggageEventListener(tracingConfig.getBaggage().getCorrelation().getFields());
            listeners.add(slf4JBaggageEventListener);
        }

        return listeners;
    }

    private OtelCurrentTraceContext createCurrentTraceContext() {
        ContextStorage.addWrapper(new EventPublishingContextWrapper(publisher));
        return new OtelCurrentTraceContext();
    }

    private ContextPropagators createOtelContextPropagators() {
        return ContextPropagators.create(
                TextMapPropagator.composite(
                        PropagatorFactory.getPropagator(tracingConfig.getPropagation(),
                                tracingConfig.getBaggage(),
                                otelCurrentTraceContext
                        )));
    }

    static class OTelEventPublisher implements OtelTracer.EventPublisher {

        private final List<EventListener> listeners;

        OTelEventPublisher(List<EventListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void publishEvent(Object event) {
            for (EventListener listener : this.listeners) {
                listener.onEvent(event);
            }
        }
    }

    static class PropagatorFactory {

        public static TextMapPropagator getPropagator(PropagationConfig propagationConfig,
                                                      @Nullable BaggageConfig baggageConfig,
                                                      @Nullable OtelCurrentTraceContext currentTraceContext) {
            if (baggageConfig == null || !baggageConfig.getEnabled()) {
                return getPropagatorWithoutBaggage(propagationConfig);
            }
            return getPropagatorWithBaggage(propagationConfig, baggageConfig, currentTraceContext);
        }

        private static TextMapPropagator getPropagatorWithoutBaggage(PropagationConfig propagationConfig) {
            String type = propagationConfig.getType();
            if ("B3".equals(type)) {
                return B3Propagator.injectingSingleHeader();
            } else if ("W3C".equals(type)) {
                return W3CTraceContextPropagator.getInstance();
            }
            return TextMapPropagator.noop();
        }

        private static TextMapPropagator getPropagatorWithBaggage(PropagationConfig propagationConfig,
                                                                  BaggageConfig baggageConfig,
                                                                  OtelCurrentTraceContext currentTraceContext) {
            String type = propagationConfig.getType();
            if ("B3".equals(type)) {
                List<String> remoteFields = baggageConfig.getRemoteFields();
                return TextMapPropagator.composite(B3Propagator.injectingSingleHeader(),
                        new BaggageTextMapPropagator(remoteFields,
                                new OtelBaggageManager(currentTraceContext, remoteFields, Collections.emptyList())));
            } else if ("W3C".equals(type)) {
                List<String> remoteFields = baggageConfig.getRemoteFields();
                return TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(),
                        W3CBaggagePropagator.getInstance(), new BaggageTextMapPropagator(remoteFields,
                                new OtelBaggageManager(currentTraceContext, remoteFields, Collections.emptyList())));
            }
            return TextMapPropagator.noop();
        }
    }
}
