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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.nested.BaggageConfig;
import org.apache.dubbo.config.nested.ExporterConfig;
import org.apache.dubbo.config.nested.PropagationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.tracing.exporter.otlp.OTlpSpanExporter;
import org.apache.dubbo.tracing.exporter.zipkin.ZipkinSpanExporter;
import org.apache.dubbo.tracing.tracer.TracerProvider;
import org.apache.dubbo.tracing.utils.PropagationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import io.opentelemetry.api.common.AttributeKey;

import static org.apache.dubbo.tracing.utils.ObservationConstants.DEFAULT_APPLICATION_NAME;

public class OpenTelemetryProvider implements TracerProvider {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(OpenTelemetryProvider.class);

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
        List<io.opentelemetry.sdk.trace.export.SpanExporter> spanExporters = getSpanExporters();

        String applicationName = applicationModel
                .getApplicationConfigManager()
                .getApplication()
                .map(ApplicationConfig::getName)
                .orElse(DEFAULT_APPLICATION_NAME);

        this.publisher = new OTelEventPublisher(getEventListeners());

        // [Micrometer Tracing component] A Micrometer Tracing wrapper for OTel
        this.otelCurrentTraceContext = createCurrentTraceContext();

        // Due to https://github.com/micrometer-metrics/tracing/issues/343
        String RESOURCE_ATTRIBUTES_CLASS_NAME = "io.opentelemetry.semconv.ResourceAttributes";
        boolean isLowVersion = !ClassUtils.isPresent(
                RESOURCE_ATTRIBUTES_CLASS_NAME, Thread.currentThread().getContextClassLoader());
        AttributeKey<String> serviceNameAttributeKey = AttributeKey.stringKey("service.name");
        String SERVICE_NAME = "SERVICE_NAME";

        if (isLowVersion) {
            RESOURCE_ATTRIBUTES_CLASS_NAME = "io.opentelemetry.semconv.resource.attributes.ResourceAttributes";
        }
        try {
            serviceNameAttributeKey = (AttributeKey<String>) ClassUtils.resolveClass(
                            RESOURCE_ATTRIBUTES_CLASS_NAME,
                            Thread.currentThread().getContextClassLoader())
                    .getDeclaredField(SERVICE_NAME)
                    .get(null);
        } catch (Throwable ignored) {
        }
        // [OTel component] SdkTracerProvider is an SDK implementation for TracerProvider
        io.opentelemetry.sdk.trace.SdkTracerProvider sdkTracerProvider =
                io.opentelemetry.sdk.trace.SdkTracerProvider.builder()
                        .setSampler(getSampler())
                        .setResource(io.opentelemetry.sdk.resources.Resource.create(
                                io.opentelemetry.api.common.Attributes.of(serviceNameAttributeKey, applicationName)))
                        .addSpanProcessor(io.opentelemetry.sdk.trace.export.BatchSpanProcessor.builder(
                                        new CompositeSpanExporter(spanExporters, null, null, null))
                                .build())
                        .build();

        io.opentelemetry.context.propagation.ContextPropagators otelContextPropagators = createOtelContextPropagators();

        // [OTel component] The SDK implementation of OpenTelemetry
        io.opentelemetry.sdk.OpenTelemetrySdk openTelemetrySdk = io.opentelemetry.sdk.OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(otelContextPropagators)
                .build();

        // [OTel component] Tracer is a component that handles the life-cycle of a span
        io.opentelemetry.api.trace.Tracer otelTracer =
                openTelemetrySdk.getTracerProvider().get("org.apache.dubbo", Version.getVersion());

        OTelPropagatorProvider.createMicrometerPropagator(otelContextPropagators, otelTracer);

        // [Micrometer Tracing component] A Micrometer Tracing wrapper for OTel's Tracer.
        return new OtelTracer(
                otelTracer,
                otelCurrentTraceContext,
                publisher,
                new OtelBaggageManager(
                        otelCurrentTraceContext,
                        tracingConfig.getBaggage().getRemoteFields(),
                        Collections.emptyList()));
    }

    private List<io.opentelemetry.sdk.trace.export.SpanExporter> getSpanExporters() {
        ExporterConfig exporterConfig = tracingConfig.getTracingExporter();
        ExporterConfig.ZipkinConfig zipkinConfig = exporterConfig.getZipkinConfig();
        ExporterConfig.OtlpConfig otlpConfig = exporterConfig.getOtlpConfig();
        List<io.opentelemetry.sdk.trace.export.SpanExporter> res = new ArrayList<>();
        if (zipkinConfig != null && StringUtils.isNotEmpty(zipkinConfig.getEndpoint())) {
            LOGGER.info("Create zipkin span exporter.");
            res.add(ZipkinSpanExporter.getSpanExporter(applicationModel, zipkinConfig));
        }
        if (otlpConfig != null && StringUtils.isNotEmpty(otlpConfig.getEndpoint())) {
            LOGGER.info("Create OTlp span exporter.");
            res.add(OTlpSpanExporter.getSpanExporter(applicationModel, otlpConfig));
        }

        return res;
    }

    /**
     * sampler with probability
     *
     * @return sampler
     */
    private io.opentelemetry.sdk.trace.samplers.Sampler getSampler() {
        io.opentelemetry.sdk.trace.samplers.Sampler rootSampler =
                io.opentelemetry.sdk.trace.samplers.Sampler.traceIdRatioBased(
                        tracingConfig.getSampling().getProbability());
        return io.opentelemetry.sdk.trace.samplers.Sampler.parentBased(rootSampler);
    }

    private List<EventListener> getEventListeners() {
        List<EventListener> listeners = new ArrayList<>();

        // [Micrometer Tracing component] A Micrometer Tracing listener for setting up MDC.
        Slf4JEventListener slf4JEventListener = new Slf4JEventListener();
        listeners.add(slf4JEventListener);

        if (tracingConfig.getBaggage().getEnabled()) {
            // [Micrometer Tracing component] A Micrometer Tracing listener for setting Baggage in MDC.
            // Customizable with correlation fields.
            Slf4JBaggageEventListener slf4JBaggageEventListener = new Slf4JBaggageEventListener(
                    tracingConfig.getBaggage().getCorrelation().getFields());
            listeners.add(slf4JBaggageEventListener);
        }

        return listeners;
    }

    private OtelCurrentTraceContext createCurrentTraceContext() {
        io.opentelemetry.context.ContextStorage.addWrapper(new EventPublishingContextWrapper(publisher));
        return new OtelCurrentTraceContext();
    }

    private io.opentelemetry.context.propagation.ContextPropagators createOtelContextPropagators() {
        return io.opentelemetry.context.propagation.ContextPropagators.create(
                io.opentelemetry.context.propagation.TextMapPropagator.composite(PropagatorFactory.getPropagator(
                        tracingConfig.getPropagation(), tracingConfig.getBaggage(), otelCurrentTraceContext)));
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

        public static io.opentelemetry.context.propagation.TextMapPropagator getPropagator(
                PropagationConfig propagationConfig,
                @Nullable BaggageConfig baggageConfig,
                @Nullable OtelCurrentTraceContext currentTraceContext) {
            if (baggageConfig == null || !baggageConfig.getEnabled()) {
                return getPropagatorWithoutBaggage(propagationConfig);
            }
            return getPropagatorWithBaggage(propagationConfig, baggageConfig, currentTraceContext);
        }

        private static io.opentelemetry.context.propagation.TextMapPropagator getPropagatorWithoutBaggage(
                PropagationConfig propagationConfig) {
            String type = propagationConfig.getType();
            PropagationType propagationType = PropagationType.forValue(type);

            if (PropagationType.B3 == propagationType) {
                return io.opentelemetry.extension.trace.propagation.B3Propagator.injectingSingleHeader();
            } else if (PropagationType.W3C == propagationType) {
                return io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance();
            }
            return io.opentelemetry.context.propagation.TextMapPropagator.noop();
        }

        private static io.opentelemetry.context.propagation.TextMapPropagator getPropagatorWithBaggage(
                PropagationConfig propagationConfig,
                BaggageConfig baggageConfig,
                OtelCurrentTraceContext currentTraceContext) {
            String type = propagationConfig.getType();
            PropagationType propagationType = PropagationType.forValue(type);
            if (PropagationType.B3 == propagationType) {
                List<String> remoteFields = baggageConfig.getRemoteFields();
                return io.opentelemetry.context.propagation.TextMapPropagator.composite(
                        io.opentelemetry.extension.trace.propagation.B3Propagator.injectingSingleHeader(),
                        new BaggageTextMapPropagator(
                                remoteFields,
                                new OtelBaggageManager(currentTraceContext, remoteFields, Collections.emptyList())));
            } else if (PropagationType.W3C == propagationType) {
                List<String> remoteFields = baggageConfig.getRemoteFields();
                return io.opentelemetry.context.propagation.TextMapPropagator.composite(
                        io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance(),
                        io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator.getInstance(),
                        new BaggageTextMapPropagator(
                                remoteFields,
                                new OtelBaggageManager(currentTraceContext, remoteFields, Collections.emptyList())));
            }
            return io.opentelemetry.context.propagation.TextMapPropagator.noop();
        }
    }
}
