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
package org.apache.dubbo.spring.boot.beans.factory.config;

import org.apache.dubbo.config.spring.ConfigCenterBean;
import org.apache.dubbo.config.spring.context.DubboConfigBeanInitializer;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.spring.boot.autoconfigure.DubboConfigurationProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

import java.util.HashMap;
import java.util.Map;


/**
 * Used to set some property values in DubboConfigurationProperties to BeanDefinition in advance
 * It can ensure that the correct configuration can be accessed normally
 * when the prepareDubboConfigBeans method of {@link DubboConfigBeanInitializer} is executed
 */
public class DubboConfigurationPropertiesToBeanDefinitionPostProcessor
    implements BeanDefinitionRegistryPostProcessor, BeanPostProcessor {

    private BeanDefinitionRegistry registry;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DubboConfigurationProperties) {
            DubboConfigurationProperties dubboConfigurationProperties = (DubboConfigurationProperties) bean;

            DubboBeanUtils.registerBeanDefinition(registry, ConfigCenterBean.class.getName(),
                ConfigCenterBean.class, extraPropertyValuesForConfigCenter(dubboConfigurationProperties));
        }
        return bean;
    }

    private Map<String, Object> extraPropertyValuesForConfigCenter(DubboConfigurationProperties dubboConfigurationProperties) {
        Map<String, Object> extraPropertyValues = new HashMap<>(8);
        extraPropertyValues.put("includeSpringEnv", dubboConfigurationProperties.getConfigCenter().getIncludeSpringEnv());
        extraPropertyValues.put("configFile", dubboConfigurationProperties.getConfigCenter().getConfigFile());
        extraPropertyValues.put("appConfigFile", dubboConfigurationProperties.getConfigCenter().getAppConfigFile());
        return extraPropertyValues;
    }

}
