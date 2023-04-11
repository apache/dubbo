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
package org.apache.dubbo.spring.boot.observability.autoconfigure;

import org.apache.dubbo.spring.boot.observability.annotation.ConditionalOnDubboTracingEnable;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * copy from {@link org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration}
 * this class is available starting from Boot 3.0. It's not available if you're using Boot < 3.0
 */
@ConditionalOnDubboTracingEnable
@ConditionalOnClass(name = {"io.micrometer.observation.Observation","io.micrometer.tracing.Tracer","io.micrometer.tracing.propagation.Propagator"})
@AutoConfigureAfter(name = "org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration")
public class DubboMicrometerTracingAutoConfiguration {

    /**
     * {@code @Order} value of
     * {@link #propagatingReceiverTracingObservationHandler(io.micrometer.tracing.Tracer, io.micrometer.tracing.propagation.Propagator )}.
     */
    public static final int RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER = 1000;

    /**
     * {@code @Order} value of
     * {@link #propagatingSenderTracingObservationHandler(io.micrometer.tracing.Tracer, io.micrometer.tracing.propagation.Propagator )}.
     */
    public static final int SENDER_TRACING_OBSERVATION_HANDLER_ORDER = 2000;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(io.micrometer.tracing.Tracer.class)
    public io.micrometer.tracing.handler.DefaultTracingObservationHandler defaultTracingObservationHandler(io.micrometer.tracing.Tracer tracer) {
        return new io.micrometer.tracing.handler.DefaultTracingObservationHandler(tracer);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({io.micrometer.tracing.Tracer.class, io.micrometer.tracing.propagation.Propagator.class})
    @Order(SENDER_TRACING_OBSERVATION_HANDLER_ORDER)
    public io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler<?> propagatingSenderTracingObservationHandler(io.micrometer.tracing.Tracer tracer,
                                                                                                                                  io.micrometer.tracing.propagation.Propagator propagator) {
        return new io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler<>(tracer, propagator);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({io.micrometer.tracing.Tracer.class, io.micrometer.tracing.propagation.Propagator.class})
    @Order(RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER)
    public io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler<?> propagatingReceiverTracingObservationHandler(io.micrometer.tracing.Tracer tracer,
                                                                                                                                      io.micrometer.tracing.propagation.Propagator propagator) {
        return new io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler<>(tracer, propagator);
    }

}
