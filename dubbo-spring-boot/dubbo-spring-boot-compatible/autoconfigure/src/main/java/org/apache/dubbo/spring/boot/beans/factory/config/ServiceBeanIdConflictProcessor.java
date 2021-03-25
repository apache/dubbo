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

import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.spring.ServiceBean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.springframework.util.ClassUtils.getUserClass;
import static org.springframework.util.ClassUtils.isAssignable;

/**
 * The post-processor for resolving the id conflict of {@link ServiceBean} when an interface is
 * implemented by multiple services with different groups or versions that are exported on one provider
 * <p>
 * Current implementation is a temporary resolution, and will be removed in the future.
 *
 * @see CommonAnnotationBeanPostProcessor
 * @since 2.7.7
 * @deprecated
 */
public class ServiceBeanIdConflictProcessor implements MergedBeanDefinitionPostProcessor, DisposableBean, PriorityOrdered {

    /**
     * The key is the class names of interfaces that were exported by {@link ServiceBean}
     * The value is bean names of {@link ServiceBean} or {@link ServiceConfig}.
     */
    private Map<String, String> interfaceNamesToBeanNames = new HashMap<>();

    /**
     * Holds the bean names of {@link ServiceBean} or {@link ServiceConfig}.
     */
    private Set<String> conflictedBeanNames = new LinkedHashSet<>();

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        // Get raw bean type
        Class<?> rawBeanType = getUserClass(beanType);
        if (isAssignable(ServiceConfig.class, rawBeanType)) { // ServiceConfig type or sub-type
            String interfaceName = (String) beanDefinition.getPropertyValues().get("interface");
            String mappedBeanName = interfaceNamesToBeanNames.putIfAbsent(interfaceName, beanName);
            // If mapped bean name exists and does not equal current bean name
            if (mappedBeanName != null && !mappedBeanName.equals(beanName)) {
                // conflictedBeanNames will record current bean name.
                conflictedBeanNames.add(beanName);
            }
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (conflictedBeanNames.contains(beanName) && bean instanceof ServiceConfig) {
            ServiceConfig serviceConfig = (ServiceConfig) bean;
            if (isConflictedServiceConfig(serviceConfig)) {
                // Set id as the bean name
                serviceConfig.setId(beanName);
            }

        }
        return bean;
    }

    private boolean isConflictedServiceConfig(ServiceConfig serviceConfig) {
        return Objects.equals(serviceConfig.getId(), serviceConfig.getInterface());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * Keep the order being higher than {@link CommonAnnotationBeanPostProcessor#getOrder()} that is
     * {@link Ordered#LOWEST_PRECEDENCE}
     *
     * @return {@link Ordered#LOWEST_PRECEDENCE} +1
     */
    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE + 1;
    }

    @Override
    public void destroy() throws Exception {
        interfaceNamesToBeanNames.clear();
        conflictedBeanNames.clear();
    }
}
