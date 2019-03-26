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
package org.apache.dubbo.config.spring.beans.factory.annotation;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.spring.context.properties.DefaultDubboConfigBinder;
import org.apache.dubbo.config.spring.context.properties.DubboConfigBinder;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * {@link DubboConfigBindingBeanPostProcessor}
 */
@PropertySource({"classpath:/META-INF/config.properties"})
@Configuration
public class DubboConfigBindingBeanPostProcessorTest {

    @Bean("applicationBean")
    public ApplicationConfig applicationConfig() {
        return new ApplicationConfig();
    }

    @Bean
    public DubboConfigBinder dubboConfigBinder() {
        return new DefaultDubboConfigBinder();
    }

    @Test
    public void test() {

        final AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

        applicationContext.register(getClass());

        Class<?> processorClass = DubboConfigBindingBeanPostProcessor.class;

        applicationContext.registerBeanDefinition("DubboConfigBindingBeanPostProcessor", rootBeanDefinition(processorClass).addConstructorArgValue("dubbo.application").addConstructorArgValue("applicationBean").getBeanDefinition());

        applicationContext.refresh();

        ApplicationConfig applicationConfig = applicationContext.getBean(ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application", applicationConfig.getName());

    }
}
