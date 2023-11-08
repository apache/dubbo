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
import org.apache.dubbo.config.spring.context.annotation.DubboConfigConfigurationRegistrar;
import org.apache.dubbo.config.spring.extension.SpringExtensionInjector;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.config.spring.util.EnvironmentUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.SortedMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

public class DubboContextPostProcessor
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, EnvironmentAware {

    /**
     * The bean name of {@link DubboConfigConfigurationRegistrar}
     */
    public static final String BEAN_NAME = "dubboContextPostProcessor";

    private ApplicationContext applicationContext;

    private ConfigurableEnvironment environment;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ApplicationModel applicationModel = DubboBeanUtils.getApplicationModel(beanFactory);
        ModuleModel moduleModel = DubboBeanUtils.getModuleModel(beanFactory);

        // Initialize SpringExtensionInjector
        SpringExtensionInjector.get(applicationModel).init(applicationContext);
        SpringExtensionInjector.get(moduleModel).init(applicationContext);
        DubboBeanUtils.getInitializationContext(beanFactory).setApplicationContext(applicationContext);

        // Initialize dubbo Environment before ConfigManager
        // Extract dubbo props from Spring env and put them to app config
        SortedMap<String, String> dubboProperties = EnvironmentUtils.filterDubboProperties(environment);
        applicationModel.getModelEnvironment().getAppConfigMap().putAll(dubboProperties);

        // register ConfigManager singleton
        beanFactory.registerSingleton(ConfigManager.BEAN_NAME, applicationModel.getApplicationConfigManager());
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        DubboSpringInitializer.initialize(beanDefinitionRegistry);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }
}
