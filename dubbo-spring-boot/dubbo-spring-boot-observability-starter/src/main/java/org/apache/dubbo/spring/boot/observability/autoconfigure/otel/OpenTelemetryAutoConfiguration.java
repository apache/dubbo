package org.apache.dubbo.spring.boot.observability.autoconfigure.otel;

import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.exporter.SpanFilter;
import io.micrometer.tracing.exporter.SpanReporter;
import io.micrometer.tracing.otel.bridge.CompositeSpanExporter;
import io.micrometer.tracing.otel.bridge.EventListener;
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelSpanCustomizer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.dubbo.spring.boot.observability.annotation.ConditionalOnDubboTracingEnable;
import org.apache.dubbo.spring.boot.observability.autoconfigure.DubboMicrometerTracingAutoConfiguration;
import org.apache.dubbo.spring.boot.observability.config.DubboTracingProperties;
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

@AutoConfiguration(before = DubboMicrometerTracingAutoConfiguration.class, afterName = "org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration")
@ConditionalOnDubboTracingEnable
@ConditionalOnClass({OtelTracer.class, SdkTracerProvider.class, OpenTelemetry.class})
@EnableConfigurationProperties(DubboTracingProperties.class)
public class OpenTelemetryAutoConfiguration {

    /**
     * Default value for application name if {@code spring.application.name} is not set.
     */
    private static final String DEFAULT_APPLICATION_NAME = "application";

    private final DubboTracingProperties tracingProperties;

    OpenTelemetryAutoConfiguration(DubboTracingProperties tracingProperties) {
        this.tracingProperties = tracingProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider, ContextPropagators contextPropagators) {
        return OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).setPropagators(contextPropagators)
            .build();
    }

    @Bean
    @ConditionalOnMissingBean
    SdkTracerProvider otelSdkTracerProvider(Environment environment, ObjectProvider<SpanProcessor> spanProcessors,
                                            Sampler sampler) {
        String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder().setSampler(sampler)
            .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName)));
        spanProcessors.orderedStream().forEach(builder::addSpanProcessor);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    ContextPropagators otelContextPropagators(ObjectProvider<TextMapPropagator> textMapPropagators) {
        return ContextPropagators.create(TextMapPropagator.composite(textMapPropagators.orderedStream().collect(Collectors.toList())));
    }

    @Bean
    @ConditionalOnMissingBean
    Sampler otelSampler() {
        Sampler rootSampler = Sampler.traceIdRatioBased(this.tracingProperties.getRatio());
        return Sampler.parentBased(rootSampler);
    }

    @Bean
    @ConditionalOnMissingBean
    SpanProcessor otelSpanProcessor(ObjectProvider<SpanExporter> spanExporters,
                                    ObjectProvider<SpanExportingPredicate> spanExportingPredicates, ObjectProvider<SpanReporter> spanReporters,
                                    ObjectProvider<SpanFilter> spanFilters) {
        return BatchSpanProcessor.builder(new CompositeSpanExporter(spanExporters.orderedStream().collect(Collectors.toList()),
            spanExportingPredicates.orderedStream().collect(Collectors.toList()), spanReporters.orderedStream().collect(Collectors.toList()),
            spanFilters.orderedStream().collect(Collectors.toList()))).build();
    }

    @Bean
    @ConditionalOnMissingBean
    Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("org.apache.dubbo");
    }

    @Bean
    @ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
    OtelTracer micrometerOtelTracer(Tracer tracer, OtelTracer.EventPublisher eventPublisher,
                                    OtelCurrentTraceContext otelCurrentTraceContext) {
        return new OtelTracer(tracer, otelCurrentTraceContext, eventPublisher,
            new OtelBaggageManager(otelCurrentTraceContext, this.tracingProperties.getRemoteFields(),
                Collections.emptyList()));
    }

    @Bean
    @ConditionalOnMissingBean
    OtelPropagator otelPropagator(ContextPropagators contextPropagators, Tracer tracer) {
        return new OtelPropagator(contextPropagators, tracer);
    }

    @Bean
    @ConditionalOnMissingBean
    OtelTracer.EventPublisher otelTracerEventPublisher(List<EventListener> eventListeners) {
        return new OTelEventPublisher(eventListeners);
    }

    @Bean
    @ConditionalOnMissingBean
    OtelCurrentTraceContext otelCurrentTraceContext(OtelTracer.EventPublisher publisher) {
        ContextStorage.addWrapper(new EventPublishingContextWrapper(publisher));
        return new OtelCurrentTraceContext();
    }

    @Bean
    @ConditionalOnMissingBean
    Slf4JEventListener otelSlf4JEventListener() {
        return new Slf4JEventListener();
    }

    @Bean
    @ConditionalOnMissingBean(SpanCustomizer.class)
    OtelSpanCustomizer otelSpanCustomizer() {
        return new OtelSpanCustomizer();
    }

    @Configuration(proxyBeanMethods = false)
    static class BaggageConfiguration {

        private final DubboTracingProperties tracingProperties;

        BaggageConfiguration(DubboTracingProperties tracingProperties) {
            this.tracingProperties = tracingProperties;
        }

        @Bean
        @ConditionalOnMissingBean
        //todo test;
        @ConditionalOnProperty(prefix = "dubbo.tracing", name = "baggage", havingValue = "W3C",
            matchIfMissing = true)
        TextMapPropagator w3cTextMapPropagatorWithBaggage(OtelCurrentTraceContext otelCurrentTraceContext) {
            List<String> remoteFields = this.tracingProperties.getRemoteFields();
            return TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(),
                W3CBaggagePropagator.getInstance(), new BaggageTextMapPropagator(remoteFields,
                    new OtelBaggageManager(otelCurrentTraceContext, remoteFields, Collections.emptyList())));
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "dubbo.tracing", name = "baggage", havingValue = "B3")
        TextMapPropagator b3BaggageTextMapPropagator(OtelCurrentTraceContext otelCurrentTraceContext) {
            List<String> remoteFields = this.tracingProperties.getRemoteFields();
            return TextMapPropagator.composite(B3Propagator.injectingSingleHeader(),
                new BaggageTextMapPropagator(remoteFields,
                    new OtelBaggageManager(otelCurrentTraceContext, remoteFields, Collections.emptyList())));
        }

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

}

