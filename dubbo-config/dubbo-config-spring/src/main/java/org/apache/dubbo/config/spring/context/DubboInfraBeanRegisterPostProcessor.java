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
package org.apache.dubbo.config.spring.context;

import org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * Register some infrastructure beans if not exists.
 * This post-processor MUST impl BeanDefinitionRegistryPostProcessor,
 * in order to enable the registered BeanFactoryPostProcessor bean to be loaded and executed.
 *
 * @see org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors(
 *org.springframework.beans.factory.config.ConfigurableListableBeanFactory, java.util.List)
 */
public class DubboInfraBeanRegisterPostProcessor implements BeanDefinitionRegistryPostProcessor {

    /**
     * The bean name of {@link ReferenceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "dubboInfraBeanRegisterPostProcessor";

    private BeanDefinitionRegistry registry;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        // In Spring 3.2.x, registry may be null because do not call postProcessBeanDefinitionRegistry method before postProcessBeanFactory
        if (registry != null) {
            // register ReferenceAnnotationBeanPostProcessor early before PropertySourcesPlaceholderConfigurer/PropertyPlaceholderConfigurer
            // for processing early init ReferenceBean
            ReferenceAnnotationBeanPostProcessor referenceAnnotationBeanPostProcessor = beanFactory.getBean(
                ReferenceAnnotationBeanPostProcessor.BEAN_NAME, ReferenceAnnotationBeanPostProcessor.class);
            beanFactory.addBeanPostProcessor(referenceAnnotationBeanPostProcessor);

            // register PropertySourcesPlaceholderConfigurer bean if not exits
            DubboBeanUtils.registerPlaceholderConfigurerBeanIfNotExists(beanFactory, registry);
        }

        // fix https://github.com/apache/dubbo/issues/10278
        if (registry != null){
            registry.removeBeanDefinition(BEAN_NAME);
        }
    }

}
