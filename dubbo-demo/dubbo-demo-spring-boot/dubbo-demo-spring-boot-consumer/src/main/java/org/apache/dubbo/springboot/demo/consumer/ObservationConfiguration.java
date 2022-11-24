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

package org.apache.dubbo.springboot.demo.consumer;


import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporterBuilder;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.dubbo.rpc.model.ApplicationModel;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOn;

@Configuration
public class ObservationConfiguration {

    /**
     * Default value for application name if {@code spring.application.name} is not set.
     */
    private static final String DEFAULT_APPLICATION_NAME = "application";

    @Bean
    ApplicationModel applicationModel() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        applicationModel.getBeanFactory().registerBean(observationRegistry());
        return applicationModel;
    }

    @Bean
    ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }

    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    SpanExporter spanExporter() {
        return new ZipkinSpanExporterBuilder()
            .setSender(URLConnectionSender.create("http://localhost:9411/api/v2/spans")).build();
    }

    @Bean
    SdkTracerProvider sdkTracerProvider(Environment environment) {
        String applicationName = environment.getProperty("dubbo.application.name", DEFAULT_APPLICATION_NAME);
        return SdkTracerProvider.builder().setSampler(alwaysOn())
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter()).build())
            .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName)))
            .build();
    }

    @Bean
    ContextPropagators contextPropagators() {
        return ContextPropagators.create(B3Propagator.injectingSingleHeader());
    }

    @Bean
    OpenTelemetrySdk openTelemetrySdk(SdkTracerProvider sdkTracerProvider) {
        return OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
            .setPropagators(contextPropagators()).build();
    }

    @Bean
    io.opentelemetry.api.trace.Tracer otelTracer(OpenTelemetrySdk openTelemetrySdk) {
        return openTelemetrySdk.getTracerProvider()
            .get("io.micrometer.micrometer-tracing");
    }

    @Bean
    OtelCurrentTraceContext otelCurrentTraceContext() {
        return new OtelCurrentTraceContext();
    }

    Slf4JEventListener slf4JEventListener() {
        return new Slf4JEventListener();
    }

    Slf4JBaggageEventListener slf4JBaggageEventListener() {
        return new Slf4JBaggageEventListener(Collections.emptyList());
    }

    @Bean
    OtelTracer tracer(io.opentelemetry.api.trace.Tracer otelTracer, OtelCurrentTraceContext otelCurrentTraceContext) {
        Slf4JEventListener slf4JEventListener = slf4JEventListener();
        Slf4JBaggageEventListener slf4JBaggageEventListener = slf4JBaggageEventListener();
        return new OtelTracer(otelTracer, otelCurrentTraceContext, event -> {
            slf4JEventListener.onEvent(event);
            slf4JBaggageEventListener.onEvent(event);
        }, new OtelBaggageManager(otelCurrentTraceContext, Collections.emptyList(), Collections.emptyList()));
    }

    @Bean
    Propagator propagator(io.opentelemetry.api.trace.Tracer otelTracer) {
        return new OtelPropagator(contextPropagators(), otelTracer);
    }

    @Bean
    ObservationHandlerRegistrar observationHandlerRegistrar(ObservationRegistry observationRegistry, OtelTracer tracer, Propagator propagator, MeterRegistry meterRegistry) {
        return new ObservationHandlerRegistrar(observationRegistry, tracer, propagator, meterRegistry);
    }

    @Bean
    MetricsDumper metricsDumper(MeterRegistry meterRegistry) {
        return new MetricsDumper(meterRegistry);
    }

    static class ObservationHandlerRegistrar {

        private final ObservationRegistry observationRegistry;

        private final Tracer tracer;

        private final Propagator propagator;

        private final MeterRegistry meterRegistry;

        ObservationHandlerRegistrar(ObservationRegistry observationRegistry, Tracer tracer, Propagator propagator, MeterRegistry meterRegistry) {
            this.observationRegistry = observationRegistry;
            this.tracer = tracer;
            this.propagator = propagator;
            this.meterRegistry = meterRegistry;
        }

        @PostConstruct
        void setup() {
            observationRegistry.observationConfig().observationHandler(new TracingAwareMeterObservationHandler<>(new DefaultMeterObservationHandler(meterRegistry), tracer));
            observationRegistry.observationConfig()
                .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(new PropagatingReceiverTracingObservationHandler<>(tracer, propagator), new PropagatingSenderTracingObservationHandler<>(tracer, propagator), new DefaultTracingObservationHandler(tracer)));
        }
    }


    static class MetricsDumper {
        private final MeterRegistry meterRegistry;

        MetricsDumper(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        @PreDestroy
        void dumpMetrics() {
            System.out.println("==== METRICS ====");
            this.meterRegistry.getMeters().forEach(meter -> System.out.println(" - Metric type \t[" + meter.getId().getType() + "],\tname [" + meter.getId().getName() + "],\ttags " + meter.getId().getTags() + ",\tmeasurements " + meter.measure()));
            System.out.println("=================");
        }
    }
}
