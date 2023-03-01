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
package org.apache.dubbo.spring.boot.observability.autoconfigure.brave;

import brave.CurrentSpanCustomizer;
import brave.SpanCustomizer;
import brave.Tracing;
import brave.TracingCustomizer;
import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationConfig;
import brave.baggage.BaggagePropagationCustomizer;
import brave.baggage.CorrelationScopeConfig;
import brave.baggage.CorrelationScopeCustomizer;
import brave.baggage.CorrelationScopeDecorator;
import brave.context.slf4j.MDCScopeDecorator;
import brave.handler.SpanHandler;
import brave.propagation.B3Propagation;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContextCustomizer;
import brave.propagation.Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveSpanCustomizer;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.CompositeSpanHandler;
import io.micrometer.tracing.brave.bridge.W3CPropagation;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.exporter.SpanFilter;
import io.micrometer.tracing.exporter.SpanReporter;
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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * provider Brave when you are using Boot <3.0 or you are not using spring-boot-starter-actuator
 */
@AutoConfiguration(before = DubboMicrometerTracingAutoConfiguration.class, afterName = "org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration")
@ConditionalOnClass({Tracer.class, BraveTracer.class})
@EnableConfigurationProperties(DubboTracingProperties.class)
@ConditionalOnDubboTracingEnable
public class BraveAutoConfiguration {

    private static final BraveBaggageManager BRAVE_BAGGAGE_MANAGER = new BraveBaggageManager();

    /**
     * Default value for application name if {@code spring.application.name} is not set.
     */
    private static final String DEFAULT_APPLICATION_NAME = "application";

    @Bean
    @ConditionalOnMissingBean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    CompositeSpanHandler compositeSpanHandler(ObjectProvider<SpanExportingPredicate> predicates,
                                              ObjectProvider<SpanReporter> reporters, ObjectProvider<SpanFilter> filters) {
        return new CompositeSpanHandler(predicates.orderedStream().collect(Collectors.toList()),
            reporters.orderedStream().collect(Collectors.toList()),
            filters.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    public Tracing braveTracing(Environment environment, List<SpanHandler> spanHandlers,
                                List<TracingCustomizer> tracingCustomizers, CurrentTraceContext currentTraceContext,
                                Propagation.Factory propagationFactory, Sampler sampler) {
        String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
        Tracing.Builder builder = Tracing.newBuilder().currentTraceContext(currentTraceContext).traceId128Bit(true)
            .supportsJoin(false).propagationFactory(propagationFactory).sampler(sampler)
            .localServiceName(applicationName);
        spanHandlers.forEach(builder::addSpanHandler);
        for (TracingCustomizer tracingCustomizer : tracingCustomizers) {
            tracingCustomizer.customize(builder);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public brave.Tracer braveTracer(Tracing tracing) {
        return tracing.tracer();
    }

    @Bean
    @ConditionalOnMissingBean
    public CurrentTraceContext braveCurrentTraceContext(List<CurrentTraceContext.ScopeDecorator> scopeDecorators,
                                                        List<CurrentTraceContextCustomizer> currentTraceContextCustomizers) {
        ThreadLocalCurrentTraceContext.Builder builder = ThreadLocalCurrentTraceContext.newBuilder();
        scopeDecorators.forEach(builder::addScopeDecorator);
        for (CurrentTraceContextCustomizer currentTraceContextCustomizer : currentTraceContextCustomizers) {
            currentTraceContextCustomizer.customize(builder);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public Sampler braveSampler(DubboTracingProperties properties) {
        return Sampler.create(properties.getSampling().getProbability());
    }

    @Bean
    @ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
    BraveTracer braveTracerBridge(brave.Tracer tracer, CurrentTraceContext currentTraceContext) {
        return new BraveTracer(tracer, new BraveCurrentTraceContext(currentTraceContext), BRAVE_BAGGAGE_MANAGER);
    }

    @Bean
    @ConditionalOnMissingBean
    BravePropagator bravePropagator(Tracing tracing) {
        return new BravePropagator(tracing);
    }

    @Bean
    @ConditionalOnMissingBean(SpanCustomizer.class)
    CurrentSpanCustomizer currentSpanCustomizer(Tracing tracing) {
        return CurrentSpanCustomizer.create(tracing);
    }

    @Bean
    @ConditionalOnMissingBean(io.micrometer.tracing.SpanCustomizer.class)
    BraveSpanCustomizer braveSpanCustomizer(SpanCustomizer spanCustomizer) {
        return new BraveSpanCustomizer(spanCustomizer);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(value = "dubbo.tracing.baggage.enabled", havingValue = "false")
    static class BraveNoBaggageConfiguration {

        @Bean
        @ConditionalOnMissingBean
        Propagation.Factory propagationFactory(DubboTracingProperties tracing) {
            DubboTracingProperties.Propagation.PropagationType type = tracing.getPropagation().getType();
            switch (type) {
                case B3:
                    return B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.SINGLE_NO_PARENT).build();
                case W3C:
                    return new W3CPropagation();
                default:
                    throw new IllegalArgumentException("UnSupport propagation type");
            }
        }

    }

    @ConditionalOnProperty(value = "dubbo.tracing.baggage.enabled", matchIfMissing = true)
    @Configuration(proxyBeanMethods = false)
    static class BraveBaggageConfiguration {
        private final DubboTracingProperties dubboTracingProperties;

        public BraveBaggageConfiguration(DubboTracingProperties dubboTracingProperties) {
            this.dubboTracingProperties = dubboTracingProperties;
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "dubbo.tracing.propagation", value = "type", havingValue = "B3")
        BaggagePropagation.FactoryBuilder b3PropagationFactoryBuilder(
            ObjectProvider<BaggagePropagationCustomizer> baggagePropagationCustomizers) {
            Propagation.Factory delegate =
                B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.SINGLE_NO_PARENT).build();

            BaggagePropagation.FactoryBuilder builder = BaggagePropagation.newFactoryBuilder(delegate);
            baggagePropagationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "dubbo.tracing.propagation", value = "type", havingValue = "W3C", matchIfMissing = true)
        BaggagePropagation.FactoryBuilder w3cPropagationFactoryBuilder(
            ObjectProvider<BaggagePropagationCustomizer> baggagePropagationCustomizers) {
            Propagation.Factory delegate = new W3CPropagation(BRAVE_BAGGAGE_MANAGER, Collections.emptyList());

            BaggagePropagation.FactoryBuilder builder = BaggagePropagation.newFactoryBuilder(delegate);
            baggagePropagationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        @Bean
        @ConditionalOnMissingBean
        @Order(0)
        BaggagePropagationCustomizer remoteFieldsBaggagePropagationCustomizer() {
            return (builder) -> {
                List<String> remoteFields = dubboTracingProperties.getBaggage().getRemoteFields();
                for (String fieldName : remoteFields) {
                    builder.add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create(fieldName)));
                }
            };
        }

        @Bean
        @ConditionalOnMissingBean
        Propagation.Factory propagationFactory(BaggagePropagation.FactoryBuilder factoryBuilder) {
            return factoryBuilder.build();
        }

        @Bean
        @ConditionalOnMissingBean
        CorrelationScopeDecorator.Builder mdcCorrelationScopeDecoratorBuilder(
            ObjectProvider<CorrelationScopeCustomizer> correlationScopeCustomizers) {
            CorrelationScopeDecorator.Builder builder = MDCScopeDecorator.newBuilder();
            correlationScopeCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        @Bean
        @Order(0)
        @ConditionalOnProperty(prefix = "dubbo.tracing.baggage.correlation", name = "enabled",
            matchIfMissing = true)
        CorrelationScopeCustomizer correlationFieldsCorrelationScopeCustomizer() {
            return (builder) -> {
                List<String> correlationFields = this.dubboTracingProperties.getBaggage().getCorrelation().getFields();
                for (String field : correlationFields) {
                    builder.add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(BaggageField.create(field))
                        .flushOnUpdate().build());
                }
            };
        }

        @Bean
        @ConditionalOnMissingBean(CurrentTraceContext.ScopeDecorator.class)
        CurrentTraceContext.ScopeDecorator correlationScopeDecorator(CorrelationScopeDecorator.Builder builder) {
            return builder.build();
        }

    }

}
