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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.spring.boot.observability.annotation.ConditionalOnDubboTracingEnable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Register observationRegistry to ApplicationModel.
 * Create observationRegistry when you are using Boot <3.0 or you are not using spring-boot-starter-actuator
 */
@AutoConfiguration(after =DubboMicrometerTracingAutoConfiguration.class,afterName = "org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration")
@ConditionalOnDubboTracingEnable
public class DubboObservationAutoConfiguration implements BeanFactoryAware, SmartInitializingSingleton {

    public DubboObservationAutoConfiguration(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    private final ApplicationModel applicationModel;

    private BeanFactory beanFactory;

    @Bean
    @ConditionalOnMissingBean
    ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }

    @Bean
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    public ObservationRegistryPostProcessor dubboObservationRegistryPostProcessor(ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping,
                                                                                  ObjectProvider<ObservationHandler<?>> observationHandlers) {
        return new ObservationRegistryPostProcessor(observationHandlerGrouping, observationHandlers);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        applicationModel.getBeanFactory().registerBean(beanFactory.getBean(ObservationRegistry.class));
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnMissingClass("io.micrometer.tracing.Tracer")
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    static class OnlyMetricsConfiguration {

        @Bean
        ObservationHandlerGrouping metricsObservationHandlerGrouping() {
            return new ObservationHandlerGrouping(MeterObservationHandler.class);
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Tracer.class)
    @ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    static class OnlyTracingConfiguration {

        @Bean
        ObservationHandlerGrouping tracingObservationHandlerGrouping() {
            return new ObservationHandlerGrouping(TracingObservationHandler.class);
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ MeterRegistry.class, Tracer.class })
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    static class MetricsWithTracingConfiguration {

        @Bean
        ObservationHandlerGrouping metricsAndTracingObservationHandlerGrouping() {
            return new ObservationHandlerGrouping(
               Arrays.asList(TracingObservationHandler.class, MeterObservationHandler.class));
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(MeterObservationHandler.class)
    static class MeterObservationHandlerConfiguration {

        @ConditionalOnMissingBean(type = "io.micrometer.tracing.Tracer")
        @Configuration(proxyBeanMethods = false)
        static class OnlyMetricsMeterObservationHandlerConfiguration {

            @Bean
            DefaultMeterObservationHandler defaultMeterObservationHandler(MeterRegistry meterRegistry) {
                return new DefaultMeterObservationHandler(meterRegistry);
            }

        }

        @ConditionalOnBean(Tracer.class)
        @Configuration(proxyBeanMethods = false)
        static class TracingAndMetricsObservationHandlerConfiguration {

            @Bean
            TracingAwareMeterObservationHandler<Observation.Context> tracingAwareMeterObservationHandler(
                MeterRegistry meterRegistry, Tracer tracer) {
                DefaultMeterObservationHandler delegate = new DefaultMeterObservationHandler(meterRegistry);
                return new TracingAwareMeterObservationHandler<>(delegate, tracer);
            }

        }

    }
}
