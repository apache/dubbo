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

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
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
@ConditionalOnClass(Tracer.class)
@ConditionalOnDubboTracingEnable
@AutoConfigureAfter(name = "org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration")
public class DubboMicrometerTracingAutoConfiguration {

    /**
     * {@code @Order} value of
     * {@link #propagatingReceiverTracingObservationHandler(Tracer, Propagator)}.
     */
    public static final int RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER = 1000;

    /**
     * {@code @Order} value of
     * {@link #propagatingSenderTracingObservationHandler(Tracer, Propagator)}.
     */
    public static final int SENDER_TRACING_OBSERVATION_HANDLER_ORDER = 2000;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(Tracer.class)
    public DefaultTracingObservationHandler defaultTracingObservationHandler(Tracer tracer) {
        return new DefaultTracingObservationHandler(tracer);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({Tracer.class, Propagator.class})
    @Order(SENDER_TRACING_OBSERVATION_HANDLER_ORDER)
    public PropagatingSenderTracingObservationHandler<?> propagatingSenderTracingObservationHandler(Tracer tracer,
                                                                                                    Propagator propagator) {
        return new PropagatingSenderTracingObservationHandler<>(tracer, propagator);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({Tracer.class, Propagator.class})
    @Order(RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER)
    public PropagatingReceiverTracingObservationHandler<?> propagatingReceiverTracingObservationHandler(Tracer tracer,
                                                                                                        Propagator propagator) {
        return new PropagatingReceiverTracingObservationHandler<>(tracer, propagator);
    }

}
