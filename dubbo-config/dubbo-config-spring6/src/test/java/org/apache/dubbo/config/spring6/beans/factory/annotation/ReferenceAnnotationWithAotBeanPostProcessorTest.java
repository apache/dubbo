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
package org.apache.dubbo.config.spring6.beans.factory.annotation;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.Constants;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.reference.ReferenceAttributes;
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.service.EchoService;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.javapoet.ClassName;

public class ReferenceAnnotationWithAotBeanPostProcessorTest {

    private static final String AOT_PROCESSING_PROPERTY = "spring.aot.processing";

    @AfterEach
    void clearAotProcessingProperty() {
        System.clearProperty(AOT_PROCESSING_PROPERTY);
    }

    @Test
    void javaConfigReferenceBeanPersistsMetadataForGeneratedArtifacts() {
        System.setProperty(AOT_PROCESSING_PROPERTY, "true");

        AnnotationConfigApplicationContext context = createContextWithReferencePostProcessor();
        try {
            BeanDefinition definition = context.getBeanFactory().getBeanDefinition("demoService");

            Object referencePropsJson = definition.getPropertyValues().get("referencePropsJson");
            Assertions.assertAll(
                    () -> Assertions.assertEquals(
                            DemoService.class, definition.getAttribute(ReferenceAttributes.INTERFACE_CLASS)),
                    () -> Assertions.assertEquals(
                            DemoService.class.getName(), definition.getAttribute(ReferenceAttributes.INTERFACE_NAME)),
                    () -> Assertions.assertNotNull(definition.getAttribute(Constants.REFERENCE_PROPS)),
                    () -> Assertions.assertEquals(
                            "demoService", definition.getPropertyValues().get(ReferenceAttributes.ID)),
                    () -> Assertions.assertEquals(
                            DemoService.class, definition.getPropertyValues().get(ReferenceAttributes.INTERFACE_CLASS)),
                    () -> Assertions.assertEquals(
                            DemoService.class.getName(),
                            definition.getPropertyValues().get(ReferenceAttributes.INTERFACE_NAME)),
                    () -> Assertions.assertNotNull(
                            referencePropsJson,
                            "AOT processing must persist @DubboReference properties as referencePropsJson"),
                    () -> {
                        if (referencePropsJson != null) {
                            Assertions.assertEquals(
                                    "demo",
                                    ((Map<?, ?>) JsonUtils.toJavaObject((String) referencePropsJson, Map.class))
                                            .get("group"));
                        }
                    },
                    () -> Assertions.assertEquals(
                            "demo", ((Map<?, ?>) definition.getAttribute(Constants.REFERENCE_PROPS)).get("group")));
        } finally {
            context.close();
        }
    }

    @Test
    void javaConfigReferenceBeanProducesAotContributionForProxyHints() {
        AnnotationConfigApplicationContext context =
                createContextWithReferencePostProcessor(GenericReferenceConfiguration.class);
        AnnotationConfigApplicationContext fieldContext =
                new AnnotationConfigApplicationContext(FieldReferenceConfiguration.class);

        try {
            ReferenceAnnotationWithAotBeanPostProcessor postProcessor =
                    new ReferenceAnnotationWithAotBeanPostProcessor();
            postProcessor.setBeanFactory(context.getBeanFactory());
            postProcessor.setBeanClassLoader(getClass().getClassLoader());
            postProcessor.setEnvironment(context.getEnvironment());

            ReferenceAnnotationWithAotBeanPostProcessor fieldPostProcessor =
                    new ReferenceAnnotationWithAotBeanPostProcessor();
            fieldPostProcessor.setBeanFactory(fieldContext.getBeanFactory());
            fieldPostProcessor.setBeanClassLoader(getClass().getClassLoader());
            fieldPostProcessor.setEnvironment(fieldContext.getEnvironment());
            BeanRegistrationAotContribution fieldContribution = fieldPostProcessor.processAheadOfTime(
                    RegisteredBean.of(fieldContext.getBeanFactory(), "fieldConsumer"));
            BeanRegistrationAotContribution referenceBeanContribution =
                    postProcessor.processAheadOfTime(RegisteredBean.of(context.getBeanFactory(), "demoService"));
            BeanRegistrationAotContribution genericReferenceBeanContribution =
                    postProcessor.processAheadOfTime(RegisteredBean.of(context.getBeanFactory(), "genericDemoService"));

            Assertions.assertNotNull(fieldContribution);
            Assertions.assertNotNull(
                    referenceBeanContribution, "@Bean @DubboReference ReferenceBean must contribute AOT proxy hints");
            Assertions.assertNotNull(
                    genericReferenceBeanContribution, "GenericService ReferenceBean must contribute AOT proxy hints");

            DefaultGenerationContext generationContext = new DefaultGenerationContext(
                    new ClassNameGenerator(ClassName.get("org.apache.dubbo.generated", "ReferenceHints")),
                    new InMemoryGeneratedFiles());
            referenceBeanContribution.applyTo(generationContext, null);
            Assertions.assertTrue(RuntimeHintsPredicates.proxies()
                    .forInterfaces(DemoService.class, EchoService.class, Destroyable.class)
                    .test(generationContext.getRuntimeHints()));
            Assertions.assertTrue(RuntimeHintsPredicates.reflection()
                    .onType(DemoService.class)
                    .withMemberCategory(MemberCategory.INVOKE_PUBLIC_METHODS)
                    .test(generationContext.getRuntimeHints()));
            Assertions.assertTrue(RuntimeHintsPredicates.serialization()
                    .onType(String.class)
                    .test(generationContext.getRuntimeHints()));

            DefaultGenerationContext genericGenerationContext = new DefaultGenerationContext(
                    new ClassNameGenerator(ClassName.get("org.apache.dubbo.generated", "GenericReferenceHints")),
                    new InMemoryGeneratedFiles());
            genericReferenceBeanContribution.applyTo(genericGenerationContext, null);
            Assertions.assertTrue(RuntimeHintsPredicates.proxies()
                    .forInterfaces(GenericService.class, EchoService.class, Destroyable.class, DemoService.class)
                    .test(genericGenerationContext.getRuntimeHints()));
            Assertions.assertTrue(RuntimeHintsPredicates.reflection()
                    .onType(GenericService.class)
                    .withMemberCategory(MemberCategory.INVOKE_PUBLIC_METHODS)
                    .test(genericGenerationContext.getRuntimeHints()));
            Assertions.assertTrue(RuntimeHintsPredicates.reflection()
                    .onType(DemoService.class)
                    .withMemberCategory(MemberCategory.INVOKE_PUBLIC_METHODS)
                    .test(genericGenerationContext.getRuntimeHints()));
            Assertions.assertTrue(RuntimeHintsPredicates.serialization()
                    .onType(String.class)
                    .test(genericGenerationContext.getRuntimeHints()));
        } finally {
            fieldContext.close();
            context.close();
        }
    }

    private AnnotationConfigApplicationContext createContextWithReferencePostProcessor(Class<?>... configurations) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ReferenceAnnotationWithAotBeanPostProcessor postProcessor = new ReferenceAnnotationWithAotBeanPostProcessor();
        postProcessor.setBeanClassLoader(getClass().getClassLoader());
        postProcessor.setEnvironment(context.getEnvironment());
        context.addBeanFactoryPostProcessor(beanFactory -> postProcessor.setBeanFactory(beanFactory));
        context.addBeanFactoryPostProcessor(postProcessor);
        context.register(ReferenceConfiguration.class);
        if (configurations.length > 0) {
            context.register(configurations);
        }
        context.refresh();
        return context;
    }

    @Configuration
    static class ReferenceConfiguration {

        @Bean
        @Lazy
        @DubboReference(group = "demo", init = false)
        ReferenceBean<DemoService> demoService() {
            return new ReferenceBean<>();
        }
    }

    @Configuration
    static class FieldReferenceConfiguration {

        @Bean
        FieldConsumer fieldConsumer() {
            return new FieldConsumer();
        }
    }

    @Configuration
    static class GenericReferenceConfiguration {

        @Bean
        @Lazy
        @DubboReference(interfaceClass = DemoService.class, init = false)
        ReferenceBean<GenericService> genericDemoService() {
            return new ReferenceBean<>();
        }
    }

    static class FieldConsumer {

        @DubboReference(group = "demo", init = false)
        private DemoService demoService;
    }

    interface DemoService {

        String sayHello();
    }
}
