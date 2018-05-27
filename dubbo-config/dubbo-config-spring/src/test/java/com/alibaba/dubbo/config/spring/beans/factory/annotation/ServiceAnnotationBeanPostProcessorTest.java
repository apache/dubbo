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
package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.config.spring.api.HelloService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * {@link ServiceAnnotationBeanPostProcessor} Test
 *
 * @since 2.5.8
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {ServiceAnnotationBeanPostProcessorTest.TestConfiguration.class})
@TestPropertySource(properties = {
        "package1 = com.alibaba.dubbo.config.spring.context.annotation",
        "packagesToScan = ${package1}",
        "provider.version = 1.2"
})
public class ServiceAnnotationBeanPostProcessorTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Test
    public void test() {

        Map<String, HelloService> helloServicesMap = beanFactory.getBeansOfType(HelloService.class);

        Assert.assertEquals(2, helloServicesMap.size());

        Map<String, ServiceBean> serviceBeansMap = beanFactory.getBeansOfType(ServiceBean.class);

        Assert.assertEquals(3, serviceBeansMap.size());

        Map<String, ServiceAnnotationBeanPostProcessor> beanPostProcessorsMap =
                beanFactory.getBeansOfType(ServiceAnnotationBeanPostProcessor.class);

        Assert.assertEquals(4, beanPostProcessorsMap.size());

        Assert.assertTrue(beanPostProcessorsMap.containsKey("doubleServiceAnnotationBeanPostProcessor"));
        Assert.assertTrue(beanPostProcessorsMap.containsKey("emptyServiceAnnotationBeanPostProcessor"));
        Assert.assertTrue(beanPostProcessorsMap.containsKey("serviceAnnotationBeanPostProcessor"));
        Assert.assertTrue(beanPostProcessorsMap.containsKey("serviceAnnotationBeanPostProcessor2"));

    }

    @ImportResource("META-INF/spring/dubbo-annotation-provider.xml")
    @PropertySource("META-INF/default.properties")
    @ComponentScan("com.alibaba.dubbo.config.spring.context.annotation.provider")
    public static class TestConfiguration {

        @Bean
        public ServiceAnnotationBeanPostProcessor serviceAnnotationBeanPostProcessor
                (@Value("${packagesToScan}") String... packagesToScan) {
            return new ServiceAnnotationBeanPostProcessor(packagesToScan);
        }

        @Bean
        public ServiceAnnotationBeanPostProcessor serviceAnnotationBeanPostProcessor2
                (@Value("${packagesToScan}") String... packagesToScan) {
            return new ServiceAnnotationBeanPostProcessor(packagesToScan);
        }


    }

}
