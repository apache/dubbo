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
package org.apache.dubbo.config.spring.beans.factory.annotation;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.reference.ReferenceBeanManager;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.MapPropertySource;

/**
 * Tests for the reference bean naming strategy of {@link ReferenceAnnotationBeanPostProcessor},
 * which is selected via {@link ReferenceAnnotationBeanPostProcessor#REFERENCE_BEAN_NAMING_STRATEGY_PROPERTY}.
 *
 * <p>Two consumers declare fields that share one name but reference different service interfaces.
 * With the default {@code field-name} strategy the bean name is derived from the field name, so
 * the first registration occupies the shared name and the second one has to be renamed
 * ({@code demoService#2}). Such an occupied name also breaks by-name injections such as JSR-250
 * {@code @Resource} that point at a different bean with the same name, because those injections
 * happen later and cannot be protected (see apache/dubbo#12637).
 *
 * <p>With the {@code interface-name} strategy the bean name is derived from the simple name of
 * the referenced interface instead, so same-named fields of different interfaces no longer
 * conflict with each other or with unrelated beans.
 */
class ReferenceBeanNamingStrategyTest {

    private static final String STRATEGY_PROPERTY_SOURCE_NAME = ReferenceBeanNamingStrategyTest.class.getSimpleName();

    @BeforeAll
    public static void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void tearDown() {
        DubboBootstrap.reset();
    }

    @Test
    void testDefaultFieldNameStrategy() {
        // no naming strategy configured: keep the historical field-name behavior
        AnnotationConfigApplicationContext context = buildContext(null);
        try {
            // both consumers declare a field named 'demoService': the first registration occupies
            // the bean name 'demoService', the second one is renamed to 'demoService#2'
            Assertions.assertTrue(context.containsBean("demoService"));
            Assertions.assertTrue(context.containsBean("demoService#2"));
            Assertions.assertFalse(context.containsBean("DemoService"));
            Assertions.assertFalse(context.containsBean("HelloService"));

            ReferenceBeanManager referenceBeanManager = getReferenceBeanManager(context);
            Assertions.assertNotNull(referenceBeanManager.getById("demoService"));
            Assertions.assertNotNull(referenceBeanManager.getById("demoService#2"));

            assertConsumersInjected(context);
        } finally {
            context.close();
        }
    }

    @Test
    void testInterfaceNameStrategy() {
        AnnotationConfigApplicationContext context =
                buildContext(ReferenceAnnotationBeanPostProcessor.INTERFACE_NAME_NAMING_STRATEGY);
        try {
            // bean names are derived from the referenced interface simple names,
            // so the same-named fields no longer conflict with each other
            Assertions.assertTrue(context.containsBean("DemoService"));
            Assertions.assertTrue(context.containsBean("HelloService"));
            Assertions.assertFalse(context.containsBean("demoService"));
            Assertions.assertFalse(context.containsBean("demoService#2"));

            ReferenceBeanManager referenceBeanManager = getReferenceBeanManager(context);
            Assertions.assertNotNull(referenceBeanManager.getById("DemoService"));
            Assertions.assertEquals(
                    "DemoService", referenceBeanManager.getById("DemoService").getId());
            Assertions.assertEquals(
                    "HelloService", referenceBeanManager.getById("HelloService").getId());

            Assertions.assertTrue(context.getBean("DemoService") instanceof DemoService);
            Assertions.assertTrue(context.getBean("HelloService") instanceof HelloService);

            assertConsumersInjected(context);
        } finally {
            context.close();
        }
    }

    @Test
    void testUnsetStrategyWithoutApplicationContextFallsBackToFieldName() throws Exception {
        // no application context on the processor: the default field-name strategy applies
        ReferenceAnnotationBeanPostProcessor processor = new ReferenceAnnotationBeanPostProcessor();
        Assertions.assertNull(processor.applicationContext);
        Assertions.assertEquals(
                "demoService",
                processor.resolveDefaultReferenceBeanName("demoService", DemoService.class, consumerField()));
    }

    @Test
    void testInterfaceNameStrategyFallsBackWhenSimpleNameUnavailable() throws Exception {
        ReferenceAnnotationBeanPostProcessor processor =
                processorWithStrategy(ReferenceAnnotationBeanPostProcessor.INTERFACE_NAME_NAMING_STRATEGY);
        // an anonymous class has an empty simple name, so no bean name can be derived from it
        Class<?> anonymousType = new Object() {}.getClass();
        Assertions.assertEquals(
                "demoService",
                processor.resolveDefaultReferenceBeanName("demoService", anonymousType, consumerField()));
    }

    @Test
    void testInterfaceNameStrategyFallsBackWhenInjectedTypeIsNull() throws Exception {
        ReferenceAnnotationBeanPostProcessor processor =
                processorWithStrategy(ReferenceAnnotationBeanPostProcessor.INTERFACE_NAME_NAMING_STRATEGY);
        Assertions.assertEquals(
                "demoService", processor.resolveDefaultReferenceBeanName("demoService", null, consumerField()));
    }

    @Test
    void testStrategyPropertyIsTrimmedAndCaseInsensitive() throws Exception {
        ReferenceAnnotationBeanPostProcessor processor = processorWithStrategy(" Interface-Name ");
        Assertions.assertEquals(
                "DemoService",
                processor.resolveDefaultReferenceBeanName("demoService", DemoService.class, consumerField()));
    }

    @Test
    void testBlankStrategyPropertyFallsBackToFieldName() throws Exception {
        ReferenceAnnotationBeanPostProcessor processor = processorWithStrategy("   ");
        Assertions.assertEquals(
                "demoService",
                processor.resolveDefaultReferenceBeanName("demoService", DemoService.class, consumerField()));
    }

    private void assertConsumersInjected(AnnotationConfigApplicationContext context) {
        // creating the consumer beans triggers the @DubboReference injection of the proxies
        Assertions.assertNotNull(context.getBean(FirstConsumerBean.class).getDemoService());
        Assertions.assertNotNull(context.getBean(SecondConsumerBean.class).getDemoService());
    }

    private ReferenceBeanManager getReferenceBeanManager(AnnotationConfigApplicationContext context) {
        return context.getBean(ReferenceBeanManager.BEAN_NAME, ReferenceBeanManager.class);
    }

    private ReferenceAnnotationBeanPostProcessor processorWithStrategy(String strategy) {
        ReferenceAnnotationBeanPostProcessor processor = new ReferenceAnnotationBeanPostProcessor();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (strategy != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(ReferenceAnnotationBeanPostProcessor.REFERENCE_BEAN_NAMING_STRATEGY_PROPERTY, strategy);
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource(STRATEGY_PROPERTY_SOURCE_NAME, properties));
        }
        // the strategy lookup only reads the environment; assigning the protected field directly
        // avoids pulling in the ReferenceBeanManager machinery of setApplicationContext
        processor.applicationContext = context;
        return processor;
    }

    private Member consumerField() throws NoSuchFieldException {
        return FirstConsumerBean.class.getDeclaredField("demoService");
    }

    private AnnotationConfigApplicationContext buildContext(String namingStrategy) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (namingStrategy != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(
                    ReferenceAnnotationBeanPostProcessor.REFERENCE_BEAN_NAMING_STRATEGY_PROPERTY, namingStrategy);
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource(STRATEGY_PROPERTY_SOURCE_NAME, properties));
        }
        context.register(DubboConfiguration.class, EnableDubboConfiguration.class, ConsumerConfiguration.class);
        context.refresh();
        return context;
    }

    @Configuration
    @EnableDubbo(scanBasePackages = "org.apache.dubbo.config.spring.context.annotation.provider")
    public static class EnableDubboConfiguration {}

    // The scanned provider package (org.apache.dubbo.config.spring.context.annotation.provider) contains
    // Dubbo services whose @DubboService attributes are placeholders (${demo.service.application},
    // ${demo.service.protocol}, ${demo.service.registry}, ${demo.service.version}), resolved from
    // META-INF/default.properties as in ServiceAnnotationTestConfiguration of the reference tests.
    // The bean ids below must match the resolved placeholder values so that the exported providers
    // can look up the ApplicationConfig / RegistryConfig / ProtocolConfig entries defined here.
    @Configuration
    @PropertySource("classpath:/META-INF/default.properties")
    public static class DubboConfiguration {

        @Bean("dubbo-demo-application")
        public ApplicationConfig applicationConfig() {
            ApplicationConfig applicationConfig = new ApplicationConfig();
            applicationConfig.setName("dubbo-demo-application");
            return applicationConfig;
        }

        @Bean("my-registry")
        public RegistryConfig registryConfig() {
            RegistryConfig registryConfig = new RegistryConfig();
            registryConfig.setAddress("N/A");
            return registryConfig;
        }

        @Bean("dubbo")
        public ProtocolConfig protocolConfig() {
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setName("dubbo");
            protocolConfig.setPort(12345);
            return protocolConfig;
        }
    }

    @Configuration
    public static class ConsumerConfiguration {

        @Bean
        public FirstConsumerBean firstConsumerBean() {
            return new FirstConsumerBean();
        }

        @Bean
        public SecondConsumerBean secondConsumerBean() {
            return new SecondConsumerBean();
        }
    }

    static class FirstConsumerBean {

        // the field name 'demoService' is intentionally the same as the field name of
        // SecondConsumerBean, while the referenced interface is a different type
        @DubboReference(url = "dubbo://127.0.0.1:20880", check = false)
        private HelloService demoService;

        public HelloService getDemoService() {
            return demoService;
        }
    }

    static class SecondConsumerBean {

        @DubboReference(url = "dubbo://127.0.0.1:20880", check = false)
        private DemoService demoService;

        public DemoService getDemoService() {
            return demoService;
        }
    }
}
