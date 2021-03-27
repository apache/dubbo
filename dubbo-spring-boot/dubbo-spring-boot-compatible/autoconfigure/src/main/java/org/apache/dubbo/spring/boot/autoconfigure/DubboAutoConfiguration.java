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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.apache.dubbo.config.spring.beans.factory.annotation.ServiceClassPostProcessor;
import org.apache.dubbo.config.spring.context.DubboBootstrapApplicationListener;
import org.apache.dubbo.config.spring.context.DubboLifecycleComponentApplicationListener;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

import static org.apache.dubbo.spring.boot.util.DubboUtils.BASE_PACKAGES_BEAN_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.BASE_PACKAGES_PROPERTY_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_PREFIX;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SCAN_PREFIX;

/**
 * Dubbo Auto {@link Configuration}
 *
 * @see DubboReference
 * @see DubboService
 * @see ServiceClassPostProcessor
 * @see ReferenceAnnotationBeanPostProcessor
 * @since 2.7.0
 */
@ConditionalOnProperty(prefix = DUBBO_PREFIX, name = "enabled", matchIfMissing = true)
@Configuration
@AutoConfigureAfter(DubboRelaxedBindingAutoConfiguration.class)
@EnableConfigurationProperties(DubboConfigurationProperties.class)
@EnableDubboConfig
public class DubboAutoConfiguration implements ApplicationContextAware, BeanDefinitionRegistryPostProcessor {

    /**
     * Creates {@link ServiceClassPostProcessor} Bean
     *
     * @param packagesToScan the packages to scan
     * @return {@link ServiceClassPostProcessor}
     */
    @ConditionalOnProperty(prefix = DUBBO_SCAN_PREFIX, name = BASE_PACKAGES_PROPERTY_NAME)
    @ConditionalOnBean(name = BASE_PACKAGES_BEAN_NAME)
    @Bean
    public ServiceClassPostProcessor serviceClassPostProcessor(@Qualifier(BASE_PACKAGES_BEAN_NAME)
                                                                       Set<String> packagesToScan) {
        return new ServiceClassPostProcessor(packagesToScan);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
            DubboLifecycleComponentApplicationListener dubboLifecycleComponentApplicationListener
                    = new DubboLifecycleComponentApplicationListener(applicationContext);
            context.addApplicationListener(dubboLifecycleComponentApplicationListener);

            DubboBootstrapApplicationListener dubboBootstrapApplicationListener = new DubboBootstrapApplicationListener(applicationContext);
            context.addApplicationListener(dubboBootstrapApplicationListener);
        }
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // Remove the BeanDefinitions of ApplicationListener from DubboBeanUtils#registerCommonBeans(BeanDefinitionRegistry)
        // TODO Refactoring in Dubbo 2.7.9
        removeBeanDefinition(registry, DubboLifecycleComponentApplicationListener.BEAN_NAME);
        removeBeanDefinition(registry, DubboBootstrapApplicationListener.BEAN_NAME);
    }

    private void removeBeanDefinition(BeanDefinitionRegistry registry, String beanName) {
        if (registry.containsBeanDefinition(beanName)) {
            registry.removeBeanDefinition(beanName);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // DO NOTHING
    }
}
