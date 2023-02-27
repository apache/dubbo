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

import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.apache.dubbo.config.spring.extension.SpringExtensionInjector;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.config.spring.util.EnvironmentUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.SortedMap;


/**
 * Register some infrastructure beans if not exists.
 * This post-processor MUST impl BeanDefinitionRegistryPostProcessor,
 * in order to enable the registered BeanFactoryPostProcessor bean to be loaded and executed.
 *
 * @see org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors(
 *org.springframework.beans.factory.config.ConfigurableListableBeanFactory, java.util.List)
 */
public class DubboInfraBeanRegisterPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    /**
     * The bean name of {@link ReferenceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "dubboInfraBeanRegisterPostProcessor";

    private BeanDefinitionRegistry registry;
    private ApplicationContext applicationContext;

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

        ApplicationModel applicationModel = DubboBeanUtils.getApplicationModel(beanFactory);
        ModuleModel moduleModel = DubboBeanUtils.getModuleModel(beanFactory);

        // Initialize SpringExtensionInjector
        SpringExtensionInjector.get(applicationModel).init(applicationContext);
        SpringExtensionInjector.get(moduleModel).init(applicationContext);
        DubboBeanUtils.getInitializationContext(beanFactory).setApplicationContext(applicationContext);

        // Initialize dubbo Environment before ConfigManager
        // Extract dubbo props from Spring env and put them to app config
        ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
        SortedMap<String, String> dubboProperties = EnvironmentUtils.filterDubboProperties(environment);
        applicationModel.getModelEnvironment().setAppConfigMap(dubboProperties);

        // register ConfigManager singleton
        beanFactory.registerSingleton(ConfigManager.BEAN_NAME, applicationModel.getApplicationConfigManager());

        // fix https://github.com/apache/dubbo/issues/10278
        if (registry != null){
            registry.removeBeanDefinition(BEAN_NAME);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
