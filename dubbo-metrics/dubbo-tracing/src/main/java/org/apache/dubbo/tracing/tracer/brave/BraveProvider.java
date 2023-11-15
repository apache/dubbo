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
package org.apache.dubbo.tracing.tracer.brave;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.nested.BaggageConfig;
import org.apache.dubbo.config.nested.ExporterConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.tracing.exporter.zipkin.ZipkinSpanHandler;
import org.apache.dubbo.tracing.tracer.TracerProvider;
import org.apache.dubbo.tracing.utils.PropagationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.W3CPropagation;

import static org.apache.dubbo.tracing.utils.ObservationConstants.DEFAULT_APPLICATION_NAME;
import static org.apache.dubbo.tracing.utils.ObservationSupportUtil.isSupportBraveURLSender;

public class BraveProvider implements TracerProvider {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(BraveProvider.class);

    private static final BraveBaggageManager BRAVE_BAGGAGE_MANAGER = new BraveBaggageManager();

    private final ApplicationModel applicationModel;
    private final TracingConfig tracingConfig;

    public BraveProvider(ApplicationModel applicationModel, TracingConfig tracingConfig) {
        this.applicationModel = applicationModel;
        this.tracingConfig = tracingConfig;
    }

    @Override
    public Tracer getTracer() {
        // [Brave component] SpanHandler is a component that gets called when a span is finished.
        List<brave.handler.SpanHandler> spanHandlerList = getSpanHandlers();

        String applicationName = applicationModel
                .getApplicationConfigManager()
                .getApplication()
                .map(ApplicationConfig::getName)
                .orElse(DEFAULT_APPLICATION_NAME);

        // [Brave component] CurrentTraceContext is a Brave component that allows you to
        // retrieve the current TraceContext.
        brave.propagation.ThreadLocalCurrentTraceContext braveCurrentTraceContext =
                brave.propagation.ThreadLocalCurrentTraceContext.newBuilder()
                        .addScopeDecorator(correlationScopeDecorator()) // Brave's automatic MDC setup
                        .build();

        // [Micrometer Tracing component] A Micrometer Tracing wrapper for Brave's CurrentTraceContext
        CurrentTraceContext bridgeContext = new BraveCurrentTraceContext(braveCurrentTraceContext);

        // [Brave component] Tracing is the root component that allows to configure the
        // tracer, handlers, context propagation etc.
        brave.Tracing.Builder builder = brave.Tracing.newBuilder()
                .currentTraceContext(braveCurrentTraceContext)
                .supportsJoin(false)
                .traceId128Bit(true)
                .localServiceName(applicationName)
                // For Baggage to work you need to provide a list of fields to propagate
                .propagationFactory(PropagatorFactory.getPropagationFactory(tracingConfig))
                .sampler(getSampler());
        spanHandlerList.forEach(builder::addSpanHandler);

        brave.Tracing tracing = builder.build();

        BravePropagatorProvider.createMicrometerPropagator(tracing);

        // [Brave component] Tracer is a component that handles the life-cycle of a span
        brave.Tracer braveTracer = tracing.tracer();

        // [Micrometer Tracing component] A Micrometer Tracing wrapper for Brave's Tracer
        return new BraveTracer(braveTracer, bridgeContext, BRAVE_BAGGAGE_MANAGER);
    }

    private List<brave.handler.SpanHandler> getSpanHandlers() {
        ExporterConfig exporterConfig = tracingConfig.getTracingExporter();
        List<brave.handler.SpanHandler> res = new ArrayList<>();
        if (!isSupportBraveURLSender()) {
            return res;
        }
        ExporterConfig.ZipkinConfig zipkinConfig = exporterConfig.getZipkinConfig();
        if (zipkinConfig != null && StringUtils.isNotEmpty(zipkinConfig.getEndpoint())) {
            LOGGER.info("Create zipkin span handler.");
            res.add(ZipkinSpanHandler.getSpanHandler(applicationModel, zipkinConfig));
        }
        return res;
    }

    private brave.sampler.Sampler getSampler() {
        return brave.sampler.Sampler.create(tracingConfig.getSampling().getProbability());
    }

    private Optional<brave.baggage.CorrelationScopeCustomizer> correlationFieldsCorrelationScopeCustomizer() {
        BaggageConfig.Correlation correlation = tracingConfig.getBaggage().getCorrelation();
        boolean enabled = correlation.isEnabled();
        if (!enabled) {
            return Optional.empty();
        }
        return Optional.of((builder) -> {
            List<String> correlationFields = correlation.getFields();
            for (String field : correlationFields) {
                builder.add(brave.baggage.CorrelationScopeConfig.SingleCorrelationField.newBuilder(
                                brave.baggage.BaggageField.create(field))
                        .flushOnUpdate()
                        .build());
            }
        });
    }

    private brave.propagation.CurrentTraceContext.ScopeDecorator correlationScopeDecorator() {
        brave.baggage.CorrelationScopeDecorator.Builder builder = brave.context.slf4j.MDCScopeDecorator.newBuilder();
        correlationFieldsCorrelationScopeCustomizer().ifPresent((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    static class PropagatorFactory {

        public static brave.propagation.Propagation.Factory getPropagationFactory(TracingConfig tracingConfig) {
            BaggageConfig baggageConfig = tracingConfig.getBaggage();
            if (baggageConfig == null || !baggageConfig.getEnabled()) {
                return getPropagationFactoryWithoutBaggage(tracingConfig);
            }
            return getPropagationFactoryWithBaggage(tracingConfig);
        }

        private static brave.propagation.Propagation.Factory getPropagationFactoryWithoutBaggage(
                TracingConfig tracingConfig) {
            PropagationType propagationType =
                    PropagationType.forValue(tracingConfig.getPropagation().getType());
            if (PropagationType.W3C == propagationType) {
                return new io.micrometer.tracing.brave.bridge.W3CPropagation();
            } else {
                // Brave default propagation is B3
                return brave.propagation.B3Propagation.newFactoryBuilder()
                        .injectFormat(brave.propagation.B3Propagation.Format.SINGLE_NO_PARENT)
                        .build();
            }
        }

        private static brave.propagation.Propagation.Factory getPropagationFactoryWithBaggage(
                TracingConfig tracingConfig) {
            PropagationType propagationType =
                    PropagationType.forValue(tracingConfig.getPropagation().getType());
            brave.propagation.Propagation.Factory delegate;
            if (PropagationType.W3C == propagationType) {
                delegate = new W3CPropagation(BRAVE_BAGGAGE_MANAGER, Collections.emptyList());
            } else {
                // Brave default propagation is B3
                delegate = brave.propagation.B3Propagation.newFactoryBuilder()
                        .injectFormat(brave.propagation.B3Propagation.Format.SINGLE_NO_PARENT)
                        .build();
            }
            return getBaggageFactoryBuilder(delegate, tracingConfig).build();
        }

        private static brave.baggage.BaggagePropagation.FactoryBuilder getBaggageFactoryBuilder(
                brave.propagation.Propagation.Factory delegate, TracingConfig tracingConfig) {
            brave.baggage.BaggagePropagation.FactoryBuilder builder =
                    brave.baggage.BaggagePropagation.newFactoryBuilder(delegate);

            getBaggagePropagationCustomizers(tracingConfig).forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        private static List<brave.baggage.BaggagePropagationCustomizer> getBaggagePropagationCustomizers(
                TracingConfig tracingConfig) {
            List<brave.baggage.BaggagePropagationCustomizer> res = new ArrayList<>();
            if (tracingConfig.getBaggage().getCorrelation().isEnabled()) {
                res.add(remoteFieldsBaggagePropagationCustomizer(tracingConfig));
            }
            return res;
        }

        private static brave.baggage.BaggagePropagationCustomizer remoteFieldsBaggagePropagationCustomizer(
                TracingConfig tracingConfig) {
            return (builder) -> {
                List<String> remoteFields = tracingConfig.getBaggage().getRemoteFields();
                for (String fieldName : remoteFields) {
                    builder.add(brave.baggage.BaggagePropagationConfig.SingleBaggageField.remote(
                            brave.baggage.BaggageField.create(fieldName)));
                }
            };
        }
    }
}
