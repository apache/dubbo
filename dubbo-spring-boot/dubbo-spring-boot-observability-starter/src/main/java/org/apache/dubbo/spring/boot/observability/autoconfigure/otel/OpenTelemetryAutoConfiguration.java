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
package org.apache.dubbo.spring.boot.observability.autoconfigure.otel;


import org.apache.dubbo.common.Version;
import org.apache.dubbo.spring.boot.autoconfigure.DubboConfigurationProperties;
import org.apache.dubbo.spring.boot.observability.annotation.ConditionalOnDubboTracingEnable;
import org.apache.dubbo.spring.boot.observability.autoconfigure.DubboMicrometerTracingAutoConfiguration;
import org.apache.dubbo.spring.boot.observability.autoconfigure.ObservabilityUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * provider OpenTelemetry when you are using Boot <3.0 or you are not using spring-boot-starter-actuator
 */
@AutoConfiguration(before = DubboMicrometerTracingAutoConfiguration.class, afterName = "org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration")
@ConditionalOnDubboTracingEnable
@ConditionalOnClass(name = {"io.micrometer.tracing.otel.bridge.OtelTracer",
    "io.opentelemetry.sdk.trace.SdkTracerProvider", "io.opentelemetry.api.OpenTelemetry"
    , "io.micrometer.tracing.SpanCustomizer"})
@EnableConfigurationProperties(DubboConfigurationProperties.class)
public class OpenTelemetryAutoConfiguration {

    /**
     * Default value for application name if {@code spring.application.name} is not set.
     */
    private static final String DEFAULT_APPLICATION_NAME = "application";

    private final DubboConfigurationProperties dubboConfigProperties;

    OpenTelemetryAutoConfiguration(DubboConfigurationProperties dubboConfigProperties) {
        this.dubboConfigProperties = dubboConfigProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    io.opentelemetry.api.OpenTelemetry openTelemetry(io.opentelemetry.sdk.trace.SdkTracerProvider sdkTracerProvider, io.opentelemetry.context.propagation.ContextPropagators contextPropagators) {
        return io.opentelemetry.sdk.OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).setPropagators(contextPropagators)
            .build();
    }

    @Bean
    @ConditionalOnMissingBean
    io.opentelemetry.sdk.trace.SdkTracerProvider otelSdkTracerProvider(Environment environment, ObjectProvider<io.opentelemetry.sdk.trace.SpanProcessor> spanProcessors,
                                                                       io.opentelemetry.sdk.trace.samplers.Sampler sampler) {
        String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
        io.opentelemetry.sdk.trace.SdkTracerProviderBuilder builder = io.opentelemetry.sdk.trace.SdkTracerProvider.builder().setSampler(sampler)
            .setResource(io.opentelemetry.sdk.resources.Resource.create(io.opentelemetry.api.common.Attributes.of(io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME, applicationName)));
        spanProcessors.orderedStream().forEach(builder::addSpanProcessor);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    io.opentelemetry.context.propagation.ContextPropagators otelContextPropagators(ObjectProvider<io.opentelemetry.context.propagation.TextMapPropagator> textMapPropagators) {
        return io.opentelemetry.context.propagation.ContextPropagators.create(io.opentelemetry.context.propagation.TextMapPropagator.composite(textMapPropagators.orderedStream().collect(Collectors.toList())));
    }

    @Bean
    @ConditionalOnMissingBean
    io.opentelemetry.sdk.trace.samplers.Sampler otelSampler() {
        io.opentelemetry.sdk.trace.samplers.Sampler rootSampler = io.opentelemetry.sdk.trace.samplers.Sampler.traceIdRatioBased(this.dubboConfigProperties.getTracing().getSampling().getProbability());
        return io.opentelemetry.sdk.trace.samplers.Sampler.parentBased(rootSampler);
    }

    @Bean
    @ConditionalOnMissingBean
    io.opentelemetry.sdk.trace.SpanProcessor otelSpanProcessor(ObjectProvider<io.opentelemetry.sdk.trace.export.SpanExporter> spanExporters,
                                    ObjectProvider<io.micrometer.tracing.exporter.SpanExportingPredicate> spanExportingPredicates, ObjectProvider<io.micrometer.tracing.exporter.SpanReporter> spanReporters,
                                    ObjectProvider<io.micrometer.tracing.exporter.SpanFilter> spanFilters) {
        return io.opentelemetry.sdk.trace.export.BatchSpanProcessor.builder(new io.micrometer.tracing.otel.bridge.CompositeSpanExporter(spanExporters.orderedStream().collect(Collectors.toList()),
            spanExportingPredicates.orderedStream().collect(Collectors.toList()), spanReporters.orderedStream().collect(Collectors.toList()),
            spanFilters.orderedStream().collect(Collectors.toList()))).build();
    }

    @Bean
    @ConditionalOnMissingBean
    io.opentelemetry.api.trace.Tracer otelTracer(io.opentelemetry.api.OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("org.apache.dubbo", Version.getVersion());
    }

    @Bean
    @ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
    io.micrometer.tracing.otel.bridge.OtelTracer micrometerOtelTracer(io.opentelemetry.api.trace.Tracer tracer, io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher eventPublisher,
                                                                      io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext otelCurrentTraceContext) {
        return new io.micrometer.tracing.otel.bridge.OtelTracer(tracer, otelCurrentTraceContext, eventPublisher,
                new io.micrometer.tracing.otel.bridge.OtelBaggageManager(otelCurrentTraceContext, this.dubboConfigProperties.getTracing().getBaggage().getRemoteFields(),
                        Collections.emptyList()));
    }

    @Bean
    @ConditionalOnMissingBean
    io.micrometer.tracing.otel.bridge.OtelPropagator otelPropagator(io.opentelemetry.context.propagation.ContextPropagators contextPropagators, io.opentelemetry.api.trace.Tracer tracer) {
        return new io.micrometer.tracing.otel.bridge.OtelPropagator(contextPropagators, tracer);
    }

    @Bean
    @ConditionalOnMissingBean
    io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher otelTracerEventPublisher(List<io.micrometer.tracing.otel.bridge.EventListener> eventListeners) {
        return new OTelEventPublisher(eventListeners);
    }

    @Bean
    @ConditionalOnMissingBean
    io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext otelCurrentTraceContext(io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher publisher) {
        io.opentelemetry.context.ContextStorage.addWrapper(new io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper(publisher));
        return new io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext();
    }

    @Bean
    @ConditionalOnMissingBean
    io.micrometer.tracing.otel.bridge.Slf4JEventListener otelSlf4JEventListener() {
        return new io.micrometer.tracing.otel.bridge.Slf4JEventListener();
    }

    @Bean
    @ConditionalOnMissingBean(io.micrometer.tracing.SpanCustomizer.class)
    io.micrometer.tracing.otel.bridge.OtelSpanCustomizer otelSpanCustomizer() {
        return new io.micrometer.tracing.otel.bridge.OtelSpanCustomizer();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = ObservabilityUtils.DUBBO_TRACING_BAGGAGE, name = "enabled", matchIfMissing = true)
    static class BaggageConfiguration {

        private final DubboConfigurationProperties dubboConfigProperties;

        BaggageConfiguration(DubboConfigurationProperties dubboConfigProperties) {
            this.dubboConfigProperties = dubboConfigProperties;
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = ObservabilityUtils.DUBBO_TRACING_PROPAGATION, name = "type", havingValue = "W3C",
                matchIfMissing = true)
        io.opentelemetry.context.propagation.TextMapPropagator w3cTextMapPropagatorWithBaggage(io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext otelCurrentTraceContext) {
            List<String> remoteFields = this.dubboConfigProperties.getTracing().getBaggage().getRemoteFields();
            return io.opentelemetry.context.propagation.TextMapPropagator.composite(io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance(),
                io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator.getInstance(), new io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator(remoteFields,
                            new io.micrometer.tracing.otel.bridge.OtelBaggageManager(otelCurrentTraceContext, remoteFields, Collections.emptyList())));
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = ObservabilityUtils.DUBBO_TRACING_PROPAGATION, name = "type", havingValue = "B3")
        io.opentelemetry.context.propagation.TextMapPropagator b3BaggageTextMapPropagator(io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext otelCurrentTraceContext) {
            List<String> remoteFields = this.dubboConfigProperties.getTracing().getBaggage().getRemoteFields();
            return io.opentelemetry.context.propagation.TextMapPropagator.composite(io.opentelemetry.extension.trace.propagation.B3Propagator.injectingSingleHeader(),
                    new io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator(remoteFields,
                            new io.micrometer.tracing.otel.bridge.OtelBaggageManager(otelCurrentTraceContext, remoteFields, Collections.emptyList())));
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = ObservabilityUtils.DUBBO_TRACING_BAGGAGE_CORRELATION, name = "enabled",
                matchIfMissing = true)
        io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener otelSlf4JBaggageEventListener() {
            return new io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener(this.dubboConfigProperties.getTracing().getBaggage().getCorrelation().getFields());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = ObservabilityUtils.DUBBO_TRACING_BAGGAGE, name = "enabled", havingValue = "false")
    static class NoBaggageConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = ObservabilityUtils.DUBBO_TRACING_PROPAGATION, name = "type", havingValue = "B3")
        io.opentelemetry.extension.trace.propagation.B3Propagator b3TextMapPropagator() {
            return io.opentelemetry.extension.trace.propagation.B3Propagator.injectingSingleHeader();
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = ObservabilityUtils.DUBBO_TRACING_PROPAGATION, name = "type", havingValue = "W3C",
                matchIfMissing = true)
        io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator w3cTextMapPropagatorWithoutBaggage() {
            return io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance();
        }

    }

    static class OTelEventPublisher implements io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher {

        private final List<io.micrometer.tracing.otel.bridge.EventListener> listeners;

        OTelEventPublisher(List<io.micrometer.tracing.otel.bridge.EventListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void publishEvent(Object event) {
            for (io.micrometer.tracing.otel.bridge.EventListener listener : this.listeners) {
                listener.onEvent(event);
            }
        }

    }

}

