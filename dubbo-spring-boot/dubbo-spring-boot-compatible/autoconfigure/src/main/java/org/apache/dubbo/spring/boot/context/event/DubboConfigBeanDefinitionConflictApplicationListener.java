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
package org.apache.dubbo.spring.boot.context.event;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForTypeIncludingAncestors;
import static org.springframework.context.ConfigurableApplicationContext.ENVIRONMENT_BEAN_NAME;

/**
 * The {@link ApplicationListener} class for Dubbo Config {@link BeanDefinition Bean Definition} to resolve conflict
 * @see BeanDefinition
 * @see ApplicationListener
 * @since 2.7.5
 */
public class DubboConfigBeanDefinitionConflictApplicationListener implements ApplicationListener<ContextRefreshedEvent>,
        Ordered {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        BeanDefinitionRegistry registry = getBeanDefinitionRegistry(applicationContext);
        resolveUniqueApplicationConfigBean(registry, applicationContext);
    }

    private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext applicationContext) {
        AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        if (beanFactory instanceof BeanDefinitionRegistry) {
            return (BeanDefinitionRegistry) beanFactory;
        }
        throw new IllegalStateException("");
    }

    /**
     * Resolve the unique {@link ApplicationConfig} Bean
     * @param registry {@link BeanDefinitionRegistry} instance
     * @param beanFactory {@link ConfigurableListableBeanFactory} instance
     * @see EnableDubboConfig
     */
    private void resolveUniqueApplicationConfigBean(BeanDefinitionRegistry registry, ListableBeanFactory beanFactory) {

        String[] beansNames = beanNamesForTypeIncludingAncestors(beanFactory, ApplicationConfig.class);

        if (beansNames.length < 2) { // If the number of ApplicationConfig beans is less than two, return immediately.
            return;
        }

        Environment environment = beanFactory.getBean(ENVIRONMENT_BEAN_NAME, Environment.class);

        // Remove ApplicationConfig Beans that are configured by "dubbo.application.*"
        Stream.of(beansNames)
                .filter(beansName -> isConfiguredApplicationConfigBeanName(environment, beansName))
                .forEach(registry::removeBeanDefinition);

        beansNames = beanNamesForTypeIncludingAncestors(beanFactory, ApplicationConfig.class);

        if (beansNames.length > 1) {
            throw new IllegalStateException(String.format("There are more than one instances of %s, whose bean definitions : %s",
                    ApplicationConfig.class.getSimpleName(),
                    Stream.of(beansNames)
                            .map(registry::getBeanDefinition)
                            .collect(Collectors.toList()))
            );
        }
    }

    private boolean isConfiguredApplicationConfigBeanName(Environment environment, String beanName) {
        boolean removed = BeanFactoryUtils.isGeneratedBeanName(beanName)
                // Dubbo ApplicationConfig id as bean name
                || Objects.equals(beanName, environment.getProperty("dubbo.application.id"));

        if (removed) {
            if (logger.isDebugEnabled()) {
                logger.debug("The {} bean [ name : {} ] has been removed!", ApplicationConfig.class.getSimpleName(), beanName);
            }
        }

        return removed;
    }


    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
