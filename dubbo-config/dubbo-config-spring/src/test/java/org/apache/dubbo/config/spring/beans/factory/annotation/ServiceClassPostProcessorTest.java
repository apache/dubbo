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

import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * {@link ServiceClassPostProcessor} Test
 *
 * @since 2.7.7
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {
                ServiceAnnotationTestConfiguration2.class,
                ServiceClassPostProcessorTest.class
        })
@TestPropertySource(properties = {
        "provider.package = org.apache.dubbo.config.spring.context.annotation.provider",
        "packagesToScan = ${provider.package}",
})
public class ServiceClassPostProcessorTest {

    @Before
    public void setUp() {
        ApplicationModel.reset();
    }

    @After
    public void tearDown() {
        ApplicationModel.reset();
    }

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Bean
    public ServiceClassPostProcessor serviceClassPostProcessor2
            (@Value("${packagesToScan}") String... packagesToScan) {
        return new ServiceClassPostProcessor(packagesToScan);
    }

    @Test
    public void test() {

        Map<String, HelloService> helloServicesMap = beanFactory.getBeansOfType(HelloService.class);

        Assert.assertEquals(2, helloServicesMap.size());

        Map<String, ServiceBean> serviceBeansMap = beanFactory.getBeansOfType(ServiceBean.class);

        Assert.assertEquals(2, serviceBeansMap.size());

        Map<String, ServiceClassPostProcessor> beanPostProcessorsMap =
                beanFactory.getBeansOfType(ServiceClassPostProcessor.class);

        Assert.assertEquals(2, beanPostProcessorsMap.size());

        Assert.assertTrue(beanPostProcessorsMap.containsKey("serviceClassPostProcessor"));
        Assert.assertTrue(beanPostProcessorsMap.containsKey("serviceClassPostProcessor2"));

    }

    @Test
    public void testMethodAnnotation() {

        Map<String, ServiceBean> serviceBeansMap = beanFactory.getBeansOfType(ServiceBean.class);

        Assert.assertEquals(2, serviceBeansMap.size());

        ServiceBean demoServiceBean = serviceBeansMap.get("ServiceBean:org.apache.dubbo.config.spring.api.DemoService:2.5.7");

        Assert.assertNotNull(demoServiceBean.getMethods());

    }

}
