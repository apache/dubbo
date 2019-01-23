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
package org.apache.dubbo.config.spring.context.context.annotation;

import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.context.annotation.consumer.test.TestConsumerConfiguration;
import org.apache.dubbo.config.spring.context.annotation.provider.DemoServiceImpl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * {@link EnableDubbo} Test
 *
 * @since 2.5.8
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EnableDubboTest.class})
@TestPropertySource(locations = "classpath:/META-INF/dubbb-provider.properties",
        properties = "demo.service.version = 2.5.7")
@EnableDubbo(scanBasePackages = "org.apache.dubbo.config.spring.context.annotation.provider")
@ComponentScan(basePackages = "org.apache.dubbo.config.spring.context.annotation.provider")
@EnableTransactionManagement
public class EnableDubboTest {

    @Autowired
    private ApplicationContext providerContext;

    @Test
    public void test() {

        DemoService demoService = providerContext.getBean(DemoService.class);

        String value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        Class<?> beanClass = AopUtils.getTargetClass(demoService);

        // DemoServiceImpl with @Transactional
        Assert.assertEquals(DemoServiceImpl.class, beanClass);

        // Test @Transactional is present or not
        Assert.assertNotNull(findAnnotation(beanClass, Transactional.class));

        AnnotationConfigApplicationContext consumerContext = new AnnotationConfigApplicationContext(TestConsumerConfiguration.class);

        TestConsumerConfiguration consumerConfiguration = consumerContext.getBean(TestConsumerConfiguration.class);

        demoService = consumerConfiguration.getDemoService();

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        TestConsumerConfiguration.Child child = consumerContext.getBean(TestConsumerConfiguration.Child.class);

        // From Child

        demoService = child.getDemoServiceFromChild();

        Assert.assertNotNull(demoService);

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        // From Parent

        demoService = child.getDemoServiceFromParent();

        Assert.assertNotNull(demoService);

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        // From Ancestor

        demoService = child.getDemoServiceFromAncestor();

        Assert.assertNotNull(demoService);

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        // Test my-registry2 bean presentation
        RegistryConfig registryConfig = consumerContext.getBean("my-registry2", RegistryConfig.class);

        // Test multiple binding
        Assert.assertEquals("N/A", registryConfig.getAddress());

    }

    @EnableDubbo(scanBasePackages = "org.apache.dubbo.config.spring.context.annotation.provider")
    @ComponentScan(basePackages = "org.apache.dubbo.config.spring.context.annotation.provider")
    @PropertySource("classpath:/META-INF/dubbb-provider.properties")
    @EnableTransactionManagement
    public static class TestProviderConfiguration {

        @Primary
        @Bean
        public PlatformTransactionManager platformTransactionManager() {
            return new PlatformTransactionManager() {

                @Override
                public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                    return null;
                }

                @Override
                public void commit(TransactionStatus status) throws TransactionException {

                }

                @Override
                public void rollback(TransactionStatus status) throws TransactionException {

                }
            };
        }
    }


}
