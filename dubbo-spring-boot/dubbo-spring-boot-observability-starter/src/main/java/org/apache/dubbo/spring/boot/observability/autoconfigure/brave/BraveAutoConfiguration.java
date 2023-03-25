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
@ConditionalOnClass(name={"io.micrometer.tracing.Tracer", "io.micrometer.tracing.brave.bridge.BraveTracer","io.micrometer.tracing.brave.bridge.BraveBaggageManager","brave.Tracing"})
@EnableConfigurationProperties(DubboTracingProperties.class)
@ConditionalOnDubboTracingEnable
public class BraveAutoConfiguration {

    private static final io.micrometer.tracing.brave.bridge.BraveBaggageManager BRAVE_BAGGAGE_MANAGER = new io.micrometer.tracing.brave.bridge.BraveBaggageManager();

    /**
     * Default value for application name if {@code spring.application.name} is not set.
     */
    private static final String DEFAULT_APPLICATION_NAME = "application";

    @Bean
    @ConditionalOnMissingBean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    io.micrometer.tracing.brave.bridge.CompositeSpanHandler compositeSpanHandler(ObjectProvider<io.micrometer.tracing.exporter.SpanExportingPredicate> predicates,
                                              ObjectProvider<io.micrometer.tracing.exporter.SpanReporter> reporters, ObjectProvider<io.micrometer.tracing.exporter.SpanFilter> filters) {
        return new io.micrometer.tracing.brave.bridge.CompositeSpanHandler(predicates.orderedStream().collect(Collectors.toList()),
            reporters.orderedStream().collect(Collectors.toList()),
            filters.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    public brave.Tracing braveTracing(Environment environment, List<brave.handler.SpanHandler> spanHandlers,
                                List<brave.TracingCustomizer> tracingCustomizers, brave.propagation.CurrentTraceContext currentTraceContext,
                                      brave.propagation.Propagation.Factory propagationFactory,  brave.sampler.Sampler sampler) {
        String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
        brave.Tracing.Builder builder = brave.Tracing.newBuilder().currentTraceContext(currentTraceContext).traceId128Bit(true)
            .supportsJoin(false).propagationFactory(propagationFactory).sampler(sampler)
            .localServiceName(applicationName);
        spanHandlers.forEach(builder::addSpanHandler);
        for (brave.TracingCustomizer tracingCustomizer : tracingCustomizers) {
            tracingCustomizer.customize(builder);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public brave.Tracer braveTracer(brave.Tracing tracing) {
        return tracing.tracer();
    }

    @Bean
    @ConditionalOnMissingBean
    public brave.propagation.CurrentTraceContext braveCurrentTraceContext(List<brave.propagation.CurrentTraceContext.ScopeDecorator> scopeDecorators,
                                                        List<brave.propagation.CurrentTraceContextCustomizer> currentTraceContextCustomizers) {
        brave.propagation.ThreadLocalCurrentTraceContext.Builder builder = brave.propagation.ThreadLocalCurrentTraceContext.newBuilder();
        scopeDecorators.forEach(builder::addScopeDecorator);
        for (brave.propagation.CurrentTraceContextCustomizer currentTraceContextCustomizer : currentTraceContextCustomizers) {
            currentTraceContextCustomizer.customize(builder);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public  brave.sampler.Sampler braveSampler(DubboTracingProperties properties) {
        return  brave.sampler.Sampler.create(properties.getSampling().getProbability());
    }

    @Bean
    @ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
    io.micrometer.tracing.brave.bridge.BraveTracer braveTracerBridge(brave.Tracer tracer, brave.propagation.CurrentTraceContext currentTraceContext) {
        return new io.micrometer.tracing.brave.bridge.BraveTracer(tracer, new io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext(currentTraceContext), BRAVE_BAGGAGE_MANAGER);
    }

    @Bean
    @ConditionalOnMissingBean
    io.micrometer.tracing.brave.bridge.BravePropagator bravePropagator(brave.Tracing tracing) {
        return new io.micrometer.tracing.brave.bridge.BravePropagator(tracing);
    }

    @Bean
    @ConditionalOnMissingBean(brave.SpanCustomizer.class)
    brave.CurrentSpanCustomizer currentSpanCustomizer(brave.Tracing tracing) {
        return brave.CurrentSpanCustomizer.create(tracing);
    }

    @Bean
    @ConditionalOnMissingBean(io.micrometer.tracing.SpanCustomizer.class)
    io.micrometer.tracing.brave.bridge.BraveSpanCustomizer braveSpanCustomizer(brave.SpanCustomizer spanCustomizer) {
        return new io.micrometer.tracing.brave.bridge.BraveSpanCustomizer(spanCustomizer);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(value = "dubbo.tracing.baggage.enabled", havingValue = "false")
    static class BraveNoBaggageConfiguration {

        @Bean
        @ConditionalOnMissingBean
        brave.propagation.Propagation.Factory propagationFactory(DubboTracingProperties tracing) {
            DubboTracingProperties.Propagation.PropagationType type = tracing.getPropagation().getType();
            switch (type) {
                case B3:
                    return brave.propagation.B3Propagation.newFactoryBuilder().injectFormat(brave.propagation.B3Propagation.Format.SINGLE_NO_PARENT).build();
                case W3C:
                    return new io.micrometer.tracing.brave.bridge.W3CPropagation();
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
        brave.baggage.BaggagePropagation.FactoryBuilder b3PropagationFactoryBuilder(
            ObjectProvider<brave.baggage.BaggagePropagationCustomizer> baggagePropagationCustomizers) {
            brave.propagation.Propagation.Factory delegate =
                brave.propagation.B3Propagation.newFactoryBuilder().injectFormat(brave.propagation.B3Propagation.Format.SINGLE_NO_PARENT).build();

            brave.baggage.BaggagePropagation.FactoryBuilder builder = brave.baggage.BaggagePropagation.newFactoryBuilder(delegate);
            baggagePropagationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "dubbo.tracing.propagation", value = "type", havingValue = "W3C", matchIfMissing = true)
        brave.baggage.BaggagePropagation.FactoryBuilder w3cPropagationFactoryBuilder(
            ObjectProvider<brave.baggage.BaggagePropagationCustomizer> baggagePropagationCustomizers) {
            brave.propagation.Propagation.Factory delegate = new io.micrometer.tracing.brave.bridge.W3CPropagation(BRAVE_BAGGAGE_MANAGER, Collections.emptyList());

            brave.baggage.BaggagePropagation.FactoryBuilder builder = brave.baggage.BaggagePropagation.newFactoryBuilder(delegate);
            baggagePropagationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        @Bean
        @ConditionalOnMissingBean
        @Order(0)
        brave.baggage.BaggagePropagationCustomizer remoteFieldsBaggagePropagationCustomizer() {
            return (builder) -> {
                List<String> remoteFields = dubboTracingProperties.getBaggage().getRemoteFields();
                for (String fieldName : remoteFields) {
                    builder.add(brave.baggage.BaggagePropagationConfig.SingleBaggageField.remote(brave.baggage.BaggageField.create(fieldName)));
                }
            };
        }

        @Bean
        @ConditionalOnMissingBean
        brave.propagation.Propagation.Factory propagationFactory(brave.baggage.BaggagePropagation.FactoryBuilder factoryBuilder) {
            return factoryBuilder.build();
        }

        @Bean
        @ConditionalOnMissingBean
        brave.baggage.CorrelationScopeDecorator.Builder mdcCorrelationScopeDecoratorBuilder(
            ObjectProvider<brave.baggage.CorrelationScopeCustomizer> correlationScopeCustomizers) {
            brave.baggage.CorrelationScopeDecorator.Builder builder = brave.context.slf4j.MDCScopeDecorator.newBuilder();
            correlationScopeCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        @Bean
        @Order(0)
        @ConditionalOnProperty(prefix = "dubbo.tracing.baggage.correlation", name = "enabled",
            matchIfMissing = true)
        brave.baggage.CorrelationScopeCustomizer correlationFieldsCorrelationScopeCustomizer() {
            return (builder) -> {
                List<String> correlationFields = this.dubboTracingProperties.getBaggage().getCorrelation().getFields();
                for (String field : correlationFields) {
                    builder.add(brave.baggage.CorrelationScopeConfig.SingleCorrelationField.newBuilder(brave.baggage.BaggageField.create(field))
                        .flushOnUpdate().build());
                }
            };
        }

        @Bean
        @ConditionalOnMissingBean(brave.propagation.CurrentTraceContext.ScopeDecorator.class)
        brave.propagation.CurrentTraceContext.ScopeDecorator correlationScopeDecorator(brave.baggage.CorrelationScopeDecorator.Builder builder) {
            return builder.build();
        }

    }

}
