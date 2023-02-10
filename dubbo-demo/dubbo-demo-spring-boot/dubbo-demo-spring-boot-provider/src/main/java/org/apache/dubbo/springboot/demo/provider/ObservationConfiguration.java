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

package org.apache.dubbo.springboot.demo.provider;


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
import io.micrometer.tracing.otel.bridge.ArrayListSpanProcessor;
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;

import static io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOn;

@Configuration
public class ObservationConfiguration {

    /**
     * Default value for application name if {@code spring.application.name} is not set.
     */
    private static final String DEFAULT_APPLICATION_NAME = "application";
    @javax.annotation.Resource
    private ApplicationModel applicationModel;

    @Bean
    ObservationRegistry observationRegistry() {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        applicationModel.getBeanFactory().registerBean(observationRegistry);
        return observationRegistry;
    }

    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    ArrayListSpanProcessor spanExporter() {
        return new ArrayListSpanProcessor();
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
        OtelTracer.EventPublisher eventPublisher = event -> {
            slf4JEventListener.onEvent(event);
            slf4JBaggageEventListener.onEvent(event);
        };
        ContextStorage.addWrapper(new EventPublishingContextWrapper(eventPublisher));
        return new OtelTracer(otelTracer, otelCurrentTraceContext, eventPublisher, new OtelBaggageManager(otelCurrentTraceContext, Collections.emptyList(), Collections.emptyList()));
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

    @Bean
    TracesDumper tracesDumper(ArrayListSpanProcessor arrayListSpanProcessor) {
        return new TracesDumper(arrayListSpanProcessor);
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


    static class TracesDumper {
        private final ArrayListSpanProcessor arrayListSpanProcessor;

        TracesDumper(ArrayListSpanProcessor arrayListSpanProcessor) {
            this.arrayListSpanProcessor = arrayListSpanProcessor;
        }

        @PreDestroy
        void dumpTraces() {
            System.out.println("==== TRACES ====");
            this.arrayListSpanProcessor.spans().forEach(System.out::println);
            System.out.println("=================");
        }
    }
}
