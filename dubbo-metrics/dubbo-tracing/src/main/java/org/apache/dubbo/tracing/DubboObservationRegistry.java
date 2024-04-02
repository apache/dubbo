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
package org.apache.dubbo.tracing;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.metrics.MetricsGlobalRegistry;
import org.apache.dubbo.metrics.utils.MetricsSupportUtil;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.tracing.handler.DubboClientTracingObservationHandler;
import org.apache.dubbo.tracing.handler.DubboServerTracingObservationHandler;
import org.apache.dubbo.tracing.tracer.PropagatorProvider;
import org.apache.dubbo.tracing.tracer.PropagatorProviderFactory;
import org.apache.dubbo.tracing.tracer.TracerProvider;
import org.apache.dubbo.tracing.tracer.TracerProviderFactory;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_NOT_FOUND_TRACER_DEPENDENCY;

public class DubboObservationRegistry {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DubboObservationRegistry.class);

    private final ApplicationModel applicationModel;

    private final TracingConfig tracingConfig;

    public DubboObservationRegistry(ApplicationModel applicationModel, TracingConfig tracingConfig) {
        this.applicationModel = applicationModel;
        this.tracingConfig = tracingConfig;
    }

    public void initObservationRegistry() {
        // If get ObservationRegistry.class from external(eg Spring.), use external.
        io.micrometer.observation.ObservationRegistry externalObservationRegistry =
                applicationModel.getBeanFactory().getBean(io.micrometer.observation.ObservationRegistry.class);
        if (externalObservationRegistry != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ObservationRegistry.class from external is existed.");
            }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Tracing config is: " + JsonUtils.toJson(tracingConfig));
        }

        TracerProvider tracerProvider = TracerProviderFactory.getProvider(applicationModel, tracingConfig);
        if (tracerProvider == null) {
            logger.warn(
                    COMMON_NOT_FOUND_TRACER_DEPENDENCY,
                    "",
                    "",
                    "Can not found OpenTelemetry/Brave tracer dependencies, skip init ObservationRegistry.");
            return;
        }
        // The real tracer will come from tracer implementation (OTel / Brave)
        io.micrometer.tracing.Tracer tracer = tracerProvider.getTracer();

        // The real propagator will come from tracer implementation (OTel / Brave)
        PropagatorProvider propagatorProvider = PropagatorProviderFactory.getPropagatorProvider();
        io.micrometer.tracing.propagation.Propagator propagator = propagatorProvider != null
                ? propagatorProvider.getPropagator()
                : io.micrometer.tracing.propagation.Propagator.NOOP;

        io.micrometer.observation.ObservationRegistry registry = io.micrometer.observation.ObservationRegistry.create();
        registry.observationConfig()
                // set up a first matching handler that creates spans - it comes from Micrometer Tracing.
                // set up spans for sending and receiving data over the wire and a default one.
                .observationHandler(
                        new io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler(
                                new io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler<>(
                                        tracer, propagator),
                                new io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler<>(
                                        tracer, propagator),
                                new io.micrometer.tracing.handler.DefaultTracingObservationHandler(tracer)))
                .observationHandler(
                        new io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler(
                                new DubboClientTracingObservationHandler<>(tracer),
                                new DubboServerTracingObservationHandler<>(tracer)));

        if (MetricsSupportUtil.isSupportMetrics()) {
            io.micrometer.core.instrument.MeterRegistry meterRegistry =
                    MetricsGlobalRegistry.getCompositeRegistry(applicationModel);
            registry.observationConfig()
                    .observationHandler(new io.micrometer.core.instrument.observation.DefaultMeterObservationHandler(
                            meterRegistry));
        }

        applicationModel.getBeanFactory().registerBean(registry);
        applicationModel.getBeanFactory().registerBean(tracer);
        applicationModel.getBeanFactory().registerBean(propagator);
    }
}
