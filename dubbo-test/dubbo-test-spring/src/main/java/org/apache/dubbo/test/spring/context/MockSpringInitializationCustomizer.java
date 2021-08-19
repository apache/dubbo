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
package org.apache.dubbo.test.spring.context;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.spring.context.DubboSpringInitializationContext;
import org.apache.dubbo.config.spring.context.DubboSpringInitializationCustomizer;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MockSpringInitializationCustomizer implements DubboSpringInitializationCustomizer {

    private List<DubboSpringInitializationContext> contexts = new ArrayList<>();

    @Override
    public void customize(DubboSpringInitializationContext context) {
        this.contexts.add(context);

        // register post-processor bean, expecting the bean is loaded and invoked by spring container
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(CustomBeanFactoryPostProcessor.class)
            .getBeanDefinition();
        context.getRegistry().registerBeanDefinition(CustomBeanFactoryPostProcessor.class.getName(), beanDefinition);
    }

    public List<DubboSpringInitializationContext> getContexts() {
        return contexts;
    }

    private static class CustomBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
        private ConfigurableListableBeanFactory beanFactory;

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }
    }

    public static void checkCustomizer(ConfigurableApplicationContext applicationContext) {
        Set<DubboSpringInitializationCustomizer> customizers = ExtensionLoader
            .getExtensionLoader(DubboSpringInitializationCustomizer.class)
            .getSupportedExtensionInstances();

        MockSpringInitializationCustomizer mockCustomizer = null;
        for (DubboSpringInitializationCustomizer customizer : customizers) {
            if (customizer instanceof MockSpringInitializationCustomizer) {
                mockCustomizer = (MockSpringInitializationCustomizer) customizer;
                break;
            }
        }
        Assertions.assertNotNull(mockCustomizer);

        // check applicationContext
        boolean foundInitContext = false;
        List<DubboSpringInitializationContext> contexts = mockCustomizer.getContexts();
        for (DubboSpringInitializationContext initializationContext : contexts) {
            if (initializationContext.getRegistry() == applicationContext.getBeanFactory()) {
                foundInitContext = true;
                break;
            }
        }
        Assertions.assertEquals(true, foundInitContext);

        // expect CustomBeanFactoryPostProcessor is loaded and invoked
        CustomBeanFactoryPostProcessor customBeanFactoryPostProcessor = applicationContext.getBean(CustomBeanFactoryPostProcessor.class);
        Assertions.assertEquals(applicationContext.getBeanFactory(), customBeanFactoryPostProcessor.beanFactory);
    }
}
