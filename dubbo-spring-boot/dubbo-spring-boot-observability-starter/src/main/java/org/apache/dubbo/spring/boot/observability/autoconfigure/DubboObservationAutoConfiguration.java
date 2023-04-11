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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.qos.protocol.QosProtocolWrapper;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.spring.boot.observability.annotation.ConditionalOnDubboTracingEnable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
@AutoConfiguration(after = DubboMicrometerTracingAutoConfiguration.class, afterName = "org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration")
@ConditionalOnDubboTracingEnable
@ConditionalOnClass(name = {"io.micrometer.observation.Observation", "io.micrometer.tracing.Tracer"})
public class DubboObservationAutoConfiguration implements BeanFactoryAware, SmartInitializingSingleton {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(QosProtocolWrapper.class);


    public DubboObservationAutoConfiguration(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    private final ApplicationModel applicationModel;

    private BeanFactory beanFactory;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.micrometer.observation.ObservationRegistry")
    io.micrometer.observation.ObservationRegistry observationRegistry() {
        return io.micrometer.observation.ObservationRegistry.create();
    }

    @Bean
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    @ConditionalOnClass(name = "io.micrometer.observation.ObservationHandler")
    public ObservationRegistryPostProcessor dubboObservationRegistryPostProcessor(ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping,
                                                                                  ObjectProvider<io.micrometer.observation.ObservationHandler<?>> observationHandlers) {
        return new ObservationRegistryPostProcessor(observationHandlerGrouping, observationHandlers);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            applicationModel.getBeanFactory().registerBean(beanFactory.getBean(io.micrometer.observation.ObservationRegistry.class));
            io.micrometer.tracing.Tracer bean = beanFactory.getBean(io.micrometer.tracing.Tracer.class);
            applicationModel.getBeanFactory().registerBean(bean);
        } catch (NoSuchBeanDefinitionException e) {
            logger.info("Please use a version of micrometer higher than 1.10.0 ：{}" + e.getMessage());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnMissingClass("io.micrometer.tracing.Tracer")
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    static class OnlyMetricsConfiguration {

        @Bean
        @ConditionalOnClass(name = "io.micrometer.core.instrument.observation.MeterObservationHandler")
        ObservationHandlerGrouping metricsObservationHandlerGrouping() {
            return new ObservationHandlerGrouping(io.micrometer.core.instrument.observation.MeterObservationHandler.class);
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(io.micrometer.tracing.Tracer.class)
    @ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    static class OnlyTracingConfiguration {

        @Bean
        @ConditionalOnClass(name = "io.micrometer.tracing.handler.TracingObservationHandler")
        ObservationHandlerGrouping tracingObservationHandlerGrouping() {
            return new ObservationHandlerGrouping(io.micrometer.tracing.handler.TracingObservationHandler.class);
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({MeterRegistry.class, io.micrometer.tracing.Tracer.class})
    @ConditionalOnMissingBean(type = "org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryPostProcessor")
    static class MetricsWithTracingConfiguration {

        @Bean
        @ConditionalOnClass(name = {"io.micrometer.tracing.handler.TracingObservationHandler", "io.micrometer.core.instrument.observation.MeterObservationHandler"})
        ObservationHandlerGrouping metricsAndTracingObservationHandlerGrouping() {
            return new ObservationHandlerGrouping(
                Arrays.asList(io.micrometer.tracing.handler.TracingObservationHandler.class, io.micrometer.core.instrument.observation.MeterObservationHandler.class));
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(io.micrometer.core.instrument.observation.MeterObservationHandler.class)
    static class MeterObservationHandlerConfiguration {

        @ConditionalOnMissingBean(type = "io.micrometer.tracing.Tracer")
        @Configuration(proxyBeanMethods = false)
        static class OnlyMetricsMeterObservationHandlerConfiguration {

            @Bean
            @ConditionalOnClass(name = {"io.micrometer.core.instrument.observation.DefaultMeterObservationHandler"})
            io.micrometer.core.instrument.observation.DefaultMeterObservationHandler defaultMeterObservationHandler(MeterRegistry meterRegistry) {
                return new io.micrometer.core.instrument.observation.DefaultMeterObservationHandler(meterRegistry);
            }

        }

        @ConditionalOnBean(io.micrometer.tracing.Tracer.class)
        @Configuration(proxyBeanMethods = false)
        static class TracingAndMetricsObservationHandlerConfiguration {

            @Bean
            @ConditionalOnClass(name = {"io.micrometer.tracing.handler.TracingAwareMeterObservationHandler", "io.micrometer.tracing.Tracer"})
            io.micrometer.tracing.handler.TracingAwareMeterObservationHandler<io.micrometer.observation.Observation.Context> tracingAwareMeterObservationHandler(
                MeterRegistry meterRegistry, io.micrometer.tracing.Tracer tracer) {
                io.micrometer.core.instrument.observation.DefaultMeterObservationHandler delegate = new io.micrometer.core.instrument.observation.DefaultMeterObservationHandler(meterRegistry);
                return new io.micrometer.tracing.handler.TracingAwareMeterObservationHandler<>(delegate, tracer);
            }

        }

    }
}
