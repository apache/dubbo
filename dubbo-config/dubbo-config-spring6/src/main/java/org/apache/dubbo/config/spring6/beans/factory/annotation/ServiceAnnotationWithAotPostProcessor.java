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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.beans.factory.annotation.ServiceAnnotationPostProcessor;
import org.apache.dubbo.config.spring.schema.AnnotationBeanDefinitionParser;
import org.apache.dubbo.config.spring6.utils.AotUtils;

import java.util.Collection;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * The purpose of implementing {@link BeanRegistrationAotProcessor} is to
 * supplement for {@link ServiceAnnotationPostProcessor} ability of AOT.
 *
 * @see AnnotationBeanDefinitionParser
 * @see BeanDefinitionRegistryPostProcessor
 * @since 3.3
 */
public class ServiceAnnotationWithAotPostProcessor extends ServiceAnnotationPostProcessor
        implements BeanRegistrationAotProcessor {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    public ServiceAnnotationWithAotPostProcessor(String... packagesToScan) {
        super(packagesToScan);
    }

    public ServiceAnnotationWithAotPostProcessor(Collection<?> packagesToScan) {
        super(packagesToScan);
    }

    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
        Class<?> beanClass = registeredBean.getBeanClass();
        if (beanClass.equals(ServiceBean.class)) {
            RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
            String interfaceName = (String) beanDefinition.getPropertyValues().get("interface");
            try {
                Class<?> c = Class.forName(interfaceName);
                return new DubboServiceBeanRegistrationAotContribution(c);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if (servicePackagesHolder.isClassScanned(beanClass.getName())) {
            return new DubboServiceBeanRegistrationAotContribution(beanClass);
        }

        return null;
    }

    private static class DubboServiceBeanRegistrationAotContribution implements BeanRegistrationAotContribution {

        private final Class<?> cl;

        public DubboServiceBeanRegistrationAotContribution(Class<?> cl) {
            this.cl = cl;
        }

        @Override
        public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
            generationContext
                    .getRuntimeHints()
                    .reflection()
                    .registerType(TypeReference.of(cl), MemberCategory.INVOKE_PUBLIC_METHODS);
            AotUtils.registerSerializationForService(cl, generationContext.getRuntimeHints());
        }
    }
}
