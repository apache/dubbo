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
import org.apache.dubbo.config.spring.context.DubboSpringInitContext;
import org.apache.dubbo.config.spring.context.DubboSpringInitCustomizer;

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

public class MockSpringInitCustomizer implements DubboSpringInitCustomizer {

    private List<DubboSpringInitContext> contexts = new ArrayList<>();

    @Override
    public void customize(DubboSpringInitContext context) {
        this.contexts.add(context);

        // register post-processor bean, expecting the bean is loaded and invoked by spring container
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(CustomBeanFactoryPostProcessor.class)
            .getBeanDefinition();
        context.getRegistry().registerBeanDefinition(CustomBeanFactoryPostProcessor.class.getName(), beanDefinition);
    }

    public List<DubboSpringInitContext> getContexts() {
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
        Set<DubboSpringInitCustomizer> customizers = ExtensionLoader
            .getExtensionLoader(DubboSpringInitCustomizer.class)
            .getSupportedExtensionInstances();

        MockSpringInitCustomizer mockCustomizer = null;
        for (DubboSpringInitCustomizer customizer : customizers) {
            if (customizer instanceof MockSpringInitCustomizer) {
                mockCustomizer = (MockSpringInitCustomizer) customizer;
                break;
            }
        }
        Assertions.assertNotNull(mockCustomizer);

        // check applicationContext
        boolean foundInitContext = false;
        List<DubboSpringInitContext> contexts = mockCustomizer.getContexts();
        for (DubboSpringInitContext initializationContext : contexts) {
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
